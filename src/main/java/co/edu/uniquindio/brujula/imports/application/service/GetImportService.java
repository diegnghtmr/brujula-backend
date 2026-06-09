package co.edu.uniquindio.brujula.imports.application.service;

import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;
import co.edu.uniquindio.brujula.imports.application.port.in.GetImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.StaffAssignmentPort;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotFoundException;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service: retrieve a single ImportBatch by id with access control (IMP-004).
 *
 * <p>STAFF actors are restricted to batches belonging to programs they are actively assigned to.
 * ADMIN actors bypass the assignment check.
 *
 * <p>Framework-free — wired by {@link co.edu.uniquindio.brujula.imports.infrastructure.configuration.ImportsConfiguration}.
 */
public class GetImportService implements GetImportUseCase {

  private final LoadImportBatchPort loadImportBatchPort;
  private final StaffAssignmentPort staffAssignmentPort;

  public GetImportService(
      LoadImportBatchPort loadImportBatchPort,
      StaffAssignmentPort staffAssignmentPort) {
    this.loadImportBatchPort = loadImportBatchPort;
    this.staffAssignmentPort = staffAssignmentPort;
  }

  @Override
  public ImportSummary get(UUID importId, String actorEmail, String actorRole) {
    ImportBatch batch =
        loadImportBatchPort
            .findById(new ImportBatchId(importId))
            .orElseThrow(() -> new ImportBatchNotFoundException(importId));

    if (!"ADMIN".equalsIgnoreCase(actorRole)) {
      enforceStaffAccess(actorEmail, batch.getProgramId());
    }

    return toSummary(batch);
  }

  /**
   * Enforce STAFF access: actor must have an ACTIVE account AND be assigned to the batch's program.
   */
  private void enforceStaffAccess(String actorEmail, UUID programId) {
    Optional<UUID> staffId = staffAssignmentPort.resolveActiveStaffId(actorEmail);
    if (staffId.isEmpty()) {
      throw ImportAccessDeniedException.inactiveOrUnknownActor(actorEmail);
    }
    if (!staffAssignmentPort.isAssigned(staffId.get(), programId)) {
      throw ImportAccessDeniedException.notAssignedToProgram(programId);
    }
  }

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
