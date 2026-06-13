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
import co.edu.uniquindio.brujula.imports.domain.model.Severity;
import co.edu.uniquindio.brujula.offering.application.dto.CreateOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.port.in.CreateOfferingUseCase;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: confirm a VALIDATED ImportBatch.
 *
 * <p>Single {@link Transactional} boundary on {@link #confirm}. All calls to {@link
 * CreateCourseUseCase} and {@link CreateCourseRequirementUseCase} run inside this transaction
 * because those adapters use default REQUIRED propagation. Any exception from any row propagates
 * out of this method, rolls back the entire transaction, and leaves the batch in VALIDATED status.
 *
 * <p>Idempotency record is saved INSIDE the transaction so a rolled-back attempt leaves no orphan
 * record.
 *
 * <p>Framework-free except for {@code @Transactional} — wired by {@code ImportsConfiguration}.
 */
public class ConfirmImportService implements ConfirmImportUseCase {

  /**
   * Operation identifier used in the idempotency store. Public so tests can reference it without
   * string duplication.
   */
  public static final String OPERATION = "CONFIRM_IMPORT";

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private final LoadImportBatchPort loadImportBatchPort;
  private final SaveImportBatchPort saveImportBatchPort;
  private final StaffAssignmentPort staffAssignmentPort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;
  private final AuditRecorder auditRecorder;
  private final CreateCourseUseCase createCourseUseCase;
  private final CreateCourseRequirementUseCase createCourseRequirementUseCase;
  private final CreateOfferingUseCase createOfferingUseCase;

  public ConfirmImportService(
      LoadImportBatchPort loadImportBatchPort,
      SaveImportBatchPort saveImportBatchPort,
      StaffAssignmentPort staffAssignmentPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder,
      CreateCourseUseCase createCourseUseCase,
      CreateCourseRequirementUseCase createCourseRequirementUseCase,
      CreateOfferingUseCase createOfferingUseCase) {
    this.loadImportBatchPort = loadImportBatchPort;
    this.saveImportBatchPort = saveImportBatchPort;
    this.staffAssignmentPort = staffAssignmentPort;
    this.idempotencyStorePort = idempotencyStorePort;
    this.jsonCodecPort = jsonCodecPort;
    this.auditRecorder = auditRecorder;
    this.createCourseUseCase = createCourseUseCase;
    this.createCourseRequirementUseCase = createCourseRequirementUseCase;
    this.createOfferingUseCase = createOfferingUseCase;
  }

  /**
   * Confirm a VALIDATED ImportBatch.
   *
   * <p>This method is annotated {@code @Transactional} (default REQUIRED). All curriculum write
   * use-cases that are called inside this method JOIN this transaction because their persistence
   * adapters use REQUIRED propagation. Any exception propagates and rolls back the entire
   * transaction — zero partial commits.
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
    ImportBatch batch =
        loadImportBatchPort
            .findById(ImportBatchId.of(command.importId()))
            .orElseThrow(() -> new ImportBatchNotFoundException(command.importId()));

    // ----------------------------------------------------------------
    // 3. Assignment guard — STAFF only; ADMIN bypasses
    // ----------------------------------------------------------------
    enforceAssignmentGuard(command, batch);

    // ----------------------------------------------------------------
    // 4. Optimistic-lock check: expectedVersion must match stored version
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
    // 5. Domain guard: only VALIDATED is confirmable.
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
    // 10. Audit entry (IMPORT_CONFIRMED)
    // ----------------------------------------------------------------
    auditRecorder.record(
        new RecordAuditEventCommand(
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
   * <p>COURSES → {@link CreateCourseUseCase} with initialStatus=INACTIVE (imported courses land
   * inactive/non-published). REQUIREMENTS → {@link CreateCourseRequirementUseCase} (cross-batch
   * cycle re-check runs inside the real service against the live graph). OFFERINGS → {@link
   * CreateOfferingUseCase} — rows are first GROUPED by {@code (course_id, period, option_label)} so
   * that multiple rows representing different weekly sessions of the same schedule option are
   * submitted as ONE {@link CreateOfferingCommand} with multiple {@link
   * CreateOfferingCommand.SessionDraft}s. Each group → exactly one {@code CreateOfferingUseCase}
   * call.
   *
   * <p>Any exception from a use-case propagates directly — no catch-and-continue (all-or-nothing).
   * The caller's {@code @Transactional} boundary catches the exception and rolls back.
   *
   * @param batch the VALIDATED ImportBatch whose rows to replay
   * @param command the confirm command (carries actorId and planId via batch)
   */
  private void replayRows(ImportBatch batch, ConfirmImportCommand command) {
    if (batch.getKind() == ImportKind.OFFERINGS) {
      replayOfferingsGrouped(batch, command);
      return;
    }
    for (ImportRow row : batch.getValidatedRows()) {
      switch (batch.getKind()) {
        case COURSES -> replayCourseRow(row, batch, command);
        case REQUIREMENTS -> replayRequirementRow(row, batch, command);
        default ->
            throw new UnsupportedOperationException(
                "Unsupported import kind for confirm: " + batch.getKind());
      }
    }
  }

  /**
   * Group OFFERINGS rows by {@code (course_id, period_id, option_label)} and replay each group as a
   * single {@link CreateOfferingUseCase#createOffering} call with all its sessions.
   *
   * <p>Grouping key: {@code courseId + ":OFFERINGS:" + periodId + ":" + optionLabel} (same
   * components used in the idempotency key derivation — deterministic and stable).
   *
   * <p>Idempotency key is derived from the GROUP key, not from a single row index, so that a
   * retried confirm regenerates the same key regardless of CSV row ordering.
   *
   * <p>The existing {@code expandDay} helper is reused for each session's day abbreviation.
   * Insertion order is preserved via {@link LinkedHashMap} so the sessions within a group appear in
   * CSV row order (stable, testable).
   */
  private void replayOfferingsGrouped(ImportBatch batch, ConfirmImportCommand command) {
    // Group rows by (courseId, optionLabel) — period comes from the batch, same for all rows.
    // LinkedHashMap preserves insertion order → groups appear in first-encounter order.
    Map<String, List<ImportRow>> groups = new LinkedHashMap<>();

    for (ImportRow row : batch.getValidatedRows()) {
      String courseId = row.cells().getOrDefault("course", "").trim();
      String optionLabel = row.cells().getOrDefault("option_label", "G1").trim();
      // Group key: courseId + period (from batch) + optionLabel — stable, deterministic
      String groupKey = courseId + ":OFFERINGS:" + batch.getPeriodId() + ":" + optionLabel;
      groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(row);
    }

    for (Map.Entry<String, List<ImportRow>> entry : groups.entrySet()) {
      replayOfferingGroup(entry.getKey(), entry.getValue(), batch, command);
    }
  }

  /**
   * Replay one group of OFFERINGS rows (same course + period + option_label) as a single {@link
   * CreateOfferingUseCase#createOffering} call.
   *
   * @param groupKey deterministic key {@code courseId:OFFERINGS:periodId:optionLabel}
   * @param groupRows rows belonging to this group (all share the same course + option_label)
   * @param batch the parent ImportBatch
   * @param command the confirm command
   */
  private void replayOfferingGroup(
      String groupKey, List<ImportRow> groupRows, ImportBatch batch, ConfirmImportCommand command) {
    // Use the first row of the group for the offering-level fields (course, option_label,
    // journey) — these are identical across all rows in the group by construction.
    ImportRow firstRow = groupRows.getFirst();

    UUID courseId = parseUuidOrNull(firstRow.cells().getOrDefault("course", ""));
    String optionLabel = firstRow.cells().getOrDefault("option_label", "G1");
    String journeyCode = firstRow.cells().getOrDefault("journey", "");

    // Build one SessionDraft per row in the group — each row is a distinct weekly session.
    List<CreateOfferingCommand.SessionDraft> sessions = new ArrayList<>();
    for (ImportRow row : groupRows) {
      String dayStr = expandDay(row.cells().getOrDefault("day", "MON").toUpperCase());
      String startTimeStr = row.cells().getOrDefault("start_time", "");
      String endTimeStr = row.cells().getOrDefault("end_time", "");
      LocalTime startsAt = parseTimeOrNull(startTimeStr);
      LocalTime endsAt = parseTimeOrNull(endTimeStr);
      sessions.add(new CreateOfferingCommand.SessionDraft(dayStr, startsAt, endsAt));
    }

    // One ScheduleOptionDraft containing all sessions of this group.
    CreateOfferingCommand.ScheduleOptionDraft option =
        new CreateOfferingCommand.ScheduleOptionDraft(
            optionLabel, journeyCode.isBlank() ? null : journeyCode, sessions);

    // Idempotency key derived from the GROUP key (not row index) — deterministic per group.
    // A retried confirm regenerates the same key for the same group → hits idempotency replay
    // in CreateOfferingService rather than double-creating.
    UUID groupIdempotencyKey =
        UUID.nameUUIDFromBytes(groupKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String groupHash = "import-group:" + groupKey;

    CreateOfferingCommand offeringCommand =
        new CreateOfferingCommand(
            groupIdempotencyKey,
            command.actorId(),
            batch.getProgramId(),
            courseId,
            batch.getPeriodId(),
            null, // no notes for import-originated offerings
            List.of(option),
            groupHash);

    createOfferingUseCase.createOffering(offeringCommand);
  }

  /**
   * Map a COURSES row to a {@link CreateCourseCommand} and call {@link CreateCourseUseCase}.
   *
   * <p>initialStatus is set to "INACTIVE" so imported courses land inactive/non-published.
   * Auto-publish is forbidden.
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
    int recommendedSemester =
        parseIntOrDefault(row.cells().getOrDefault("recommended_semester", "1"), 1);
    String officialCode = row.cells().get("official_code");

    // Map recommended_semester to a single RecommendedSemesterView (journey from batch context)
    // For import, we create a single recommendation without a journey code if not specified.
    String journeyCode = row.cells().get("journey");
    List<co.edu.uniquindio.brujula.curriculum.application.dto.CourseView.RecommendedSemesterView>
        semesters;
    if (journeyCode != null && !journeyCode.isBlank()) {
      semesters =
          List.of(
              new co.edu.uniquindio.brujula.curriculum.application.dto.CourseView
                  .RecommendedSemesterView(journeyCode, recommendedSemester));
    } else {
      semesters = List.of();
    }

    CreateCourseCommand courseCommand =
        new CreateCourseCommand(
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
            "INACTIVE"); // imported courses land INACTIVE (not ACTIVE/published)

    createCourseUseCase.createCourse(courseCommand);
  }

  /**
   * Map a REQUIREMENTS row to a {@link CreateCourseRequirementCommand} and call {@link
   * CreateCourseRequirementUseCase}.
   *
   * <p>The curriculum {@link CreateCourseRequirementUseCase} implementation already re-validates
   * acyclicity against the REAL persisted graph (loading all requirements for the plan via {@code
   * SaveCourseRequirementPort#findAllByPlanId}). Since we call this use-case inside this confirm
   * transaction, the persisted graph is the live state — including any rows written earlier in this
   * same confirm loop. This gives us the cross-batch cycle re-check for free, without extra code
   * here.
   */
  private void replayRequirementRow(
      ImportRow row, ImportBatch batch, ConfirmImportCommand command) {
    UUID rowIdempotencyKey = deterministicRowKey(batch.getId().value(), "REQUIREMENT", row.index());
    String rowHash = deterministicRowHash(batch.getId().value(), "REQUIREMENT", row.index());

    String destinationCourseStr = row.cells().getOrDefault("destination_course", "");
    String requiredCourseStr = row.cells().getOrDefault("required_course_or_value", "");
    String reqType = row.cells().getOrDefault("requirement_type", "COURSE_PASSED");
    boolean simultaneousAllowed =
        Boolean.parseBoolean(row.cells().getOrDefault("simultaneous_allowed", "false"));

    UUID destinationCourseId = parseUuidOrNull(destinationCourseStr);
    UUID requiredCourseId = parseUuidOrNull(requiredCourseStr);

    CreateCourseRequirementCommand reqCommand =
        new CreateCourseRequirementCommand(
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
      ImportSummary.ImportIssueDto dto =
          new ImportSummary.ImportIssueDto(
              issue.row(), issue.column(), issue.code(), issue.message(), issue.severity().name());
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
   * <p>This ensures: a retried confirm (same import, same rows) generates the same per-row key and
   * hits the idempotency replay path in the target use-case instead of double-creating. A
   * rolled-back confirm attempt stores NO idempotency record (the idempotency save was inside the
   * TX), so the next attempt re-generates the same key and succeeds.
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

  private LocalTime parseTimeOrNull(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return LocalTime.parse(value.trim(), TIME_FORMATTER);
    } catch (java.time.format.DateTimeParseException e) {
      return null;
    }
  }

  /**
   * Translates abbreviated day values accepted by {@link OfferingsValidator} (e.g. "MON") into the
   * full names required by {@link co.edu.uniquindio.brujula.offering.domain.Weekday} (e.g.
   * "MONDAY"). An unrecognised abbreviation is returned as-is (already uppercased), so {@code
   * Weekday.valueOf()} will throw a clear {@link IllegalArgumentException} with the unrecognised
   * value — making the error visible rather than silently swallowed.
   */
  private String expandDay(String abbr) {
    return switch (abbr) {
      case "MON" -> "MONDAY";
      case "TUE" -> "TUESDAY";
      case "WED" -> "WEDNESDAY";
      case "THU" -> "THURSDAY";
      case "FRI" -> "FRIDAY";
      case "SAT" -> "SATURDAY";
      case "SUN" -> "SUNDAY";
      default -> abbr;
    };
  }
}
