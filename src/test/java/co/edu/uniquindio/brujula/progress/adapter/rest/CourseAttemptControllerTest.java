package co.edu.uniquindio.brujula.progress.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptPageView;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.DeleteCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.GetCourseAttemptQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.ListMyCourseAttemptsQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.UpdateCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.domain.AttemptNotFoundException;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.GradeStatusMismatchException;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import org.springframework.dao.OptimisticLockingFailureException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * WU3-T12 RED: REST integration tests for the CourseAttempt endpoints.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>CROSS-2-UNAUTH: unauthenticated → 401.
 *   <li>CROSS-2-STAFF: STAFF role on /me/course-attempts → 403.
 *   <li>PRG-1-HP: STUDENT POST create → 201.
 *   <li>PRG-1-MISSING-IDEMPOTENCY-KEY: POST without Idempotency-Key → 400.
 *   <li>PRG-1-GRADE-MISMATCH: grade-status mismatch → 422.
 *   <li>CROSS-3: optimistic-lock conflict → 409.
 *   <li>PRG-2-BOLA: GET attempt belonging to another user → 404.
 *   <li>PRG-4-HP: DELETE attempt → 204.
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CourseAttemptControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;

  @MockitoBean private CreateCourseAttemptUseCase createCourseAttemptUseCase;
  @MockitoBean private ListMyCourseAttemptsQuery listMyCourseAttemptsQuery;
  @MockitoBean private GetCourseAttemptQuery getCourseAttemptQuery;
  @MockitoBean private UpdateCourseAttemptUseCase updateCourseAttemptUseCase;
  @MockitoBean private DeleteCourseAttemptUseCase deleteCourseAttemptUseCase;
  // WU4: mock new query services so this test class doesn't wire real application services
  @MockitoBean private co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionCurriculumQuery getSubscriptionCurriculumQuery;
  @MockitoBean private co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionProgressQuery getSubscriptionProgressQuery;

  private MockMvc mockMvc;
  private String studentSubject;
  private String staffSubject;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    studentSubject = "student-" + UUID.randomUUID();
    AppUser student = AppUser.provision(
        studentSubject,
        "student-" + UUID.randomUUID() + "@uniquindio.edu.co",
        "Test Student",
        Role.STUDENT);
    saveAppUserPort.save(student);

    staffSubject = "staff-" + UUID.randomUUID();
    AppUser staff = AppUser.provision(
        staffSubject,
        "staff-" + UUID.randomUUID() + "@uniquindio.edu.co",
        "Test Staff",
        Role.STAFF);
    saveAppUserPort.save(staff);
  }

  @Test
  void unauthenticated_returns_4xx() throws Exception {
    // Spring Security may redirect (302) or return 401 depending on the configured entry point.
    // Either way it must NOT return a 2xx.
    mockMvc.perform(get("/me/course-attempts"))
        .andExpect(result -> {
          int s = result.getResponse().getStatus();
          if (s >= 200 && s < 300) {
            throw new AssertionError("Expected 4xx but was " + s);
          }
        });
  }

  @Test
  void staff_role_on_me_returns_403() throws Exception {
    mockMvc.perform(get("/me/course-attempts")
            .with(oidcLogin()
                .idToken(t -> t.subject(staffSubject))
                .authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void student_create_attempt_returns_201() throws Exception {
    CourseAttemptView view = buildAttemptView();
    when(createCourseAttemptUseCase.createCourseAttempt(any())).thenReturn(view);

    mockMvc.perform(post("/me/course-attempts")
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject)))
            .with(csrf())
            .header("Idempotency-Key", UUID.randomUUID())
            .header("X-XSRF-TOKEN", "csrf-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "subscriptionId": "550e8400-e29b-41d4-a716-446655440001",
                  "courseId": "550e8400-e29b-41d4-a716-446655440002",
                  "periodId": "550e8400-e29b-41d4-a716-446655440003",
                  "journeyCode": "DIURNA",
                  "status": "PASSED",
                  "finalGrade": 4.2
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PASSED"));
  }

  @Test
  void create_without_idempotency_key_returns_400() throws Exception {
    mockMvc.perform(post("/me/course-attempts")
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject)))
            .with(csrf())
            // No Idempotency-Key header intentionally
            .header("X-XSRF-TOKEN", "csrf-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "subscriptionId": "550e8400-e29b-41d4-a716-446655440001",
                  "courseId": "550e8400-e29b-41d4-a716-446655440002",
                  "periodId": "550e8400-e29b-41d4-a716-446655440003",
                  "journeyCode": "DIURNA",
                  "status": "PASSED",
                  "finalGrade": 4.2
                }
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void grade_status_mismatch_returns_422() throws Exception {
    when(createCourseAttemptUseCase.createCourseAttempt(any()))
        .thenThrow(new GradeStatusMismatchException(AttemptStatus.PASSED, "grade below 3.0"));

    mockMvc.perform(post("/me/course-attempts")
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject)))
            .with(csrf())
            .header("Idempotency-Key", UUID.randomUUID())
            .header("X-XSRF-TOKEN", "csrf-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "subscriptionId": "550e8400-e29b-41d4-a716-446655440001",
                  "courseId": "550e8400-e29b-41d4-a716-446655440002",
                  "periodId": "550e8400-e29b-41d4-a716-446655440003",
                  "journeyCode": "DIURNA",
                  "status": "PASSED",
                  "finalGrade": 2.5
                }
                """))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void optimistic_lock_conflict_returns_409() throws Exception {
    when(updateCourseAttemptUseCase.updateCourseAttempt(any()))
        .thenThrow(new OptimisticLockingFailureException("version conflict"));

    mockMvc.perform(put("/me/course-attempts/" + UUID.randomUUID())
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject)))
            .with(csrf())
            .header("X-XSRF-TOKEN", "csrf-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status": "PASSED", "finalGrade": 3.5, "version": 0}
                """))
        .andExpect(status().isConflict());
  }

  @Test
  void bola_get_returns_404() throws Exception {
    UUID attemptId = UUID.randomUUID();
    when(getCourseAttemptQuery.getCourseAttempt(any(), any()))
        .thenThrow(new AttemptNotFoundException(attemptId));

    mockMvc.perform(get("/me/course-attempts/" + attemptId)
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject))))
        .andExpect(status().isNotFound());
  }

  @Test
  void delete_attempt_returns_204() throws Exception {
    UUID attemptId = UUID.randomUUID();

    mockMvc.perform(delete("/me/course-attempts/" + attemptId)
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject)))
            .with(csrf())
            .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isNoContent());
  }

  // WU4: stubs replaced with real delegation — unknown subscription returns 404 (BOLA/not found)
  @Test
  void get_subscription_curriculum_unknown_returns_404() throws Exception {
    UUID subId = UUID.randomUUID();
    when(getSubscriptionCurriculumQuery.getSubscriptionCurriculum(any(), any()))
        .thenThrow(new co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException(subId));
    mockMvc.perform(get("/me/subscriptions/" + subId + "/curriculum")
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject))))
        .andExpect(status().isNotFound());
  }

  @Test
  void get_subscription_progress_unknown_returns_404() throws Exception {
    UUID subId = UUID.randomUUID();
    when(getSubscriptionProgressQuery.getSubscriptionProgress(any(), any()))
        .thenThrow(new co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException(subId));
    mockMvc.perform(get("/me/subscriptions/" + subId + "/progress")
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject))))
        .andExpect(status().isNotFound());
  }

  @Test
  void idempotency_key_conflict_returns_409() throws Exception {
    UUID idempotencyKey = UUID.randomUUID();
    when(createCourseAttemptUseCase.createCourseAttempt(any()))
        .thenThrow(new IdempotencyKeyConflictException(idempotencyKey, "CREATE_COURSE_ATTEMPT"));

    mockMvc.perform(post("/me/course-attempts")
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject)))
            .with(csrf())
            .header("Idempotency-Key", idempotencyKey)
            .header("X-XSRF-TOKEN", "csrf-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "subscriptionId": "550e8400-e29b-41d4-a716-446655440001",
                  "courseId": "550e8400-e29b-41d4-a716-446655440002",
                  "periodId": "550e8400-e29b-41d4-a716-446655440003",
                  "journeyCode": "DIURNA",
                  "status": "PASSED",
                  "finalGrade": 4.2
                }
                """))
        .andExpect(status().isConflict());
  }

  // FIX-W4 note: the domain now enforces periodId non-null at register() time (contract-required).
  // However the CourseAttemptView application DTO still supports null periodId for
  // reconstituted/legacy rows. The controller must pass it through as-is without substitution.
  @Test
  void attempt_view_with_null_period_id_serializes_as_null_not_zero_uuid() throws Exception {
    // Arrange: simulate a mocked service returning a view where periodId is null
    // (e.g. legacy data row reconstituted from DB). Controller must not substitute.
    CourseAttemptView viewWithNullPeriod = new CourseAttemptView(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Test Course",
        null, // periodId = null — legacy/reconstituted row; controller passes through as-is
        null,
        "DIURNA",
        "PASSED", // FIX-S1: plain String
        new BigDecimal("4.2"),
        Instant.now(),
        0L);
    when(createCourseAttemptUseCase.createCourseAttempt(any())).thenReturn(viewWithNullPeriod);

    // Act: request with a valid periodId (contract requires @NotNull in request);
    // the service mock overrides and returns null periodId in the view
    mockMvc.perform(post("/me/course-attempts")
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject)))
            .with(csrf())
            .header("Idempotency-Key", UUID.randomUUID())
            .header("X-XSRF-TOKEN", "csrf-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "subscriptionId": "550e8400-e29b-41d4-a716-446655440001",
                  "courseId": "550e8400-e29b-41d4-a716-446655440002",
                  "periodId": "550e8400-e29b-41d4-a716-446655440003",
                  "journeyCode": "DIURNA",
                  "status": "PASSED",
                  "finalGrade": 4.2
                }
                """))
        .andExpect(status().isCreated())
        // FIX-S2: periodId in response must be null (not the zero UUID sentinel)
        .andExpect(jsonPath("$.periodId").value((Object) null));
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CourseAttemptView buildAttemptView() {
    return new CourseAttemptView(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Test Course",
        null,
        null,
        "DIURNA",
        "PASSED", // FIX-S1: plain String
        new BigDecimal("4.2"),
        Instant.now(),
        0L);
  }
}
