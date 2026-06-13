package co.edu.uniquindio.brujula.imports.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseRequirementUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseUseCase;
import co.edu.uniquindio.brujula.imports.application.dto.ConfirmImportCommand;
import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;
import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.SaveImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.StaffAssignmentPort;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotConfirmableException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotFoundException;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;
import co.edu.uniquindio.brujula.offering.application.dto.CreateOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.port.in.CreateOfferingUseCase;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * TDD T6.2 — Unit tests for {@link ConfirmImportService}.
 *
 * <p>All ports are mocked. Tests cover: idempotency replay/conflict, not-found (404), assignment
 * denied (403), inactive staff, optimistic-lock (409), INVALID batch → not confirmable (409), happy
 * path → CONFIRMED + audit + rows replayed, one-row-fails → exception propagates + markConfirmed
 * NOT called.
 */
class ConfirmImportServiceTest {

  private LoadImportBatchPort loadImportBatchPort;
  private SaveImportBatchPort saveImportBatchPort;
  private StaffAssignmentPort staffAssignmentPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AuditRecorder auditRecorder;
  private CreateCourseUseCase createCourseUseCase;
  private CreateCourseRequirementUseCase createCourseRequirementUseCase;
  private CreateOfferingUseCase createOfferingUseCase;
  private ConfirmImportService service;

  private static final UUID PROGRAM_ID = UUID.fromString("00000001-0000-0000-0000-000000000001");
  private static final UUID PLAN_ID = UUID.fromString("00000001-0000-0000-0000-000000000002");
  private static final UUID ACTOR_ID = UUID.fromString("00000001-0000-0000-0000-000000000003");
  private static final String ACTOR_EMAIL = "staff@test.com";
  private static final UUID STAFF_ID = UUID.fromString("00000001-0000-0000-0000-000000000004");

  @BeforeEach
  void setUp() {
    loadImportBatchPort = mock(LoadImportBatchPort.class);
    saveImportBatchPort = mock(SaveImportBatchPort.class);
    staffAssignmentPort = mock(StaffAssignmentPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    auditRecorder = mock(AuditRecorder.class);
    createCourseUseCase = mock(CreateCourseUseCase.class);
    createCourseRequirementUseCase = mock(CreateCourseRequirementUseCase.class);
    createOfferingUseCase = mock(CreateOfferingUseCase.class);

    service =
        new ConfirmImportService(
            loadImportBatchPort,
            saveImportBatchPort,
            staffAssignmentPort,
            idempotencyStorePort,
            jsonCodecPort,
            auditRecorder,
            createCourseUseCase,
            createCourseRequirementUseCase,
            createOfferingUseCase);
  }

  // -------------------------------------------------------------------------
  // T6.2-1 — Idempotent replay (same key + same hash)
  // -------------------------------------------------------------------------

  @Test
  void idempotent_retry_same_key_same_hash_returns_stored_summary() {
    UUID idempotencyKey = UUID.randomUUID();
    ImportSummary storedSummary = confirmedSummary(UUID.randomUUID());
    String storedJson = "{\"stored\":true}";

    IdempotencyStorePort.StoredRecord record =
        new IdempotencyStorePort.StoredRecord("same-hash", 200, storedJson);
    when(idempotencyStorePort.find(idempotencyKey, ConfirmImportService.OPERATION))
        .thenReturn(Optional.of(record));
    when(jsonCodecPort.fromJson(storedJson, ImportSummary.class)).thenReturn(storedSummary);

    ConfirmImportCommand command = command(idempotencyKey, "same-hash", 1L);
    ImportSummary result = service.confirm(command);

    assertThat(result).isEqualTo(storedSummary);
    verify(loadImportBatchPort, never()).findById(any());
  }

  // -------------------------------------------------------------------------
  // T6.2-2 — Idempotency key conflict (same key + different hash)
  // -------------------------------------------------------------------------

  @Test
  void idempotency_conflict_different_hash_throws() {
    UUID idempotencyKey = UUID.randomUUID();
    IdempotencyStorePort.StoredRecord record =
        new IdempotencyStorePort.StoredRecord("original-hash", 200, "{}");
    when(idempotencyStorePort.find(idempotencyKey, ConfirmImportService.OPERATION))
        .thenReturn(Optional.of(record));

    ConfirmImportCommand command = command(idempotencyKey, "different-hash", 1L);

    assertThatThrownBy(() -> service.confirm(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);
    verify(loadImportBatchPort, never()).findById(any());
  }

  // -------------------------------------------------------------------------
  // T6.2-3 — Batch not found (→ 404)
  // -------------------------------------------------------------------------

  @Test
  void batch_not_found_throws_ImportBatchNotFoundException() {
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(any())).thenReturn(Optional.empty());

    ConfirmImportCommand command = command(UUID.randomUUID(), "hash", 1L);

    assertThatThrownBy(() -> service.confirm(command))
        .isInstanceOf(ImportBatchNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // T6.2-4 — STAFF not assigned to program (→ 403)
  // -------------------------------------------------------------------------

  @Test
  void staff_not_assigned_throws_ImportAccessDeniedException() {
    ImportBatch batch = validatedCoursesBatch(2, 1L);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
    when(staffAssignmentPort.resolveActiveStaffId(ACTOR_EMAIL)).thenReturn(Optional.of(STAFF_ID));
    when(staffAssignmentPort.isAssigned(STAFF_ID, PROGRAM_ID)).thenReturn(false);

    ConfirmImportCommand command = command(UUID.randomUUID(), "hash", 1L, batch.getId());

    assertThatThrownBy(() -> service.confirm(command))
        .isInstanceOf(ImportAccessDeniedException.class);
    verify(saveImportBatchPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // T6.2-5 — Inactive/unknown STAFF account (→ 403)
  // -------------------------------------------------------------------------

  @Test
  void inactive_staff_throws_ImportAccessDeniedException() {
    ImportBatch batch = validatedCoursesBatch(2, 1L);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
    when(staffAssignmentPort.resolveActiveStaffId(ACTOR_EMAIL)).thenReturn(Optional.empty());

    ConfirmImportCommand command = command(UUID.randomUUID(), "hash", 1L, batch.getId());

    assertThatThrownBy(() -> service.confirm(command))
        .isInstanceOf(ImportAccessDeniedException.class);
    verify(saveImportBatchPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // T6.2-6 — Version mismatch (→ 409 optimistic lock)
  // -------------------------------------------------------------------------

  @Test
  void version_mismatch_throws_OptimisticLockException() {
    ImportBatch batch = validatedCoursesBatch(2, 2L); // stored version=2
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
    // ADMIN bypasses assignment check
    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 1L, batch.getId()); // expectedVersion=1 != stored=2

    assertThatThrownBy(() -> service.confirm(command))
        .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
    verify(saveImportBatchPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // T6.2-7 — INVALID batch → not confirmable (→ 409)
  // -------------------------------------------------------------------------

  @Test
  void invalid_batch_not_confirmable_throws() {
    ImportBatch batch = invalidBatch();
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));

    ConfirmImportCommand command = adminCommand(UUID.randomUUID(), "hash", 0L, batch.getId());

    assertThatThrownBy(() -> service.confirm(command))
        .isInstanceOf(ImportBatchNotConfirmableException.class);
    verify(saveImportBatchPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // T6.2-8 — Happy path COURSES: batch confirmed + audit + rows replayed
  // -------------------------------------------------------------------------

  @Test
  void happy_path_courses_batch_confirmed_and_audit_recorded() {
    UUID importId = UUID.randomUUID();
    ImportBatch batch = validatedCoursesWithRows(importId, 2);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));

    CourseView courseView = minimalCourseView();
    when(createCourseUseCase.createCourse(any())).thenReturn(courseView);
    when(saveImportBatchPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 0L, ImportBatchId.of(importId));

    ImportSummary result = service.confirm(command);

    assertThat(result.status()).isEqualTo("CONFIRMED");
    verify(createCourseUseCase, times(2)).createCourse(any()); // batch has 2 rows
    verify(auditRecorder).record(any());
    // ImportBatch.markConfirmed was called — the saved batch should be CONFIRMED
    ArgumentCaptor<ImportBatch> batchCaptor = ArgumentCaptor.forClass(ImportBatch.class);
    verify(saveImportBatchPort).save(batchCaptor.capture());
    assertThat(batchCaptor.getValue().getStatus()).isEqualTo(ImportStatus.CONFIRMED);
  }

  // -------------------------------------------------------------------------
  // T6.2-9 — Happy path REQUIREMENTS: rows replayed through requirement use-case
  // -------------------------------------------------------------------------

  @Test
  void happy_path_requirements_rows_replayed() {
    UUID importId = UUID.randomUUID();
    ImportBatch batch = validatedRequirementsWithRows(importId, 1);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));

    CourseRequirementView reqView = minimalRequirementView();
    when(createCourseRequirementUseCase.createCourseRequirement(any())).thenReturn(reqView);
    when(saveImportBatchPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 0L, ImportBatchId.of(importId));

    ImportSummary result = service.confirm(command);

    assertThat(result.status()).isEqualTo("CONFIRMED");
    verify(createCourseRequirementUseCase).createCourseRequirement(any());
    verify(createCourseUseCase, never()).createCourse(any());
  }

  // -------------------------------------------------------------------------
  // T6.2-10 — One row fails → exception propagates; markConfirmed NOT called
  // -------------------------------------------------------------------------

  @Test
  void one_row_failure_propagates_and_batch_not_marked_confirmed() {
    UUID importId = UUID.randomUUID();
    ImportBatch batch = validatedCoursesWithRows(importId, 1);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));

    when(createCourseUseCase.createCourse(any()))
        .thenThrow(new IllegalStateException("constraint violation"));

    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 0L, ImportBatchId.of(importId));

    assertThatThrownBy(() -> service.confirm(command)).isInstanceOf(IllegalStateException.class);
    // markConfirmed was NOT called → saveImportBatchPort.save should not be called
    verify(saveImportBatchPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  // -------------------------------------------------------------------------
  // T6.2-11 — ADMIN bypasses assignment check
  // -------------------------------------------------------------------------

  @Test
  void admin_bypasses_assignment_check() {
    UUID importId = UUID.randomUUID();
    ImportBatch batch = validatedCoursesWithRows(importId, 1);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
    when(createCourseUseCase.createCourse(any())).thenReturn(minimalCourseView());
    when(saveImportBatchPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 0L, ImportBatchId.of(importId));

    ImportSummary result = service.confirm(command);

    assertThat(result.status()).isEqualTo("CONFIRMED");
    // Assignment port should NOT be called for ADMIN
    verify(staffAssignmentPort, never()).resolveActiveStaffId(any());
    verify(staffAssignmentPort, never()).isAssigned(any(), any());
  }

  // -------------------------------------------------------------------------
  // Imported course row is mapped with INACTIVE initial status
  // -------------------------------------------------------------------------

  @Test
  void courses_row_mapped_with_inactive_initial_status() {
    // Imported courses must land INACTIVE
    UUID importId = UUID.randomUUID();
    ImportBatch batch = validatedCoursesWithRows(importId, 1);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
    when(createCourseUseCase.createCourse(any())).thenReturn(minimalCourseView());
    when(saveImportBatchPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 0L, ImportBatchId.of(importId));
    service.confirm(command);

    ArgumentCaptor<co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseCommand>
        captor =
            ArgumentCaptor.forClass(
                co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseCommand.class);
    verify(createCourseUseCase).createCourse(captor.capture());
    assertThat(captor.getValue().initialStatus()).isEqualTo("INACTIVE");
  }

  // -------------------------------------------------------------------------
  // Happy path OFFERINGS: batch confirmed, offering created in DRAFT, audit recorded
  // -------------------------------------------------------------------------

  @Test
  void offerings_import_creates_offering_entities_and_batch_confirmed() {
    UUID importId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();
    ImportBatch batch = validatedOfferingsBatch(importId, periodId);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
    when(createOfferingUseCase.createOffering(any()))
        .thenReturn(minimalOfferingDetailView(periodId));
    when(saveImportBatchPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 0L, ImportBatchId.of(importId));

    ImportSummary result = service.confirm(command);

    assertThat(result.status()).isEqualTo("CONFIRMED");
    verify(createOfferingUseCase, times(1)).createOffering(any());
    verify(createCourseUseCase, never()).createCourse(any());
    verify(createCourseRequirementUseCase, never()).createCourseRequirement(any());
    verify(auditRecorder).record(any());
    ArgumentCaptor<ImportBatch> batchCaptor = ArgumentCaptor.forClass(ImportBatch.class);
    verify(saveImportBatchPort).save(batchCaptor.capture());
    assertThat(batchCaptor.getValue().getStatus()).isEqualTo(ImportStatus.CONFIRMED);
  }

  // -------------------------------------------------------------------------
  // CreateOfferingCommand uses DRAFT status (offerings land DRAFT)
  // -------------------------------------------------------------------------

  @Test
  void offerings_row_maps_to_CreateOfferingCommand_with_correct_fields() {
    // Verify the command shape sent to createOfferingUseCase
    UUID importId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    ImportBatch batch = validatedOfferingsBatchWithCourse(importId, periodId, courseId);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
    when(createOfferingUseCase.createOffering(any()))
        .thenReturn(minimalOfferingDetailView(periodId));
    when(saveImportBatchPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 0L, ImportBatchId.of(importId));
    service.confirm(command);

    ArgumentCaptor<CreateOfferingCommand> captor =
        ArgumentCaptor.forClass(CreateOfferingCommand.class);
    verify(createOfferingUseCase).createOffering(captor.capture());
    CreateOfferingCommand sent = captor.getValue();

    // CreateOffering always creates in DRAFT — no explicit status field on CreateOfferingCommand,
    // but CreateOfferingService.createOffering calls CourseOffering.create() which always starts
    // DRAFT. Assert the command carries the expected structural fields.
    assertThat(sent.periodId()).isEqualTo(periodId);
    assertThat(sent.courseId()).isEqualTo(courseId);
    assertThat(sent.programId()).isEqualTo(batch.getProgramId());
    assertThat(sent.scheduleOptions()).hasSize(1);
    CreateOfferingCommand.ScheduleOptionDraft option = sent.scheduleOptions().getFirst();
    assertThat(option.label()).isEqualTo("G1");
    assertThat(option.journeyCode()).isEqualTo("DIURNA");
    assertThat(option.sessions()).hasSize(1);
    CreateOfferingCommand.SessionDraft session = option.sessions().getFirst();
    assertThat(session.day()).isEqualTo("MONDAY");
    assertThat(session.startsAt()).isEqualTo(java.time.LocalTime.of(8, 0));
    assertThat(session.endsAt()).isEqualTo(java.time.LocalTime.of(10, 0));
  }

  // -------------------------------------------------------------------------
  // One offering row fails → exception propagates, batch NOT confirmed
  // -------------------------------------------------------------------------

  @Test
  void offerings_one_row_failure_propagates_and_batch_not_confirmed() {
    UUID importId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();
    ImportBatch batch = validatedOfferingsBatch(importId, periodId);
    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
    when(createOfferingUseCase.createOffering(any()))
        .thenThrow(new IllegalStateException("offering constraint violation"));

    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 0L, ImportBatchId.of(importId));

    assertThatThrownBy(() -> service.confirm(command)).isInstanceOf(IllegalStateException.class);
    verify(saveImportBatchPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  // -------------------------------------------------------------------------
  // Multi-session grouping: 3 rows same option → 1 createOffering call with 3 sessions
  // -------------------------------------------------------------------------

  /**
   * Three rows sharing the same (course_id, period, option_label) on different weekdays MUST result
   * in exactly ONE call to createOfferingUseCase with ONE CreateOfferingCommand containing 3
   * sessions. Before the grouping fix, this produces 3 separate calls (fragmented).
   */
  @Test
  void three_rows_same_option_different_days_grouped_into_one_offering_with_three_sessions() {
    UUID importId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    ImportBatch batch = threeRowSameOptionBatch(importId, periodId, courseId);

    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
    when(createOfferingUseCase.createOffering(any()))
        .thenReturn(minimalOfferingDetailView(periodId));
    when(saveImportBatchPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 0L, ImportBatchId.of(importId));

    ImportSummary result = service.confirm(command);

    assertThat(result.status()).isEqualTo("CONFIRMED");

    // THE GROUPING ASSERTION: exactly 1 call, not 3
    ArgumentCaptor<CreateOfferingCommand> captor =
        ArgumentCaptor.forClass(CreateOfferingCommand.class);
    verify(createOfferingUseCase, times(1)).createOffering(captor.capture());

    CreateOfferingCommand sent = captor.getValue();
    assertThat(sent.courseId())
        .as("Grouped command must reference the shared courseId")
        .isEqualTo(courseId);
    assertThat(sent.periodId())
        .as("Grouped command must reference the shared periodId")
        .isEqualTo(periodId);
    assertThat(sent.scheduleOptions())
        .as("Grouped command must have exactly 1 schedule option")
        .hasSize(1);

    CreateOfferingCommand.ScheduleOptionDraft option = sent.scheduleOptions().getFirst();
    assertThat(option.label()).as("Option label must be G1").isEqualTo("G1");
    assertThat(option.sessions())
        .as("The single option must carry 3 sessions (MON, WED, FRI)")
        .hasSize(3);

    // Verify all 3 weekdays are represented in the sessions
    List<String> days =
        option.sessions().stream().map(CreateOfferingCommand.SessionDraft::day).toList();
    assertThat(days)
        .as("Sessions must contain MONDAY, WEDNESDAY, FRIDAY in some order")
        .containsExactlyInAnyOrder("MONDAY", "WEDNESDAY", "FRIDAY");
  }

  /**
   * Exception propagation: when createOfferingUseCase throws on the FIRST offering group, the
   * exception propagates and the batch is NOT marked CONFIRMED.
   *
   * <p>NOTE: This test proves EXCEPTION PROPAGATION only (the invalid group throws on the first
   * createOffering call, so no group writes anything before the failure). It does NOT prove real DB
   * rollback of an already-written group. The authoritative rollback proof is in
   * ConfirmImportOfferingsIT, where a valid group is written within the TX before the invalid group
   * fails, and COUNT==0 after rollback confirms full-TX atomicity.
   *
   * <p>This tests that the all-or-nothing TX invariant is preserved after the grouping refactor.
   * Since grouping produces fewer calls, a failure on ANY group must roll back ALL prior groups.
   */
  @Test
  void atomicity_exception_propagation_batch_not_confirmed_on_first_offering_failure() {
    UUID importId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();
    UUID courseId1 = UUID.randomUUID();
    UUID courseId2 = UUID.randomUUID();
    // Batch with 2 distinct offerings: courseId1/G1 (2 sessions) and courseId2/G2 (1 session)
    ImportBatch batch = twoGroupOfferingsBatch(importId, periodId, courseId1, courseId2);

    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
    // First call throws — simulates constraint violation on the first offering group
    when(createOfferingUseCase.createOffering(any()))
        .thenThrow(new IllegalStateException("constraint violation on offering group 1"));

    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 0L, ImportBatchId.of(importId));

    assertThatThrownBy(() -> service.confirm(command))
        .as("Exception from first offering group must propagate (all-or-nothing)")
        .isInstanceOf(IllegalStateException.class);

    // Batch must NOT be saved/confirmed
    verify(saveImportBatchPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  // -------------------------------------------------------------------------
  // Distinct option_label on same course + period → two separate offerings
  //   Guards against a regression where the group key drops the optionLabel component,
  //   which would silently over-merge G1 and G2 into a single offering.
  // -------------------------------------------------------------------------

  /**
   * Two rows with the same courseId AND the same periodId but DIFFERENT option_label ("G1" on MON,
   * "G2" on TUE) MUST produce exactly TWO separate createOffering calls, each with a distinct
   * option label. A bug that strips optionLabel from the group key would collapse them into one
   * call — this test would go RED immediately.
   */
  @Test
  void two_options_same_course_produce_two_separate_offerings() {
    UUID importId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    ImportBatch batch = sameCourseTwoOptionsBatch(importId, periodId, courseId);

    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
    when(createOfferingUseCase.createOffering(any()))
        .thenReturn(minimalOfferingDetailView(periodId));
    when(saveImportBatchPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    ConfirmImportCommand command =
        adminCommand(UUID.randomUUID(), "hash", 0L, ImportBatchId.of(importId));

    ImportSummary result = service.confirm(command);

    assertThat(result.status()).isEqualTo("CONFIRMED");

    // THE OVER-MERGE GUARD: same course + same period + different option_label → 2 calls
    ArgumentCaptor<CreateOfferingCommand> captor =
        ArgumentCaptor.forClass(CreateOfferingCommand.class);
    verify(createOfferingUseCase, times(2)).createOffering(captor.capture());

    List<CreateOfferingCommand> commands = captor.getAllValues();
    assertThat(commands).hasSize(2);

    // Both commands must reference the SAME courseId and periodId
    assertThat(commands)
        .allSatisfy(
            cmd -> {
              assertThat(cmd.courseId()).isEqualTo(courseId);
              assertThat(cmd.periodId()).isEqualTo(periodId);
              assertThat(cmd.scheduleOptions()).hasSize(1);
            });

    // Each command must carry a DISTINCT option label
    List<String> capturedLabels =
        commands.stream().map(cmd -> cmd.scheduleOptions().getFirst().label()).toList();
    assertThat(capturedLabels)
        .as("G1 and G2 must NOT be merged into a single offering")
        .containsExactlyInAnyOrder("G1", "G2");
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private ConfirmImportCommand command(UUID key, String hash, long expectedVersion) {
    return command(key, hash, expectedVersion, ImportBatchId.generate());
  }

  private ConfirmImportCommand command(
      UUID key, String hash, long expectedVersion, ImportBatchId importId) {
    return new ConfirmImportCommand(
        importId.value(), ACTOR_EMAIL, "STAFF", ACTOR_ID, expectedVersion, key, hash);
  }

  private ConfirmImportCommand adminCommand(
      UUID key, String hash, long expectedVersion, ImportBatchId importId) {
    return new ConfirmImportCommand(
        importId.value(), "admin@test.com", "ADMIN", ACTOR_ID, expectedVersion, key, hash);
  }

  private ImportBatch validatedCoursesBatch(int validRows, long version) {
    ImportBatchId id = ImportBatchId.generate();
    return ImportBatch.reconstitute(
        id,
        ImportKind.COURSES,
        PROGRAM_ID,
        PLAN_ID,
        null,
        ImportStatus.VALIDATED,
        validRows,
        validRows,
        List.of(),
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "course_name", "Test Course",
                    "credits", "3",
                    "weekly_hours", "4",
                    "type", "REQUIRED",
                    "recommended_semester", "2",
                    "can_be_enabled", "true"))),
        ImportBatch.CURRENT_SCHEMA_VERSION,
        version,
        Instant.now());
  }

  private ImportBatch validatedCoursesWithRows(UUID importId, int rowCount) {
    List<ImportRow> rows = new java.util.ArrayList<>();
    for (int i = 0; i < rowCount; i++) {
      rows.add(
          new ImportRow(
              i,
              Map.of(
                  "course_name", "Course " + i,
                  "credits", "3",
                  "weekly_hours", "4",
                  "type", "REQUIRED",
                  "recommended_semester", "1",
                  "can_be_enabled", "true")));
    }
    return ImportBatch.reconstitute(
        ImportBatchId.of(importId),
        ImportKind.COURSES,
        PROGRAM_ID,
        PLAN_ID,
        null,
        ImportStatus.VALIDATED,
        rowCount,
        rowCount,
        List.of(),
        rows,
        ImportBatch.CURRENT_SCHEMA_VERSION,
        0L,
        Instant.now());
  }

  private ImportBatch validatedRequirementsWithRows(UUID importId, int rowCount) {
    List<ImportRow> rows = new java.util.ArrayList<>();
    UUID destCourse = UUID.randomUUID();
    UUID reqCourse = UUID.randomUUID();
    for (int i = 0; i < rowCount; i++) {
      rows.add(
          new ImportRow(
              i,
              Map.of(
                  "destination_course",
                  destCourse.toString(),
                  "requirement_type",
                  "COURSE_PASSED",
                  "required_course_or_value",
                  reqCourse.toString(),
                  "simultaneous_allowed",
                  "false")));
    }
    return ImportBatch.reconstitute(
        ImportBatchId.of(importId),
        ImportKind.REQUIREMENTS,
        PROGRAM_ID,
        PLAN_ID,
        null,
        ImportStatus.VALIDATED,
        rowCount,
        rowCount,
        List.of(),
        rows,
        ImportBatch.CURRENT_SCHEMA_VERSION,
        0L,
        Instant.now());
  }

  private ImportBatch validatedOfferingsBatch(UUID importId, UUID periodId) {
    UUID courseId = UUID.randomUUID();
    return validatedOfferingsBatchWithCourse(importId, periodId, courseId);
  }

  private ImportBatch validatedOfferingsBatchWithCourse(
      UUID importId, UUID periodId, UUID courseId) {
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "course", courseId.toString(),
                    "option_label", "G1",
                    "journey", "DIURNA",
                    "day", "MON",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")));
    return ImportBatch.reconstitute(
        ImportBatchId.of(importId),
        ImportKind.OFFERINGS,
        PROGRAM_ID,
        null,
        periodId,
        ImportStatus.VALIDATED,
        1,
        1,
        List.of(),
        rows,
        ImportBatch.CURRENT_SCHEMA_VERSION,
        0L,
        Instant.now());
  }

  private OfferingDetailView minimalOfferingDetailView(UUID periodId) {
    return new OfferingDetailView(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Test Course",
        PROGRAM_ID,
        periodId,
        "DRAFT",
        null,
        0L,
        List.of());
  }

  private ImportBatch invalidBatch() {
    return ImportBatch.invalid(
        ImportBatchId.generate(),
        ImportKind.COURSES,
        PROGRAM_ID,
        PLAN_ID,
        null,
        0,
        3,
        List.of(ImportIssue.error(0, null, "EMPTY_FILE", "No data rows found")),
        Instant.now());
  }

  private CourseView minimalCourseView() {
    return new CourseView(
        UUID.randomUUID(),
        PLAN_ID,
        PROGRAM_ID,
        null,
        "Test Course",
        "REQUIRED",
        3,
        4,
        List.of(),
        "INACTIVE",
        0L);
  }

  private CourseRequirementView minimalRequirementView() {
    return new CourseRequirementView(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "COURSE_PASSED",
        UUID.randomUUID(),
        null,
        false,
        null,
        false,
        "ACTIVE",
        0L);
  }

  private ImportSummary confirmedSummary(UUID id) {
    return new ImportSummary(
        id,
        "COURSES",
        PROGRAM_ID,
        PLAN_ID,
        null,
        "CONFIRMED",
        3,
        3,
        List.of(),
        List.of(),
        Instant.now(),
        1L);
  }

  /**
   * Creates a VALIDATED OFFERINGS ImportBatch with 3 rows for the same (course, option_label) on
   * different weekdays (MON, WED, FRI). Models a 3-session weekly offering.
   */
  private ImportBatch threeRowSameOptionBatch(UUID importId, UUID periodId, UUID courseId) {
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "course", courseId.toString(),
                    "option_label", "G1",
                    "journey", "DIURNA",
                    "day", "MON",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")),
            new ImportRow(
                1,
                Map.of(
                    "course", courseId.toString(),
                    "option_label", "G1",
                    "journey", "DIURNA",
                    "day", "WED",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")),
            new ImportRow(
                2,
                Map.of(
                    "course", courseId.toString(),
                    "option_label", "G1",
                    "journey", "DIURNA",
                    "day", "FRI",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")));
    return ImportBatch.reconstitute(
        ImportBatchId.of(importId),
        ImportKind.OFFERINGS,
        PROGRAM_ID,
        null,
        periodId,
        ImportStatus.VALIDATED,
        3,
        3,
        List.of(),
        rows,
        ImportBatch.CURRENT_SCHEMA_VERSION,
        0L,
        Instant.now());
  }

  /**
   * Creates a VALIDATED OFFERINGS ImportBatch with 2 distinct offering groups: - group 1: courseId1
   * / G1 (2 rows: MON + WED) - group 2: courseId2 / G2 (1 row: FRI) Used to test atomicity with
   * grouped confirm.
   */
  private ImportBatch twoGroupOfferingsBatch(
      UUID importId, UUID periodId, UUID courseId1, UUID courseId2) {
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "course", courseId1.toString(),
                    "option_label", "G1",
                    "journey", "DIURNA",
                    "day", "MON",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")),
            new ImportRow(
                1,
                Map.of(
                    "course", courseId1.toString(),
                    "option_label", "G1",
                    "journey", "DIURNA",
                    "day", "WED",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")),
            new ImportRow(
                2,
                Map.of(
                    "course", courseId2.toString(),
                    "option_label", "G2",
                    "journey", "NOCTURNA",
                    "day", "FRI",
                    "start_time", "18:00",
                    "end_time", "20:00",
                    "status", "ACTIVE")));
    return ImportBatch.reconstitute(
        ImportBatchId.of(importId),
        ImportKind.OFFERINGS,
        PROGRAM_ID,
        null,
        periodId,
        ImportStatus.VALIDATED,
        3,
        3,
        List.of(),
        rows,
        ImportBatch.CURRENT_SCHEMA_VERSION,
        0L,
        Instant.now());
  }

  /**
   * Creates a VALIDATED OFFERINGS ImportBatch with 2 rows for the SAME courseId and SAME periodId
   * but DIFFERENT option_label: G1 on MON and G2 on TUE. Guards against the over-merge regression
   * where the group key accidentally drops the optionLabel component and collapses both groups into
   * one offering.
   */
  private ImportBatch sameCourseTwoOptionsBatch(UUID importId, UUID periodId, UUID courseId) {
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "course", courseId.toString(),
                    "option_label", "G1",
                    "journey", "DIURNA",
                    "day", "MON",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")),
            new ImportRow(
                1,
                Map.of(
                    "course", courseId.toString(),
                    "option_label", "G2",
                    "journey", "DIURNA",
                    "day", "TUE",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")));
    return ImportBatch.reconstitute(
        ImportBatchId.of(importId),
        ImportKind.OFFERINGS,
        PROGRAM_ID,
        null,
        periodId,
        ImportStatus.VALIDATED,
        2,
        2,
        List.of(),
        rows,
        ImportBatch.CURRENT_SCHEMA_VERSION,
        0L,
        Instant.now());
  }
}
