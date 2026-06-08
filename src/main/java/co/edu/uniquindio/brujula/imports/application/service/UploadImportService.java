package co.edu.uniquindio.brujula.imports.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;
import co.edu.uniquindio.brujula.imports.application.dto.UploadImportCommand;
import co.edu.uniquindio.brujula.imports.application.port.in.UploadImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.out.FileParserPort;
import co.edu.uniquindio.brujula.imports.application.port.out.SaveImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.StaffAssignmentPort;
import co.edu.uniquindio.brujula.imports.domain.exception.FileSizeExceededException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException;
import co.edu.uniquindio.brujula.imports.domain.exception.MissingImportParameterException;
import co.edu.uniquindio.brujula.imports.domain.exception.UnsupportedFileTypeException;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import co.edu.uniquindio.brujula.imports.domain.model.Severity;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case implementation: upload and validate an import file (IMP-001, IMP-002, IMP-011).
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Idempotency check (same key + same hash → replay; same key + different hash → 409)
 *   <li>File type guard (415 for unsupported)
 *   <li>File size guard (413 for too large)
 *   <li>Required-parameter guard (400 for missing planId/periodId)
 *   <li>Assignment guard — STAFF only (403 for unassigned or INACTIVE account) [WARNING-1 closure]
 *   <li>Parse (select FileParserPort by content-type/filename)
 *   <li>Validate (per-kind validator)
 *   <li>Persist ImportBatch (SaveImportBatchPort)
 *   <li>Audit record (IMPORT_UPLOADED)
 *   <li>Idempotency save
 *   <li>Return ImportSummary
 * </ol>
 *
 * <p>Framework-free — wired by {@link co.edu.uniquindio.brujula.imports.infrastructure.configuration.ImportsConfiguration}.
 * Mirrors {@link co.edu.uniquindio.brujula.offering.application.service.CreateOfferingService} for
 * idempotency + audit patterns.
 */
public class UploadImportService implements UploadImportUseCase {

  static final String OPERATION = "UPLOAD_IMPORT";

  private final List<FileParserPort> parsers;
  private final ImportValidator importValidator;
  private final SaveImportBatchPort saveImportBatchPort;
  private final StaffAssignmentPort staffAssignmentPort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;
  private final AuditRecorder auditRecorder;
  private final long maxFileSizeBytes;

  public UploadImportService(
      List<FileParserPort> parsers,
      ImportValidator importValidator,
      SaveImportBatchPort saveImportBatchPort,
      StaffAssignmentPort staffAssignmentPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder,
      long maxFileSizeBytes) {
    this.parsers = parsers;
    this.importValidator = importValidator;
    this.saveImportBatchPort = saveImportBatchPort;
    this.staffAssignmentPort = staffAssignmentPort;
    this.idempotencyStorePort = idempotencyStorePort;
    this.jsonCodecPort = jsonCodecPort;
    this.auditRecorder = auditRecorder;
    this.maxFileSizeBytes = maxFileSizeBytes;
  }

  @Override
  public ImportSummary upload(UploadImportCommand command) {
    // 1. Idempotency check (mirrors CreateOfferingService / CreateStaffService pattern)
    Optional<IdempotencyStorePort.StoredRecord> existing =
        idempotencyStorePort.find(command.idempotencyKey(), OPERATION);
    if (existing.isPresent()) {
      IdempotencyStorePort.StoredRecord stored = existing.get();
      if (!stored.requestHash().equals(command.requestHash())) {
        throw new IdempotencyKeyConflictException(command.idempotencyKey(), OPERATION);
      }
      return jsonCodecPort.fromJson(stored.responseBodyJson(), ImportSummary.class);
    }

    // 2. File type guard — find a supporting parser or throw 415
    FileParserPort parser = findParser(command.contentType(), command.filename());

    // 3. File size guard
    if (command.fileBytes().length > maxFileSizeBytes) {
      throw new FileSizeExceededException(command.fileBytes().length, maxFileSizeBytes);
    }

    // 4. Required-parameter guard
    validateRequiredParameters(command);

    // 5. Assignment guard (STAFF only, WARNING-1 closure)
    enforceAssignmentGuard(command);

    // 6. Parse
    FileParserPort.ParseResult parseResult = parser.parse(command.fileBytes(), command.kind());

    // 7. Validate — build ImportBatch domain object
    ImportBatchId batchId = ImportBatchId.generate();
    Instant now = Instant.now();
    ImportBatch batch;
    if (parseResult.hasStructuralIssues()) {
      batch =
          importValidator.validate(
              batchId,
              command.kind(),
              command.programId(),
              command.planId(),
              command.periodId(),
              List.of(), // no rows
              parseResult.structuralIssues(),
              now);
    } else {
      batch =
          importValidator.validate(
              batchId,
              command.kind(),
              command.programId(),
              command.planId(),
              command.periodId(),
              parseResult.rows(),
              List.of(),
              now);
    }

    // 8. Persist
    saveImportBatchPort.save(batch);

    // 9. Build summary view
    ImportSummary summary = toSummary(batch);
    String responseBodyJson = jsonCodecPort.toJson(summary);

    // 10. Idempotency save
    idempotencyStorePort.save(
        command.idempotencyKey(), OPERATION, command.requestHash(), 201, responseBodyJson);

    // 11. Audit (only on successful upload, not on 403/415/413 — SC-011-A, SC-011-C)
    auditRecorder.record(
        new RecordAuditEventCommand(
            command.actorId(),
            command.actorRole(),
            "IMPORT_UPLOADED",
            "import_batch",
            batch.getId().value(),
            command.programId(),
            null,
            null));

    return summary;
  }

  /**
   * Find a parser that supports the given content-type and filename.
   *
   * @throws UnsupportedFileTypeException if no parser supports the format (→ 415)
   */
  private FileParserPort findParser(String contentType, String filename) {
    return parsers.stream()
        .filter(p -> p.supports(contentType, filename))
        .findFirst()
        .orElseThrow(() -> new UnsupportedFileTypeException(contentType, filename));
  }

  /**
   * Validates that required parameters are present for the given kind.
   *
   * @throws MissingImportParameterException if a required param is absent (→ 400)
   */
  private void validateRequiredParameters(UploadImportCommand command) {
    switch (command.kind()) {
      case COURSES, REQUIREMENTS -> {
        if (command.planId() == null) {
          throw new MissingImportParameterException(
              "planId",
              "planId is required when kind is %s".formatted(command.kind()));
        }
      }
      case OFFERINGS -> {
        if (command.periodId() == null) {
          throw new MissingImportParameterException(
              "periodId",
              "periodId is required when kind is OFFERINGS");
        }
      }
    }
  }

  /**
   * Enforces the assignment guard for STAFF actors (IMP-010, INV-06, WARNING-1 closure).
   *
   * <p>ADMIN actors bypass this check entirely.
   * STAFF actors must: (1) have an ACTIVE StaffAccount, (2) be assigned to the target program.
   *
   * <p>Uses {@link StaffAssignmentPort#resolveActiveStaffId(String)} — INACTIVE accounts
   * return empty → 403, closing WARNING-1.
   *
   * @throws ImportAccessDeniedException if access is denied (→ 403 in WU4)
   */
  private void enforceAssignmentGuard(UploadImportCommand command) {
    if ("ADMIN".equalsIgnoreCase(command.actorRole())) {
      return; // ADMIN bypasses assignment check (SC-001-J)
    }

    // STAFF: resolve ACTIVE staff account only (WARNING-1 closure)
    Optional<UUID> staffId = staffAssignmentPort.resolveActiveStaffId(command.actorEmail());
    if (staffId.isEmpty()) {
      throw ImportAccessDeniedException.inactiveOrUnknownActor(command.actorEmail());
    }

    // Check program assignment
    if (!staffAssignmentPort.isAssigned(staffId.get(), command.programId())) {
      throw ImportAccessDeniedException.notAssignedToProgram(command.programId());
    }
  }

  /** Builds an ImportSummary application DTO from an ImportBatch. */
  private ImportSummary toSummary(ImportBatch batch) {
    List<ImportSummary.ImportIssueDto> errors = new ArrayList<>();
    List<ImportSummary.ImportIssueDto> warnings = new ArrayList<>();

    for (ImportIssue issue : batch.getIssues()) {
      ImportSummary.ImportIssueDto dto =
          new ImportSummary.ImportIssueDto(
              issue.row(),
              issue.column(),
              issue.code(),
              issue.message(),
              issue.severity().name());
      if (issue.severity() == Severity.ERROR) {
        errors.add(dto);
      } else {
        warnings.add(dto);
      }
    }

    return new ImportSummary(
        batch.getId().value(),
        batch.getKind().name(),
        batch.getProgramId(),
        batch.getPlanId(),
        batch.getPeriodId(),
        batch.getStatus().name(),
        batch.getTotalRows(),
        batch.getValidRows(),
        errors,
        warnings,
        batch.getCreatedAt(),
        batch.getVersion());
  }
}
