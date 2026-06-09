package co.edu.uniquindio.brujula.imports.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseRequirementCommand;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseRequirementUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseUseCase;
import co.edu.uniquindio.brujula.imports.application.dto.ConfirmImportCommand;
import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;
import co.edu.uniquindio.brujula.imports.application.port.in.ConfirmImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.SaveImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.StaffAssignmentPort;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotFoundException;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;
import co.edu.uniquindio.brujula.imports.domain.model.Severity;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case: confirm a VALIDATED ImportBatch (IMP-005, ADR-F6-001).
 *
 * <p>Single {@link Transactional} boundary on {@link #confirm}. All calls to
 * {@link CreateCourseUseCase} and {@link CreateCourseRequirementUseCase} run inside this
 * transaction because those adapters use default REQUIRED propagation. Any exception from any
 * row propagates out of this method, rolls back the entire transaction, and leaves the batch
 * in VALIDATED status.
 *
 * <p>Idempotency record is saved INSIDE the transaction so a rolled-back attempt leaves no
 * orphan record (ADR-F6-001 pitfall (b)).
 *
 * <p>Framework-free except for {@code @Transactional} — wired by {@code ImportsConfiguration}.
 */
public class ConfirmImportService implements ConfirmImportUseCase {

    /**
     * Operation identifier used in the idempotency store.
     * Public so tests can reference it without string duplication.
     */
    public static final String OPERATION = "CONFIRM_IMPORT";

    private final LoadImportBatchPort loadImportBatchPort;
    private final SaveImportBatchPort saveImportBatchPort;
    private final StaffAssignmentPort staffAssignmentPort;
    private final IdempotencyStorePort idempotencyStorePort;
    private final JsonCodecPort jsonCodecPort;
    private final AuditRecorder auditRecorder;
    private final CreateCourseUseCase createCourseUseCase;
    private final CreateCourseRequirementUseCase createCourseRequirementUseCase;

    public ConfirmImportService(
            LoadImportBatchPort loadImportBatchPort,
            SaveImportBatchPort saveImportBatchPort,
            StaffAssignmentPort staffAssignmentPort,
            IdempotencyStorePort idempotencyStorePort,
            JsonCodecPort jsonCodecPort,
            AuditRecorder auditRecorder,
            CreateCourseUseCase createCourseUseCase,
            CreateCourseRequirementUseCase createCourseRequirementUseCase) {
        this.loadImportBatchPort = loadImportBatchPort;
        this.saveImportBatchPort = saveImportBatchPort;
        this.staffAssignmentPort = staffAssignmentPort;
        this.idempotencyStorePort = idempotencyStorePort;
        this.jsonCodecPort = jsonCodecPort;
        this.auditRecorder = auditRecorder;
        this.createCourseUseCase = createCourseUseCase;
        this.createCourseRequirementUseCase = createCourseRequirementUseCase;
    }

    /**
     * Confirm a VALIDATED ImportBatch.
     *
     * <p>This method is annotated {@code @Transactional} (default REQUIRED). All curriculum
     * write use-cases that are called inside this method JOIN this transaction because their
     * persistence adapters use REQUIRED propagation. Any exception propagates and rolls back
     * the entire transaction — zero partial commits (Decision #2 / INV-03).
     */
    @Override
    @Transactional
    public ImportSummary confirm(ConfirmImportCommand command) {

        // ----------------------------------------------------------------
        // 1. Idempotency check (mirrors UploadImportService / CreateOfferingService)
        // ----------------------------------------------------------------
        Optional<IdempotencyStorePort.StoredRecord> existing =
                idempotencyStorePort.find(command.idempotencyKey(), OPERATION);
        if (existing.isPresent()) {
            IdempotencyStorePort.StoredRecord stored = existing.get();
            if (!stored.requestHash().equals(command.requestHash())) {
                throw new IdempotencyKeyConflictException(command.idempotencyKey(), OPERATION);
            }
            return jsonCodecPort.fromJson(stored.responseBodyJson(), ImportSummary.class);
        }

        // ----------------------------------------------------------------
        // 2. Load ImportBatch — not found → 404
        // ----------------------------------------------------------------
        ImportBatch batch = loadImportBatchPort
                .findById(ImportBatchId.of(command.importId()))
                .orElseThrow(() -> new ImportBatchNotFoundException(command.importId()));

        // ----------------------------------------------------------------
        // 3. Assignment guard — STAFF only; ADMIN bypasses (INV-06)
        // ----------------------------------------------------------------
        enforceAssignmentGuard(command, batch);

        // ----------------------------------------------------------------
        // 4. Optimistic-lock check (ADR-010): expectedVersion must match stored version
        // ----------------------------------------------------------------
        if (batch.getVersion() != command.expectedVersion()) {
            throw new ObjectOptimisticLockingFailureException(
                    ImportBatch.class,
                    command.importId(),
                    "Version conflict: expected %d but stored batch is at version %d"
                            .formatted(command.expectedVersion(), batch.getVersion()),
                    null);
        }

        // ----------------------------------------------------------------
        // 5. Domain guard: only VALIDATED is confirmable (INV-01, INV-02)
        //    ImportBatch.markConfirmed() will throw ImportBatchNotConfirmableException
        //    if status is INVALID or CONFIRMED.
        // ----------------------------------------------------------------
        // (Checked inside markConfirmed below; calling it at the right moment)

        // ----------------------------------------------------------------
        // 6. ATOMIC ROW REPLAY (inside this @Transactional boundary)
        //    All writes join the current TX via Propagation.REQUIRED.
        //    Any exception propagates → full rollback.
        // ----------------------------------------------------------------
        replayRows(batch, command);

        // ----------------------------------------------------------------
        // 7. Mark batch CONFIRMED and persist (inside the TX)
        // ----------------------------------------------------------------
        batch.markConfirmed(); // throws ImportBatchNotConfirmableException if not VALIDATED
        ImportBatch saved = saveImportBatchPort.save(batch);

        // ----------------------------------------------------------------
        // 8. Build summary view
        // ----------------------------------------------------------------
        ImportSummary summary = toSummary(saved);
        String responseBodyJson = jsonCodecPort.toJson(summary);

        // ----------------------------------------------------------------
        // 9. Idempotency save (INSIDE TX — rolled-back attempt leaves no record)
        // ----------------------------------------------------------------
        idempotencyStorePort.save(
                command.idempotencyKey(), OPERATION, command.requestHash(), 200, responseBodyJson);

        // ----------------------------------------------------------------
        // 10. Audit entry (IMPORT_CONFIRMED, IMP-011, SC-011-B)
        // ----------------------------------------------------------------
        auditRecorder.record(new RecordAuditEventCommand(
                command.actorId(),
                command.actorRole(),
                "IMPORT_CONFIRMED",
                "import_batch",
                saved.getId().value(),
                saved.getProgramId(),
                null,
                null));

        return summary;
    }

    // =========================================================================
    // Private — row replay
    // =========================================================================

    /**
     * Replay all validated rows for the given kind through the appropriate write use-case.
     *
     * <p>COURSES → {@link CreateCourseUseCase} with initialStatus=INACTIVE (NFR-003).
     * REQUIREMENTS → {@link CreateCourseRequirementUseCase} (cross-batch cycle re-check runs
     * inside the real service against the live graph — carry-forward from WU3 verify).
     * OFFERINGS → deferred to WU7 (throws {@link UnsupportedOperationException}).
     *
     * <p>Any exception from a use-case propagates directly — no catch-and-continue (all-or-nothing
     * per frozen Decision #2). The caller's {@code @Transactional} boundary catches the exception
     * and rolls back.
     *
     * @param batch the VALIDATED ImportBatch whose rows to replay
     * @param command the confirm command (carries actorId and planId via batch)
     */
    private void replayRows(ImportBatch batch, ConfirmImportCommand command) {
        if (batch.getKind() == ImportKind.OFFERINGS) {
            throw new UnsupportedOperationException(
                    "OFFERINGS confirmation is deferred to WU7. Batch kind OFFERINGS cannot be confirmed yet.");
        }

        for (ImportRow row : batch.getValidatedRows()) {
            switch (batch.getKind()) {
                case COURSES -> replayCourseRow(row, batch, command);
                case REQUIREMENTS -> replayRequirementRow(row, batch, command);
                default -> throw new UnsupportedOperationException(
                        "Unsupported import kind for confirm: " + batch.getKind() + " — deferred to WU7.");
            }
        }
    }

    /**
     * Map a COURSES row to a {@link CreateCourseCommand} and call {@link CreateCourseUseCase}.
     *
     * <p>NFR-003: initialStatus is set to "INACTIVE" so imported courses land INACTIVE/non-published.
     * Auto-publish is FORBIDDEN (RN-122).
     *
     * <p>Per-row idempotency key: derived deterministically from the batch id + row index. This
     * ensures a retried confirm (same importId, same rows) does not double-create courses, and a
     * rolled-back attempt (whose idempotency record was also rolled back inside the TX) can safely
     * retry.
     */
    private void replayCourseRow(ImportRow row, ImportBatch batch, ConfirmImportCommand command) {
        // Generate deterministic per-row idempotency key: hash of (batchId + "COURSE:" + rowIndex)
        UUID rowIdempotencyKey = deterministicRowKey(batch.getId().value(), "COURSE", row.index());
        String rowHash = deterministicRowHash(batch.getId().value(), "COURSE", row.index());

        String name = row.cells().getOrDefault("course_name", "");
        String type = row.cells().getOrDefault("type", "REQUIRED");
        int credits = parseIntOrDefault(row.cells().getOrDefault("credits", "0"), 0);
        int weeklyHours = parseIntOrDefault(row.cells().getOrDefault("weekly_hours", "0"), 0);
        int recommendedSemester = parseIntOrDefault(
                row.cells().getOrDefault("recommended_semester", "1"), 1);
        String officialCode = row.cells().get("official_code");

        // Map recommended_semester to a single RecommendedSemesterView (journey from batch context)
        // For import, we create a single recommendation without a journey code if not specified.
        String journeyCode = row.cells().get("journey");
        List<co.edu.uniquindio.brujula.curriculum.application.dto.CourseView.RecommendedSemesterView> semesters;
        if (journeyCode != null && !journeyCode.isBlank()) {
            semesters = List.of(new co.edu.uniquindio.brujula.curriculum.application.dto.CourseView
                    .RecommendedSemesterView(journeyCode, recommendedSemester));
        } else {
            semesters = List.of();
        }

        CreateCourseCommand courseCommand = new CreateCourseCommand(
                rowIdempotencyKey,
                rowHash,
                command.actorId(),
                batch.getPlanId(),
                batch.getProgramId(),
                name,
                officialCode,
                type,
                credits,
                weeklyHours,
                semesters,
                "INACTIVE"); // NFR-003: imported courses land INACTIVE (not ACTIVE/published)

        createCourseUseCase.createCourse(courseCommand);
    }

    /**
     * Map a REQUIREMENTS row to a {@link CreateCourseRequirementCommand} and call
     * {@link CreateCourseRequirementUseCase}.
     *
     * <p>The cross-batch cycle re-check (WU3 carry-forward SUGGESTION): the curriculum
     * {@link CreateCourseRequirementUseCase} implementation already re-validates acyclicity
     * against the REAL persisted graph (loading all requirements for the plan via
     * {@code SaveCourseRequirementPort#findAllByPlanId}). Since we call this use-case inside
     * this confirm transaction, the persisted graph is the live state — including any rows
     * written earlier in this same confirm loop. This gives us the cross-batch cycle re-check
     * for free, without extra code here.
     */
    private void replayRequirementRow(ImportRow row, ImportBatch batch, ConfirmImportCommand command) {
        UUID rowIdempotencyKey = deterministicRowKey(batch.getId().value(), "REQUIREMENT", row.index());
        String rowHash = deterministicRowHash(batch.getId().value(), "REQUIREMENT", row.index());

        String destinationCourseStr = row.cells().getOrDefault("destination_course", "");
        String requiredCourseStr = row.cells().getOrDefault("required_course_or_value", "");
        String reqType = row.cells().getOrDefault("requirement_type", "COURSE_PASSED");
        boolean simultaneousAllowed = Boolean.parseBoolean(
                row.cells().getOrDefault("simultaneous_allowed", "false"));

        UUID destinationCourseId = parseUuidOrNull(destinationCourseStr);
        UUID requiredCourseId = parseUuidOrNull(requiredCourseStr);

        CreateCourseRequirementCommand reqCommand = new CreateCourseRequirementCommand(
                rowIdempotencyKey,
                rowHash,
                command.actorId(),
                destinationCourseId,
                batch.getPlanId(),
                reqType,
                requiredCourseId,
                null,
                simultaneousAllowed);

        createCourseRequirementUseCase.createCourseRequirement(reqCommand);
    }

    // =========================================================================
    // Private — guards
    // =========================================================================

    private void enforceAssignmentGuard(ConfirmImportCommand command, ImportBatch batch) {
        if ("ADMIN".equalsIgnoreCase(command.actorRole())) {
            return; // ADMIN bypasses assignment check
        }

        // STAFF: resolve ACTIVE staff account (mirrors UploadImportService)
        Optional<UUID> staffId = staffAssignmentPort.resolveActiveStaffId(command.actorEmail());
        if (staffId.isEmpty()) {
            throw ImportAccessDeniedException.inactiveOrUnknownActor(command.actorEmail());
        }

        if (!staffAssignmentPort.isAssigned(staffId.get(), batch.getProgramId())) {
            throw ImportAccessDeniedException.notAssignedToProgram(batch.getProgramId());
        }
    }

    // =========================================================================
    // Private — summary builder
    // =========================================================================

    private ImportSummary toSummary(ImportBatch batch) {
        List<ImportSummary.ImportIssueDto> errors = new ArrayList<>();
        List<ImportSummary.ImportIssueDto> warnings = new ArrayList<>();

        for (ImportIssue issue : batch.getIssues()) {
            ImportSummary.ImportIssueDto dto = new ImportSummary.ImportIssueDto(
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

    // =========================================================================
    // Private — deterministic per-row key helpers
    // =========================================================================

    /**
     * Generate a deterministic UUID from (batchId, kind, rowIndex).
     *
     * <p>This ensures: a retried confirm (same import, same rows) generates the same per-row key
     * and hits the idempotency replay path in the target use-case instead of double-creating.
     * A rolled-back confirm attempt stores NO idempotency record (the idempotency save was inside
     * the TX), so the next attempt re-generates the same key and succeeds.
     */
    private UUID deterministicRowKey(UUID batchId, String kind, int rowIndex) {
        String seed = batchId + ":" + kind + ":" + rowIndex;
        return UUID.nameUUIDFromBytes(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String deterministicRowHash(UUID batchId, String kind, int rowIndex) {
        return "import-row:" + batchId + ":" + kind + ":" + rowIndex;
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
