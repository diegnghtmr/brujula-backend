package co.edu.uniquindio.brujula.imports.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * TDD T6.2 — Unit tests for {@link ConfirmImportService}.
 *
 * <p>All ports are mocked. Tests cover: idempotency replay/conflict, not-found (404),
 * assignment denied (403), inactive staff, optimistic-lock (409), INVALID batch → not
 * confirmable (409), happy path → CONFIRMED + audit + rows replayed, one-row-fails →
 * exception propagates + markConfirmed NOT called.
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

        service = new ConfirmImportService(
                loadImportBatchPort,
                saveImportBatchPort,
                staffAssignmentPort,
                idempotencyStorePort,
                jsonCodecPort,
                auditRecorder,
                createCourseUseCase,
                createCourseRequirementUseCase);
    }

    // -------------------------------------------------------------------------
    // T6.2-1 — Idempotent replay (same key + same hash)
    // -------------------------------------------------------------------------

    @Test
    void idempotent_retry_same_key_same_hash_returns_stored_summary() {
        // SC-005-E
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
        ConfirmImportCommand command = adminCommand(UUID.randomUUID(), "hash", 1L, batch.getId()); // expectedVersion=1 != stored=2

        assertThatThrownBy(() -> service.confirm(command))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
        verify(saveImportBatchPort, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // T6.2-7 — INVALID batch → not confirmable (→ 409)
    // -------------------------------------------------------------------------

    @Test
    void invalid_batch_not_confirmable_throws() {
        // INV-02
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
        // SC-005-A, IMP-011-B, SC-011-B
        UUID importId = UUID.randomUUID();
        ImportBatch batch = validatedCoursesWithRows(importId, 2);
        when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
        when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));

        CourseView courseView = minimalCourseView();
        when(createCourseUseCase.createCourse(any())).thenReturn(courseView);
        when(saveImportBatchPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jsonCodecPort.toJson(any())).thenReturn("{}");

        ConfirmImportCommand command = adminCommand(UUID.randomUUID(), "hash", 0L,
                ImportBatchId.of(importId));

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

        ConfirmImportCommand command = adminCommand(UUID.randomUUID(), "hash", 0L,
                ImportBatchId.of(importId));

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

        ConfirmImportCommand command = adminCommand(UUID.randomUUID(), "hash", 0L,
                ImportBatchId.of(importId));

        assertThatThrownBy(() -> service.confirm(command))
                .isInstanceOf(IllegalStateException.class);
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

        ConfirmImportCommand command = adminCommand(UUID.randomUUID(), "hash", 0L,
                ImportBatchId.of(importId));

        ImportSummary result = service.confirm(command);

        assertThat(result.status()).isEqualTo("CONFIRMED");
        // Assignment port should NOT be called for ADMIN
        verify(staffAssignmentPort, never()).resolveActiveStaffId(any());
        verify(staffAssignmentPort, never()).isAssigned(any(), any());
    }

    // -------------------------------------------------------------------------
    // T6.2-12 — NFR-003: imported course row is mapped with INACTIVE initial status
    // -------------------------------------------------------------------------

    @Test
    void courses_row_mapped_with_inactive_initial_status() {
        // NFR-003: imported courses must land INACTIVE
        UUID importId = UUID.randomUUID();
        ImportBatch batch = validatedCoursesWithRows(importId, 1);
        when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
        when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));
        when(createCourseUseCase.createCourse(any())).thenReturn(minimalCourseView());
        when(saveImportBatchPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jsonCodecPort.toJson(any())).thenReturn("{}");

        ConfirmImportCommand command = adminCommand(UUID.randomUUID(), "hash", 0L,
                ImportBatchId.of(importId));
        service.confirm(command);

        ArgumentCaptor<co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseCommand>
                captor = ArgumentCaptor.forClass(
                        co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseCommand.class);
        verify(createCourseUseCase).createCourse(captor.capture());
        assertThat(captor.getValue().initialStatus()).isEqualTo("INACTIVE");
    }

    // -------------------------------------------------------------------------
    // T6.2-13 — OFFERINGS kind deferred to WU7
    // -------------------------------------------------------------------------

    @Test
    void offerings_kind_deferred_throws_UnsupportedOperationException() {
        UUID importId = UUID.randomUUID();
        ImportBatch batch = validatedOfferingsBatch(importId);
        when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
        when(loadImportBatchPort.findById(batch.getId())).thenReturn(Optional.of(batch));

        ConfirmImportCommand command = adminCommand(UUID.randomUUID(), "hash", 0L,
                ImportBatchId.of(importId));

        assertThatThrownBy(() -> service.confirm(command))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("WU7");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ConfirmImportCommand command(UUID key, String hash, long expectedVersion) {
        return command(key, hash, expectedVersion, ImportBatchId.generate());
    }

    private ConfirmImportCommand command(UUID key, String hash, long expectedVersion,
            ImportBatchId importId) {
        return new ConfirmImportCommand(
                importId.value(),
                ACTOR_EMAIL,
                "STAFF",
                ACTOR_ID,
                expectedVersion,
                key,
                hash);
    }

    private ConfirmImportCommand adminCommand(UUID key, String hash, long expectedVersion,
            ImportBatchId importId) {
        return new ConfirmImportCommand(
                importId.value(),
                "admin@test.com",
                "ADMIN",
                ACTOR_ID,
                expectedVersion,
                key,
                hash);
    }

    private ImportBatch validatedCoursesBatch(int validRows, long version) {
        ImportBatchId id = ImportBatchId.generate();
        return ImportBatch.reconstitute(
                id, ImportKind.COURSES, PROGRAM_ID, PLAN_ID, null,
                ImportStatus.VALIDATED,
                validRows, validRows,
                List.of(),
                List.of(new ImportRow(0, Map.of(
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
            rows.add(new ImportRow(i, Map.of(
                    "course_name", "Course " + i,
                    "credits", "3",
                    "weekly_hours", "4",
                    "type", "REQUIRED",
                    "recommended_semester", "1",
                    "can_be_enabled", "true")));
        }
        return ImportBatch.reconstitute(
                ImportBatchId.of(importId),
                ImportKind.COURSES, PROGRAM_ID, PLAN_ID, null,
                ImportStatus.VALIDATED,
                rowCount, rowCount,
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
            rows.add(new ImportRow(i, Map.of(
                    "destination_course", destCourse.toString(),
                    "requirement_type", "COURSE_PASSED",
                    "required_course_or_value", reqCourse.toString(),
                    "simultaneous_allowed", "false")));
        }
        return ImportBatch.reconstitute(
                ImportBatchId.of(importId),
                ImportKind.REQUIREMENTS, PROGRAM_ID, PLAN_ID, null,
                ImportStatus.VALIDATED,
                rowCount, rowCount,
                List.of(),
                rows,
                ImportBatch.CURRENT_SCHEMA_VERSION,
                0L,
                Instant.now());
    }

    private ImportBatch validatedOfferingsBatch(UUID importId) {
        List<ImportRow> rows = List.of(new ImportRow(0, Map.of(
                "period", UUID.randomUUID().toString(),
                "course", UUID.randomUUID().toString(),
                "option_label", "G1",
                "day", "MON",
                "start_time", "08:00",
                "end_time", "10:00")));
        return ImportBatch.reconstitute(
                ImportBatchId.of(importId),
                ImportKind.OFFERINGS, PROGRAM_ID, null,
                UUID.randomUUID(),
                ImportStatus.VALIDATED,
                1, 1,
                List.of(),
                rows,
                ImportBatch.CURRENT_SCHEMA_VERSION,
                0L,
                Instant.now());
    }

    private ImportBatch invalidBatch() {
        return ImportBatch.invalid(
                ImportBatchId.generate(),
                ImportKind.COURSES,
                PROGRAM_ID, PLAN_ID, null,
                0, 3,
                List.of(ImportIssue.error(0, null, "EMPTY_FILE", "No data rows found")),
                Instant.now());
    }

    private CourseView minimalCourseView() {
        return new CourseView(
                UUID.randomUUID(), PLAN_ID, PROGRAM_ID,
                null, "Test Course", "REQUIRED",
                3, 4, List.of(), "INACTIVE", 0L);
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
                id, "COURSES", PROGRAM_ID, PLAN_ID, null,
                "CONFIRMED", 3, 3, List.of(), List.of(),
                Instant.now(), 1L);
    }
}
