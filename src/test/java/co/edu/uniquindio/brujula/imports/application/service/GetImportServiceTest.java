package co.edu.uniquindio.brujula.imports.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;
import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.StaffAssignmentPort;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotFoundException;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;
import co.edu.uniquindio.brujula.imports.domain.model.Severity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GetImportService}.
 *
 * <p>Covers: batch not found → ImportBatchNotFoundException; ADMIN bypasses assignment check; STAFF
 * inactive → ImportAccessDeniedException; STAFF not assigned → ImportAccessDeniedException; STAFF
 * assigned → returns ImportSummary; toSummary error/warning classification.
 */
class GetImportServiceTest {

  private LoadImportBatchPort loadImportBatchPort;
  private StaffAssignmentPort staffAssignmentPort;
  private GetImportService service;

  private static final UUID PROGRAM_ID = UUID.fromString("00000001-0000-0000-0000-000000000001");
  private static final UUID PLAN_ID = UUID.fromString("00000001-0000-0000-0000-000000000002");
  private static final UUID PERIOD_ID = UUID.fromString("00000001-0000-0000-0000-000000000003");
  private static final UUID STAFF_ID = UUID.fromString("00000001-0000-0000-0000-000000000004");
  private static final String STAFF_EMAIL = "staff@test.com";

  @BeforeEach
  void setUp() {
    loadImportBatchPort = mock(LoadImportBatchPort.class);
    staffAssignmentPort = mock(StaffAssignmentPort.class);
    service = new GetImportService(loadImportBatchPort, staffAssignmentPort);
  }

  // -------------------------------------------------------------------------
  // Not found
  // -------------------------------------------------------------------------

  @Test
  void returns_not_found_exception_when_batch_absent() {
    UUID missingId = UUID.randomUUID();
    when(loadImportBatchPort.findById(new ImportBatchId(missingId))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(missingId, STAFF_EMAIL, "ADMIN"))
        .isInstanceOf(ImportBatchNotFoundException.class)
        .hasMessageContaining(missingId.toString());
  }

  // -------------------------------------------------------------------------
  // ADMIN path: bypasses assignment check
  // -------------------------------------------------------------------------

  @Test
  void admin_actor_bypasses_assignment_check_and_returns_summary() {
    UUID batchId = UUID.randomUUID();
    ImportBatch batch = validatedBatch(batchId, ImportKind.COURSES);
    when(loadImportBatchPort.findById(new ImportBatchId(batchId))).thenReturn(Optional.of(batch));

    ImportSummary result = service.get(batchId, null, "ADMIN");

    assertThat(result.id()).isEqualTo(batchId);
    assertThat(result.kind()).isEqualTo("COURSES");
    assertThat(result.status()).isEqualTo("VALIDATED");
    // Admin must NOT trigger assignment checks
    verify(staffAssignmentPort, never()).resolveActiveStaffId(null);
    verify(staffAssignmentPort, never()).isAssigned(null, null);
  }

  @Test
  void admin_actor_case_insensitive_role_bypasses_assignment_check() {
    UUID batchId = UUID.randomUUID();
    ImportBatch batch = validatedBatch(batchId, ImportKind.REQUIREMENTS);
    when(loadImportBatchPort.findById(new ImportBatchId(batchId))).thenReturn(Optional.of(batch));

    ImportSummary result = service.get(batchId, null, "admin");

    assertThat(result.kind()).isEqualTo("REQUIREMENTS");
    verify(staffAssignmentPort, never()).resolveActiveStaffId(null);
  }

  // -------------------------------------------------------------------------
  // STAFF path: inactive or unknown actor → 403
  // -------------------------------------------------------------------------

  @Test
  void staff_inactive_actor_throws_access_denied() {
    UUID batchId = UUID.randomUUID();
    ImportBatch batch = validatedBatch(batchId, ImportKind.COURSES);
    when(loadImportBatchPort.findById(new ImportBatchId(batchId))).thenReturn(Optional.of(batch));
    when(staffAssignmentPort.resolveActiveStaffId(STAFF_EMAIL)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(batchId, STAFF_EMAIL, "STAFF"))
        .isInstanceOf(ImportAccessDeniedException.class)
        .hasMessageContaining(STAFF_EMAIL);
  }

  // -------------------------------------------------------------------------
  // STAFF path: active staff but not assigned → 403
  // -------------------------------------------------------------------------

  @Test
  void staff_not_assigned_to_program_throws_access_denied() {
    UUID batchId = UUID.randomUUID();
    ImportBatch batch = validatedBatch(batchId, ImportKind.COURSES);
    when(loadImportBatchPort.findById(new ImportBatchId(batchId))).thenReturn(Optional.of(batch));
    when(staffAssignmentPort.resolveActiveStaffId(STAFF_EMAIL)).thenReturn(Optional.of(STAFF_ID));
    when(staffAssignmentPort.isAssigned(STAFF_ID, PROGRAM_ID)).thenReturn(false);

    assertThatThrownBy(() -> service.get(batchId, STAFF_EMAIL, "STAFF"))
        .isInstanceOf(ImportAccessDeniedException.class)
        .hasMessageContaining(PROGRAM_ID.toString());
  }

  // -------------------------------------------------------------------------
  // STAFF path: active staff assigned → returns summary
  // -------------------------------------------------------------------------

  @Test
  void staff_assigned_to_program_returns_summary() {
    UUID batchId = UUID.randomUUID();
    ImportBatch batch = validatedBatch(batchId, ImportKind.COURSES);
    when(loadImportBatchPort.findById(new ImportBatchId(batchId))).thenReturn(Optional.of(batch));
    when(staffAssignmentPort.resolveActiveStaffId(STAFF_EMAIL)).thenReturn(Optional.of(STAFF_ID));
    when(staffAssignmentPort.isAssigned(STAFF_ID, PROGRAM_ID)).thenReturn(true);

    ImportSummary result = service.get(batchId, STAFF_EMAIL, "STAFF");

    assertThat(result.id()).isEqualTo(batchId);
    assertThat(result.programId()).isEqualTo(PROGRAM_ID);
    assertThat(result.status()).isEqualTo("VALIDATED");
  }

  // -------------------------------------------------------------------------
  // toSummary: issue classification (ERROR vs WARNING)
  // -------------------------------------------------------------------------

  @Test
  void summary_classifies_error_and_warning_issues_separately() {
    UUID batchId = UUID.randomUUID();
    ImportIssue errorIssue = ImportIssue.error(1, "course_name", "COURSE_NAME_MISSING", "missing");
    ImportIssue warningIssue = ImportIssue.warning(2, "credits", "LOW_CREDITS", "low");

    ImportBatch batchWithIssues =
        ImportBatch.invalid(
            ImportBatchId.of(batchId),
            ImportKind.COURSES,
            PROGRAM_ID,
            PLAN_ID,
            PERIOD_ID,
            0,
            2,
            List.of(errorIssue, warningIssue),
            Instant.now());

    when(loadImportBatchPort.findById(new ImportBatchId(batchId)))
        .thenReturn(Optional.of(batchWithIssues));

    ImportSummary result = service.get(batchId, null, "ADMIN");

    assertThat(result.errors()).hasSize(1);
    assertThat(result.errors().get(0).code()).isEqualTo("COURSE_NAME_MISSING");
    assertThat(result.errors().get(0).severity()).isEqualTo(Severity.ERROR.name());
    assertThat(result.warnings()).hasSize(1);
    assertThat(result.warnings().get(0).code()).isEqualTo("LOW_CREDITS");
    assertThat(result.warnings().get(0).severity()).isEqualTo(Severity.WARNING.name());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static ImportBatch validatedBatch(UUID batchId, ImportKind kind) {
    return ImportBatch.reconstitute(
        ImportBatchId.of(batchId),
        kind,
        PROGRAM_ID,
        PLAN_ID,
        PERIOD_ID,
        ImportStatus.VALIDATED,
        3,
        3,
        List.of(),
        List.of(),
        ImportBatch.CURRENT_SCHEMA_VERSION,
        0L,
        Instant.now());
  }
}
