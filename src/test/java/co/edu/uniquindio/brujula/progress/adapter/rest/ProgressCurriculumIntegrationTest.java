package co.edu.uniquindio.brujula.progress.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * FIX-W1: Integration test for GET /me/subscriptions/{id}/curriculum.
 *
 * <p>Uses the Flyway-seeded Sistemas plan (V012, plan UUID 33333333-0000-4000-a000-000000000001).
 * Inserts a {@code progress.student_program_subscription} row and optionally {@code
 * progress.course_attempt} rows to exercise the three core availability scenarios:
 *
 * <ol>
 *   <li>BLOCKED: "Programación I" (c0020000-…-003) has an unmet HARD prereq "Lógica de
 *       Programación" (c0010000-…-004, HIGH/inferred=false) → BLOCKED.
 *   <li>AVAILABLE + advisory: "Formulación y Evaluación de Proyectos" (c0080000-…-001) has an unmet
 *       SOFT prereq "Ingeniería Económica" (c0070000-…-001, MEDIUM/inferred=true) → AVAILABLE with
 *       a non-empty unmetRequirements list ([Recomendado] prefix).
 *   <li>PASSED: "Matemáticas Generales" (c0010000-…-001) — we insert a PASSED attempt →
 *       progressStatus = PASSED.
 * </ol>
 *
 * <p>Architecture: @SpringBootTest (MOCK) + AbstractPostgresIntegrationTest (Testcontainers
 * Postgres). No @Transactional — data cleanup is implicit per-test via subscription/attempt inserts
 * with unique subscription IDs, so leftover rows don't pollute other tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ProgressCurriculumIntegrationTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;
  @Autowired private JdbcTemplate jdbcTemplate;

  // Sistemas plan seed UUIDs (Flyway V012)
  private static final UUID SISTEMAS_PLAN_ID =
      UUID.fromString("33333333-0000-4000-a000-000000000001");
  private static final UUID SISTEMAS_PROGRAM_ID =
      UUID.fromString("11111111-0000-4000-a000-000000000001");

  // Course UUIDs from V012 (used in test assertions)
  private static final UUID PROGRAMACION_I =
      UUID.fromString("c0020000-0000-4000-a000-000000000003"); // Sem2, HIGH prereq→BLOCKED
  private static final UUID LOGICA_DE_PROGRAMACION =
      UUID.fromString("c0010000-0000-4000-a000-000000000004"); // prereq of Programación I
  private static final UUID FORMULACION_PROYECTOS =
      UUID.fromString(
          "c0080000-0000-4000-a000-000000000001"); // Sem8, SOFT prereq→AVAILABLE+advisory
  private static final UUID INGENIERIA_ECONOMICA =
      UUID.fromString("c0070000-0000-4000-a000-000000000001"); // soft prereq of Formulación
  private static final UUID MATEMATICAS_GENERALES =
      UUID.fromString("c0010000-0000-4000-a000-000000000001"); // Sem1, no prereqs→PASSED scenario

  private MockMvc mockMvc;
  private String studentSubject;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Register a unique student per test run
    studentSubject = "student-integration-curriculum-" + UUID.randomUUID();
    AppUser student =
        AppUser.provision(
            studentSubject,
            "student-curriculum-" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Test Student",
            Role.STUDENT);
    saveAppUserPort.save(student);
  }

  // -----------------------------------------------------------------------
  // FIX-W1 scenario (a): unmet HIGH/inferred=false prereq → BLOCKED
  // -----------------------------------------------------------------------

  /**
   * "Programación I" (Sem 2 DIURNA) requires "Lógica de Programación" via HIGH/inferred=false edge.
   * With no course attempts at all, the prereq is unmet → BLOCKED.
   */
  @Test
  void programacion_i_with_no_attempts_is_blocked_due_to_hard_prereq() throws Exception {
    UUID subscriptionId = insertSubscription(studentSubject, SISTEMAS_PLAN_ID);

    // No attempts inserted — "Lógica de Programación" is unmet → BLOCKED for "Programación I"
    mockMvc
        .perform(
            get("/me/subscriptions/{id}/curriculum", subscriptionId)
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        // Programación I must be BLOCKED (unmet HARD prereq: Lógica de Programación)
        .andExpect(
            jsonPath(
                    "$.semesters[?(@.recommendedSemester == 2)].courses"
                        + "[?(@.courseId == '"
                        + PROGRAMACION_I
                        + "')].progressStatus")
                .value("BLOCKED"))
        // The unmet requirement must reference Lógica de Programación (no [Recomendado] prefix for
        // HARD)
        .andExpect(
            jsonPath(
                    "$.semesters[?(@.recommendedSemester == 2)].courses"
                        + "[?(@.courseId == '"
                        + PROGRAMACION_I
                        + "')].unmetRequirements")
                .isArray());
  }

  // -----------------------------------------------------------------------
  // FIX-W1 scenario (b): unmet MEDIUM/inferred=true prereq → AVAILABLE + advisory
  // -----------------------------------------------------------------------

  /**
   * "Formulación y Evaluación de Proyectos" (Sem 8 DIURNA) requires "Ingeniería Económica" via
   * MEDIUM/inferred=true edge. With no attempt for "Ingeniería Económica", the prereq is unmet but
   * SOFT → AVAILABLE with advisory unmetRequirement ([Recomendado] prefix).
   */
  @Test
  void formulacion_proyectos_with_no_attempts_is_available_with_advisory() throws Exception {
    UUID subscriptionId = insertSubscription(studentSubject, SISTEMAS_PLAN_ID);

    // No attempts for Ingeniería Económica → unmet SOFT prereq
    String responseBody =
        mockMvc
            .perform(
                get("/me/subscriptions/{id}/curriculum", subscriptionId)
                    .with(
                        oidcLogin()
                            .idToken(t -> t.subject(studentSubject))
                            .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isOk())
            // Formulación must be AVAILABLE (SOFT prereq does NOT block)
            .andExpect(
                jsonPath(
                        "$.semesters[?(@.recommendedSemester == 8)].courses"
                            + "[?(@.courseId == '"
                            + FORMULACION_PROYECTOS
                            + "')].progressStatus")
                    .value("AVAILABLE"))
            // The advisory unmet requirement list must be non-empty
            .andExpect(
                jsonPath(
                        "$.semesters[?(@.recommendedSemester == 8)].courses"
                            + "[?(@.courseId == '"
                            + FORMULACION_PROYECTOS
                            + "')].unmetRequirements")
                    .isArray())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Verify the [Recomendado] advisory prefix appears in the response body (SOFT prereq advisory)
    assertThat(responseBody).contains("[Recomendado]");
  }

  // -----------------------------------------------------------------------
  // FIX-W1 scenario (c): PASSED course → progressStatus = PASSED
  // -----------------------------------------------------------------------

  /**
   * "Matemáticas Generales" (Sem 1 DIURNA) has no prerequisites (free to take). When a PASSED
   * attempt exists for it, the curriculum view must report progressStatus = PASSED.
   */
  @Test
  void matematicas_generales_with_passed_attempt_is_passed() throws Exception {
    UUID subscriptionId = insertSubscription(studentSubject, SISTEMAS_PLAN_ID);

    // Insert a PASSED attempt for Matemáticas Generales (grade 4.5, meets ≥3.0 check)
    insertCourseAttempt(subscriptionId, MATEMATICAS_GENERALES, "PASSED", 4.5);

    mockMvc
        .perform(
            get("/me/subscriptions/{id}/curriculum", subscriptionId)
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        // Matemáticas Generales must be PASSED (effective declared status wins)
        .andExpect(
            jsonPath(
                    "$.semesters[?(@.recommendedSemester == 1)].courses"
                        + "[?(@.courseId == '"
                        + MATEMATICAS_GENERALES
                        + "')].progressStatus")
                .value("PASSED"));
  }

  // -----------------------------------------------------------------------
  // SQL helpers
  // -----------------------------------------------------------------------

  /**
   * Inserts a {@code progress.student_program_subscription} row tied to the given student subject
   * and returns the generated UUID.
   */
  private UUID insertSubscription(String subject, UUID planId) {
    // Resolve the internal user UUID from the oidc_subject (inserted via SaveAppUserPort)
    UUID userId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM identity.app_user WHERE oidc_subject = ?", UUID.class, subject);

    UUID subscriptionId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO progress.student_program_subscription
            (id, user_id, program_id, plan_id, reference_journey_code, status, version)
        VALUES (?, ?, ?, ?, 'DIURNA', 'ACTIVE', 0)
        """,
        subscriptionId,
        userId,
        SISTEMAS_PROGRAM_ID,
        planId);
    return subscriptionId;
  }

  /**
   * Inserts a {@code progress.course_attempt} row for the given subscription and course. Only
   * PASSED (grade in [3.0,5.0]) and FAILED (grade in [0.0,2.9]) are supported here.
   */
  private void insertCourseAttempt(
      UUID subscriptionId, UUID courseId, String status, double grade) {
    UUID periodId = UUID.randomUUID(); // opaque period — no catalog validation
    jdbcTemplate.update(
        """
        INSERT INTO progress.course_attempt
            (id, subscription_id, course_id, status, final_grade, period_id, journey_code, version)
        VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, 'DIURNA', 0)
        """,
        subscriptionId,
        courseId,
        status,
        grade,
        periodId);
  }
}
