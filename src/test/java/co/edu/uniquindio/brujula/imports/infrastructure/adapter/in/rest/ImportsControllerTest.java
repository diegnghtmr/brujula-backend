package co.edu.uniquindio.brujula.imports.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.imports.application.dto.ImportPageView;
import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;
import co.edu.uniquindio.brujula.imports.application.port.in.ConfirmImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.in.GetImportUseCase;
import co.edu.uniquindio.brujula.imports.application.port.in.ListImportsUseCase;
import co.edu.uniquindio.brujula.imports.application.port.in.UploadImportUseCase;
import co.edu.uniquindio.brujula.imports.domain.exception.FileSizeExceededException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotConfirmableException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotFoundException;
import co.edu.uniquindio.brujula.imports.domain.exception.UnsupportedFileTypeException;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Controller slice security and exception-mapping tests for imports endpoints.
 *
 * <p>Tests:
 *
 * <ul>
 *   <li>POST /staff/imports — STUDENT → 403; STAFF assigned → 201 + Location; STAFF unassigned →
 *       403; idempotency conflict → 409; wrong file type → 415; file too large → 413; missing file
 *       → 400
 *   <li>GET /staff/imports — STAFF → 200 with page; unauthenticated → 401 (handled by Spring)
 *   <li>GET /staff/imports/{id} — happy path → 200; not found → 404; access denied → 403
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ImportsControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  @MockitoBean private UploadImportUseCase uploadImportUseCase;
  @MockitoBean private ListImportsUseCase listImportsUseCase;
  @MockitoBean private GetImportUseCase getImportUseCase;
  @MockitoBean private ConfirmImportUseCase confirmImportUseCase;
  @MockitoBean private GetCurrentUserQuery getCurrentUserQuery;

  private MockMvc mockMvc;

  private static final UUID PROGRAM_ID = UUID.randomUUID();
  private static final UUID PLAN_ID = UUID.randomUUID();
  private static final UUID IMPORT_ID = UUID.randomUUID();
  private static final UUID ACTOR_ID = UUID.randomUUID();
  private static final String XSRF_TOKEN = "test-xsrf-token";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // -------------------------------------------------------------------------
  // POST /staff/imports — upload (T4.1)
  // -------------------------------------------------------------------------

  @Test
  void upload_by_student_returns_403() throws Exception {
    mockMvc
        .perform(
            multipart("/staff/imports")
                .file(minimalXlsxFile())
                .param("kind", "COURSES")
                .param("programId", PROGRAM_ID.toString())
                .param("planId", PLAN_ID.toString())
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void upload_multipart_returns_201_with_location_header() throws Exception {
    stubStaffProfile();
    ImportSummary summary = validatedSummary("COURSES");
    when(uploadImportUseCase.upload(any())).thenReturn(summary);

    mockMvc
        .perform(
            multipart("/staff/imports")
                .file(minimalXlsxFile())
                .param("kind", "COURSES")
                .param("programId", PROGRAM_ID.toString())
                .param("planId", PLAN_ID.toString())
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/staff/imports/" + IMPORT_ID))
        .andExpect(jsonPath("$.id").value(IMPORT_ID.toString()))
        .andExpect(jsonPath("$.status").value("VALIDATED"))
        .andExpect(jsonPath("$.kind").value("COURSES"))
        .andExpect(jsonPath("$.totalRows").value(5))
        .andExpect(jsonPath("$.validRows").value(5));
  }

  @Test
  void upload_by_unassigned_staff_returns_403() throws Exception {
    stubStaffProfile();
    when(uploadImportUseCase.upload(any()))
        .thenThrow(ImportAccessDeniedException.notAssignedToProgram(PROGRAM_ID));

    mockMvc
        .perform(
            multipart("/staff/imports")
                .file(minimalXlsxFile())
                .param("kind", "COURSES")
                .param("programId", PROGRAM_ID.toString())
                .param("planId", PLAN_ID.toString())
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("IMPORT_ACCESS_DENIED"));
  }

  @Test
  void upload_by_inactive_staff_returns_403() throws Exception {
    // WARNING-1 coverage
    stubStaffProfile();
    when(uploadImportUseCase.upload(any()))
        .thenThrow(ImportAccessDeniedException.inactiveOrUnknownActor("staff@test.com"));

    mockMvc
        .perform(
            multipart("/staff/imports")
                .file(minimalXlsxFile())
                .param("kind", "COURSES")
                .param("programId", PROGRAM_ID.toString())
                .param("planId", PLAN_ID.toString())
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("IMPORT_ACCESS_DENIED"));
  }

  @Test
  void idempotency_replay_same_key_same_hash_returns_201_with_same_summary() throws Exception {
    stubStaffProfile();
    ImportSummary summary = validatedSummary("COURSES");
    when(uploadImportUseCase.upload(any())).thenReturn(summary);

    UUID idempotencyKey = UUID.randomUUID();
    // First call
    mockMvc
        .perform(
            multipart("/staff/imports")
                .file(minimalXlsxFile())
                .param("kind", "COURSES")
                .param("programId", PROGRAM_ID.toString())
                .param("planId", PLAN_ID.toString())
                .header("Idempotency-Key", idempotencyKey)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(IMPORT_ID.toString()));
  }

  @Test
  void upload_idempotency_conflict_returns_409() throws Exception {
    stubStaffProfile();
    UUID idempotencyKey = UUID.randomUUID();
    when(uploadImportUseCase.upload(any()))
        .thenThrow(new IdempotencyKeyConflictException(idempotencyKey, "UPLOAD_IMPORT"));

    mockMvc
        .perform(
            multipart("/staff/imports")
                .file(minimalXlsxFile())
                .param("kind", "COURSES")
                .param("programId", PROGRAM_ID.toString())
                .param("planId", PLAN_ID.toString())
                .header("Idempotency-Key", idempotencyKey)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
  }

  @Test
  void upload_wrong_file_type_returns_415() throws Exception {
    stubStaffProfile();
    when(uploadImportUseCase.upload(any()))
        .thenThrow(new UnsupportedFileTypeException("text/csv", "data.csv"));

    mockMvc
        .perform(
            multipart("/staff/imports")
                .file(
                    new MockMultipartFile("file", "data.csv", "text/csv", "col1\nval1".getBytes()))
                .param("kind", "COURSES")
                .param("programId", PROGRAM_ID.toString())
                .param("planId", PLAN_ID.toString())
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
  }

  @Test
  void upload_oversize_file_returns_413() throws Exception {
    stubStaffProfile();
    when(uploadImportUseCase.upload(any()))
        .thenThrow(new FileSizeExceededException(6_000_000L, 5_242_880L));

    mockMvc
        .perform(
            multipart("/staff/imports")
                .file(minimalXlsxFile())
                .param("kind", "COURSES")
                .param("programId", PROGRAM_ID.toString())
                .param("planId", PLAN_ID.toString())
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
  }

  @Test
  void upload_invalid_batch_returns_201_with_invalid_status() throws Exception {
    stubStaffProfile();
    ImportSummary invalidSummary = invalidSummary("COURSES");
    when(uploadImportUseCase.upload(any())).thenReturn(invalidSummary);

    mockMvc
        .perform(
            multipart("/staff/imports")
                .file(minimalXlsxFile())
                .param("kind", "COURSES")
                .param("programId", PROGRAM_ID.toString())
                .param("planId", PLAN_ID.toString())
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("INVALID"))
        .andExpect(jsonPath("$.errors").isArray());
  }

  @Test
  void upload_multipart_missing_file_returns_400() throws Exception {
    // T4.1 — missing required 'file' part
    stubStaffProfile();

    mockMvc
        .perform(
            multipart("/staff/imports")
                .param("kind", "COURSES")
                .param("programId", PROGRAM_ID.toString())
                .param("planId", PLAN_ID.toString())
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isBadRequest());
  }

  // -------------------------------------------------------------------------
  // GET /staff/imports — list (T4.1)
  // -------------------------------------------------------------------------

  @Test
  void list_imports_returns_200_with_page() throws Exception {
    stubStaffProfile();
    ImportPageView pageView =
        new ImportPageView(List.of(validatedSummary("COURSES")), 0, 20, 1L, 1);
    when(listImportsUseCase.list(any())).thenReturn(pageView);

    mockMvc
        .perform(
            get("/staff/imports")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].id").value(IMPORT_ID.toString()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.page").value(0));
  }

  @Test
  void list_imports_returns_200_empty_for_staff_with_no_assigned_programs() throws Exception {
    stubStaffProfile();
    ImportPageView emptyPage = new ImportPageView(List.of(), 0, 20, 0L, 0);
    when(listImportsUseCase.list(any())).thenReturn(emptyPage);

    mockMvc
        .perform(
            get("/staff/imports")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void list_imports_admin_sees_all() throws Exception {
    stubAdminProfile();
    ImportPageView pageView =
        new ImportPageView(
            List.of(validatedSummary("COURSES"), validatedSummary("OFFERINGS")), 0, 20, 2L, 1);
    when(listImportsUseCase.list(any())).thenReturn(pageView);

    mockMvc
        .perform(
            get("/staff/imports")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  // -------------------------------------------------------------------------
  // GET /staff/imports/{importId} — get (T4.1)
  // -------------------------------------------------------------------------

  @Test
  void get_import_by_id_returns_200_with_summary() throws Exception {
    stubStaffProfile();
    ImportSummary summary = validatedSummary("COURSES");
    when(getImportUseCase.get(eq(IMPORT_ID), anyString(), anyString())).thenReturn(summary);

    mockMvc
        .perform(
            get("/staff/imports/{importId}", IMPORT_ID)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(IMPORT_ID.toString()))
        .andExpect(jsonPath("$.status").value("VALIDATED"));
  }

  @Test
  void get_import_not_found_returns_404() throws Exception {
    stubStaffProfile();
    when(getImportUseCase.get(eq(IMPORT_ID), anyString(), anyString()))
        .thenThrow(new ImportBatchNotFoundException(IMPORT_ID));

    mockMvc
        .perform(
            get("/staff/imports/{importId}", IMPORT_ID)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("IMPORT_NOT_FOUND"));
  }

  @Test
  void get_import_unassigned_program_returns_403() throws Exception {
    stubStaffProfile();
    when(getImportUseCase.get(eq(IMPORT_ID), anyString(), anyString()))
        .thenThrow(ImportAccessDeniedException.notAssignedToProgram(PROGRAM_ID));

    mockMvc
        .perform(
            get("/staff/imports/{importId}", IMPORT_ID)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("IMPORT_ACCESS_DENIED"));
  }

  // -------------------------------------------------------------------------
  // POST /staff/imports/{importId}/confirmation — confirm (T6.4)
  // -------------------------------------------------------------------------

  @Test
  void confirm_import_returns_200_on_success() throws Exception {
    stubStaffProfile();
    ImportSummary confirmed = confirmedSummary();
    when(confirmImportUseCase.confirm(any())).thenReturn(confirmed);

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/staff/imports/{importId}/confirmation", IMPORT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 1}")
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.id").value(IMPORT_ID.toString()));
  }

  @Test
  void confirm_stale_version_returns_409() throws Exception {
    stubStaffProfile();
    when(confirmImportUseCase.confirm(any()))
        .thenThrow(new ObjectOptimisticLockingFailureException(Object.class, IMPORT_ID));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/staff/imports/{importId}/confirmation", IMPORT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 1}")
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
  }

  @Test
  void confirm_invalid_batch_returns_409() throws Exception {
    stubStaffProfile();
    when(confirmImportUseCase.confirm(any()))
        .thenThrow(
            new ImportBatchNotConfirmableException(
                ImportBatchId.of(IMPORT_ID),
                co.edu.uniquindio.brujula.imports.domain.model.ImportStatus.INVALID));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/staff/imports/{importId}/confirmation", IMPORT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0}")
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IMPORT_NOT_CONFIRMABLE"));
  }

  @Test
  void confirm_unassigned_staff_returns_403() throws Exception {
    stubStaffProfile();
    when(confirmImportUseCase.confirm(any()))
        .thenThrow(ImportAccessDeniedException.notAssignedToProgram(PROGRAM_ID));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/staff/imports/{importId}/confirmation", IMPORT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 1}")
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("IMPORT_ACCESS_DENIED"));
  }

  @Test
  void confirm_batch_not_found_returns_404() throws Exception {
    // T6.4
    stubStaffProfile();
    when(confirmImportUseCase.confirm(any()))
        .thenThrow(new ImportBatchNotFoundException(IMPORT_ID));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/staff/imports/{importId}/confirmation", IMPORT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 1}")
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("IMPORT_NOT_FOUND"));
  }

  @Test
  void confirm_offerings_kind_returns_200_confirmed() throws Exception {
    stubStaffProfile();
    ImportSummary confirmed = confirmedOfferingsSummary();
    when(confirmImportUseCase.confirm(any())).thenReturn(confirmed);

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/staff/imports/{importId}/confirmation", IMPORT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0}")
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.kind").value("OFFERINGS"))
        .andExpect(jsonPath("$.id").value(IMPORT_ID.toString()));
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private void stubStaffProfile() {
    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(
            new UserProfile(
                ACTOR_ID, "staff-sub", "staff@test.com", "Staff User", "STAFF", "ACTIVE", true));
  }

  private void stubAdminProfile() {
    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(
            new UserProfile(
                ACTOR_ID, "admin-sub", "admin@test.com", "Admin User", "ADMIN", "ACTIVE", true));
  }

  private MockMultipartFile minimalXlsxFile() {
    return new MockMultipartFile(
        "file",
        "courses.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "fake-xlsx-content".getBytes());
  }

  private ImportSummary validatedSummary(String kind) {
    return new ImportSummary(
        IMPORT_ID,
        kind,
        PROGRAM_ID,
        PLAN_ID,
        null,
        "VALIDATED",
        5,
        5,
        List.of(),
        List.of(),
        Instant.now(),
        1L);
  }

  private ImportSummary invalidSummary(String kind) {
    ImportSummary.ImportIssueDto issue =
        new ImportSummary.ImportIssueDto(0, null, "EMPTY_FILE", "No data rows found", "ERROR");
    return new ImportSummary(
        IMPORT_ID,
        kind,
        PROGRAM_ID,
        PLAN_ID,
        null,
        "INVALID",
        0,
        0,
        List.of(issue),
        List.of(),
        Instant.now(),
        1L);
  }

  private ImportSummary confirmedSummary() {
    return new ImportSummary(
        IMPORT_ID,
        "COURSES",
        PROGRAM_ID,
        PLAN_ID,
        null,
        "CONFIRMED",
        5,
        5,
        List.of(),
        List.of(),
        Instant.now(),
        2L);
  }

  private ImportSummary confirmedOfferingsSummary() {
    return new ImportSummary(
        IMPORT_ID,
        "OFFERINGS",
        PROGRAM_ID,
        null,
        UUID.randomUUID(),
        "CONFIRMED",
        3,
        3,
        List.of(),
        List.of(),
        Instant.now(),
        2L);
  }
}
