package co.edu.uniquindio.brujula.imports.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.imports.application.dto.ImportPageView;
import co.edu.uniquindio.brujula.imports.application.dto.ListImportsQuery;
import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.StaffAssignmentPort;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * DEBT-F7-003: unit tests for {@link ListImportsService}.
 *
 * <p>Covers: ADMIN path (no assignment filter), STAFF path with active staff + assigned programs,
 * STAFF inactive/unknown actor → empty page, STAFF with empty assigned programs → empty page,
 * kind/status filter parsing, in-memory pagination, toSummary issue classification.
 */
class ListImportsServiceTest {

  private LoadImportBatchPort loadImportBatchPort;
  private StaffAssignmentPort staffAssignmentPort;
  private ListImportsService service;

  private static final UUID PROGRAM_ID = UUID.fromString("00000001-0000-0000-0000-000000000001");
  private static final UUID PLAN_ID = UUID.fromString("00000001-0000-0000-0000-000000000002");
  private static final UUID PERIOD_ID = UUID.fromString("00000001-0000-0000-0000-000000000003");
  private static final UUID STAFF_ID = UUID.fromString("00000001-0000-0000-0000-000000000004");
  private static final String STAFF_EMAIL = "staff@test.com";

  @BeforeEach
  void setUp() {
    loadImportBatchPort = mock(LoadImportBatchPort.class);
    staffAssignmentPort = mock(StaffAssignmentPort.class);
    service = new ListImportsService(loadImportBatchPort, staffAssignmentPort);
  }

  // -------------------------------------------------------------------------
  // ADMIN path: sees all batches, does NOT call staffAssignmentPort
  // -------------------------------------------------------------------------

  @Test
  void admin_actor_calls_findAll_without_assignment_check() {
    ImportBatch batch = validatedBatch(ImportKind.COURSES);
    when(loadImportBatchPort.findAll(null, null)).thenReturn(List.of(batch));

    ListImportsQuery query = new ListImportsQuery(null, "ADMIN", null, null, 0, 10);
    ImportPageView result = service.list(query);

    assertThat(result.content()).hasSize(1);
    assertThat(result.totalElements()).isEqualTo(1);
    verify(staffAssignmentPort, never()).resolveActiveStaffId(null);
    verify(staffAssignmentPort, never()).getAssignedPrograms(null);
  }

  @Test
  void admin_actor_case_insensitive_role_match() {
    when(loadImportBatchPort.findAll(null, null)).thenReturn(List.of());

    ListImportsQuery query = new ListImportsQuery(null, "admin", null, null, 0, 10);
    ImportPageView result = service.list(query);

    assertThat(result.content()).isEmpty();
    verify(loadImportBatchPort).findAll(null, null);
    verify(staffAssignmentPort, never()).resolveActiveStaffId(null);
  }

  // -------------------------------------------------------------------------
  // STAFF path: inactive or unknown actor → empty page
  // -------------------------------------------------------------------------

  @Test
  void staff_inactive_or_unknown_actor_returns_empty_page() {
    when(staffAssignmentPort.resolveActiveStaffId(STAFF_EMAIL)).thenReturn(Optional.empty());

    ListImportsQuery query = new ListImportsQuery(STAFF_EMAIL, "STAFF", null, null, 0, 10);
    ImportPageView result = service.list(query);

    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isEqualTo(0);
    assertThat(result.totalPages()).isEqualTo(0);
    // Must not query any batches when actor is inactive
    verify(loadImportBatchPort, never()).findByProgramIds(null, null, null);
    verify(loadImportBatchPort, never()).findAll(null, null);
  }

  // -------------------------------------------------------------------------
  // STAFF path: active staff with no assigned programs → empty list
  // -------------------------------------------------------------------------

  @Test
  void staff_with_no_assigned_programs_returns_empty_page() {
    when(staffAssignmentPort.resolveActiveStaffId(STAFF_EMAIL)).thenReturn(Optional.of(STAFF_ID));
    when(staffAssignmentPort.getAssignedPrograms(STAFF_ID)).thenReturn(List.of());

    ListImportsQuery query = new ListImportsQuery(STAFF_EMAIL, "STAFF", null, null, 0, 10);
    ImportPageView result = service.list(query);

    assertThat(result.content()).isEmpty();
    // Must not call the DB when no programs are assigned
    verify(loadImportBatchPort, never()).findByProgramIds(null, null, null);
  }

  // -------------------------------------------------------------------------
  // STAFF path: active staff with assigned programs → calls findByProgramIds
  // -------------------------------------------------------------------------

  @Test
  void staff_with_assigned_programs_loads_batches_via_single_query() {
    List<UUID> programs = List.of(PROGRAM_ID);
    ImportBatch batch = validatedBatch(ImportKind.COURSES);
    when(staffAssignmentPort.resolveActiveStaffId(STAFF_EMAIL)).thenReturn(Optional.of(STAFF_ID));
    when(staffAssignmentPort.getAssignedPrograms(STAFF_ID)).thenReturn(programs);
    when(loadImportBatchPort.findByProgramIds(programs, null, null)).thenReturn(List.of(batch));

    ListImportsQuery query = new ListImportsQuery(STAFF_EMAIL, "STAFF", null, null, 0, 10);
    ImportPageView result = service.list(query);

    assertThat(result.content()).hasSize(1);
    assertThat(result.totalElements()).isEqualTo(1);
    verify(loadImportBatchPort).findByProgramIds(programs, null, null);
  }

  // -------------------------------------------------------------------------
  // Kind/status filter parsing
  // -------------------------------------------------------------------------

  @Test
  void kind_filter_is_parsed_from_string_and_forwarded() {
    when(loadImportBatchPort.findAll(ImportKind.COURSES, null)).thenReturn(List.of());

    ListImportsQuery query = new ListImportsQuery(null, "ADMIN", "COURSES", null, 0, 10);
    service.list(query);

    verify(loadImportBatchPort).findAll(ImportKind.COURSES, null);
  }

  @Test
  void status_filter_is_parsed_from_string_and_forwarded() {
    when(loadImportBatchPort.findAll(null, ImportStatus.VALIDATED)).thenReturn(List.of());

    ListImportsQuery query = new ListImportsQuery(null, "ADMIN", null, "VALIDATED", 0, 10);
    service.list(query);

    verify(loadImportBatchPort).findAll(null, ImportStatus.VALIDATED);
  }

  @Test
  void null_kind_filter_passes_null_to_port() {
    when(loadImportBatchPort.findAll(null, null)).thenReturn(List.of());

    ListImportsQuery query = new ListImportsQuery(null, "ADMIN", null, null, 0, 10);
    service.list(query);

    verify(loadImportBatchPort).findAll(null, null);
  }

  // -------------------------------------------------------------------------
  // In-memory pagination
  // -------------------------------------------------------------------------

  @Test
  void pagination_correctly_slices_results() {
    List<ImportBatch> batches =
        List.of(
            validatedBatch(ImportKind.COURSES),
            validatedBatch(ImportKind.COURSES),
            validatedBatch(ImportKind.REQUIREMENTS));
    when(loadImportBatchPort.findAll(null, null)).thenReturn(batches);

    // Request page 1 (0-based), size 2 → 2nd slice = 1 element
    ListImportsQuery query = new ListImportsQuery(null, "ADMIN", null, null, 1, 2);
    ImportPageView result = service.list(query);

    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.totalElements()).isEqualTo(3);
    assertThat(result.totalPages()).isEqualTo(2);
    assertThat(result.content()).hasSize(1);
  }

  @Test
  void pagination_first_page_of_exact_size_returns_full_page() {
    List<ImportBatch> batches =
        List.of(validatedBatch(ImportKind.COURSES), validatedBatch(ImportKind.REQUIREMENTS));
    when(loadImportBatchPort.findAll(null, null)).thenReturn(batches);

    ListImportsQuery query = new ListImportsQuery(null, "ADMIN", null, null, 0, 2);
    ImportPageView result = service.list(query);

    assertThat(result.content()).hasSize(2);
    assertThat(result.totalPages()).isEqualTo(1);
  }

  @Test
  void empty_batch_list_returns_empty_page_with_zero_totals() {
    when(loadImportBatchPort.findAll(null, null)).thenReturn(List.of());

    ListImportsQuery query = new ListImportsQuery(null, "ADMIN", null, null, 0, 10);
    ImportPageView result = service.list(query);

    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isEqualTo(0);
    assertThat(result.totalPages()).isEqualTo(0);
  }

  // -------------------------------------------------------------------------
  // toSummary: issue classification (ERROR vs WARNING)
  // -------------------------------------------------------------------------

  @Test
  void summary_correctly_classifies_error_and_warning_issues() {
    ImportIssue errorIssue = ImportIssue.error(1, "course_name", "COURSE_NAME_MISSING", "missing");
    ImportIssue warningIssue = ImportIssue.warning(2, "credits", "LOW_CREDITS", "low");

    ImportBatch batchWithIssues =
        ImportBatch.invalid(
            ImportBatchId.generate(),
            ImportKind.COURSES,
            PROGRAM_ID,
            PLAN_ID,
            PERIOD_ID,
            0,
            2,
            List.of(errorIssue, warningIssue),
            Instant.now());

    when(loadImportBatchPort.findAll(null, null)).thenReturn(List.of(batchWithIssues));

    ListImportsQuery query = new ListImportsQuery(null, "ADMIN", null, null, 0, 10);
    ImportPageView result = service.list(query);

    assertThat(result.content()).hasSize(1);
    var summary = result.content().get(0);
    // ERROR issues go to errors list
    assertThat(summary.errors()).hasSize(1);
    assertThat(summary.errors().get(0).code()).isEqualTo("COURSE_NAME_MISSING");
    assertThat(summary.errors().get(0).severity()).isEqualTo("ERROR");
    // WARNING issues go to warnings list
    assertThat(summary.warnings()).hasSize(1);
    assertThat(summary.warnings().get(0).code()).isEqualTo("LOW_CREDITS");
    assertThat(summary.warnings().get(0).severity()).isEqualTo("WARNING");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static ImportBatch validatedBatch(ImportKind kind) {
    return ImportBatch.validated(
        ImportBatchId.generate(),
        kind,
        PROGRAM_ID,
        PLAN_ID,
        PERIOD_ID,
        3,
        3,
        List.of(),
        List.of(new ImportRow(0, Map.of("course_name", "Cálculo I"))),
        Instant.now());
  }
}
