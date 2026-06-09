package co.edu.uniquindio.brujula.curriculum.adapter.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * REST adapter integration tests for CurriculumController (Phase 4, WU4).
 *
 * <p>Tests: GET /courses/{id} → 200/404, GET /courses/{id}/requirements → 200, GET
 * /plans/{planId}/courses → 200 (with role filtering), GET /plans/{planId}/sequential-tracks → 200,
 * GET /plans/{planId}/degree-work-modalities → 200.
 *
 * <p>Uses Testcontainers PostgreSQL + Flyway + spring-security-test (oidcLogin). Satisfies:
 * CUR-001/CUR-003/CUR-005/CUR-006 REST adapters.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CurriculumControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // ---------------------------------------------------------------------------
  // GET /courses/{courseId}
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void get_course_existing_returns_200() throws Exception {
    UUID planId = insertStudyPlan("Plan Test REST", "ACTIVE", 160);
    UUID courseId = insertCourse(planId, "Cálculo I", "REQUIRED", 4, 4, "ACTIVE");

    mockMvc
        .perform(
            get("/courses/{id}", courseId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(courseId.toString()))
        .andExpect(jsonPath("$.name").value("Cálculo I"));
  }

  @Test
  @Transactional
  void get_course_nonexistent_returns_404() throws Exception {
    mockMvc
        .perform(
            get("/courses/{id}", UUID.randomUUID())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isNotFound());
  }

  // ---------------------------------------------------------------------------
  // GET /courses/{courseId}/requirements
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void list_course_requirements_returns_200() throws Exception {
    UUID planId = insertStudyPlan("Plan Requisitos REST", "ACTIVE", 150);
    UUID c1 = insertCourse(planId, "Cálculo II", "REQUIRED", 4, 4, "ACTIVE");
    UUID c2 = insertCourse(planId, "Cálculo I", "REQUIRED", 4, 4, "ACTIVE");
    insertCourseRequirementCoursePassed(c1, c2);

    mockMvc
        .perform(
            get("/courses/{id}/requirements", c1)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1));
  }

  // ---------------------------------------------------------------------------
  // GET /plans/{planId}/courses
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void list_plan_courses_as_student_excludes_inactive_courses() throws Exception {
    UUID planId = insertStudyPlan("Plan Cursos REST", "ACTIVE", 170);
    UUID activeId = insertCourse(planId, "Materia Activa", "REQUIRED", 3, 3, "ACTIVE");
    insertCourse(planId, "Materia Inactiva", "REQUIRED", 2, 2, "INACTIVE");

    mockMvc
        .perform(
            get("/plans/{planId}/courses", planId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id == '" + activeId + "')]").exists())
        .andExpect(jsonPath("$.content[?(@.status == 'INACTIVE')]").doesNotExist());
  }

  @Test
  @Transactional
  void list_plan_courses_as_staff_includes_all_statuses() throws Exception {
    UUID planId = insertStudyPlan("Plan Cursos Staff", "ACTIVE", 170);
    insertCourse(planId, "Materia Activa", "REQUIRED", 3, 3, "ACTIVE");
    insertCourse(planId, "Materia Inactiva", "REQUIRED", 2, 2, "INACTIVE");

    mockMvc
        .perform(
            get("/plans/{planId}/courses", planId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  // ---------------------------------------------------------------------------
  // GET /plans/{planId}/sequential-tracks
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void list_sequential_tracks_returns_200() throws Exception {
    UUID planId = insertStudyPlan("Plan Tracks REST", "ACTIVE", 165);
    insertSequentialTrack(planId, "PERSONAL_ELECTIVE", "Electiva Humanística", false, null);

    mockMvc
        .perform(
            get("/plans/{planId}/sequential-tracks", planId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @Transactional
  void list_sequential_tracks_with_gatekeeper_exposes_gatekeeperCourseId() throws Exception {
    UUID planId = insertStudyPlan("Plan GK REST", "ACTIVE", 170);
    UUID gkCourse = insertCourse(planId, "Curso Gatekeeper", "REQUIRED", 3, 3, "ACTIVE");
    insertSequentialTrack(planId, "DEEPENING", "Profundización", false, gkCourse);

    mockMvc
        .perform(
            get("/plans/{planId}/sequential-tracks", planId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].gatekeeperCourseId").value(gkCourse.toString()));
  }

  // ---------------------------------------------------------------------------
  // GET /plans/{planId}/degree-work-modalities
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void list_degree_work_modalities_returns_200() throws Exception {
    UUID planId = insertStudyPlan("Plan TdG REST", "ACTIVE", 160);
    insertDegreeWorkModality(planId, "Aplicación", 6, 6, "ACTIVE");

    mockMvc
        .perform(
            get("/plans/{planId}/degree-work-modalities", planId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Aplicación"));
  }

  // ---------------------------------------------------------------------------
  // GET /plans/{planId}/malla
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void get_plan_malla_returns_200_with_semester_buckets() throws Exception {
    UUID planId = insertStudyPlan("Plan Malla REST", "ACTIVE", 160);
    insertCourse(planId, "Programación I", "REQUIRED", 4, 4, "ACTIVE");
    // INACTIVE course must be excluded from malla
    insertCourse(planId, "Materia Inactiva", "REQUIRED", 2, 2, "INACTIVE");

    mockMvc
        .perform(
            get("/plans/{planId}/malla", planId)
                .param("journeyCode", "DIURNA")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.planId").value(planId.toString()))
        .andExpect(jsonPath("$.journeyCode").value("DIURNA"))
        .andExpect(jsonPath("$.semesterBuckets").isArray());
  }

  @Test
  @Transactional
  void get_plan_malla_nocturna_returns_nocturna_buckets() throws Exception {
    UUID planId = insertStudyPlan("Plan Malla NOCTURNA REST", "ACTIVE", 160);
    insertCourseWithSemester(planId, "Redes", "REQUIRED", 3, 3, "ACTIVE", "NOCTURNA", 5);

    mockMvc
        .perform(
            get("/plans/{planId}/malla", planId)
                .param("journeyCode", "NOCTURNA")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.journeyCode").value("NOCTURNA"))
        .andExpect(jsonPath("$.semesterBuckets").isArray())
        .andExpect(jsonPath("$.semesterBuckets.length()").value(1))
        .andExpect(jsonPath("$.semesterBuckets[0].recommendedSemester").value(5));
  }

  /**
   * W-1 (INT-5): asserts that the malla HTTP JSON exposes requirement confidence/inferred fields
   * and graduationRequirements. Uses the Flyway-seeded Sistemas plan (V012): Programación I (Sem 2
   * DIURNA) has a COURSE_PASSED prerequisite → Lógica de Programación with confidence=HIGH and
   * inferred=false. The graduation advisory must also be non-empty.
   */
  @Test
  void get_plan_malla_seeded_sistemas_exposes_requirements_confidence_and_graduation_requirements()
      throws Exception {
    // Sistemas plan seeded by V012 — fixed UUID, no @Transactional (read-only against real seed)
    String sistemasPlanId = "33333333-0000-4000-a000-000000000001";

    mockMvc
        .perform(
            get("/plans/{planId}/malla", sistemasPlanId)
                .param("journeyCode", "DIURNA")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        // graduationRequirements must be present and non-empty (CUR-008 §3)
        .andExpect(jsonPath("$.graduationRequirements").isString())
        .andExpect(jsonPath("$.graduationRequirements").isNotEmpty())
        // Semester 2 must exist (Programación I → Lógica de Programación edge is seeded there)
        .andExpect(jsonPath("$.semesterBuckets[?(@.recommendedSemester == 2)]").exists())
        // Programación I (c0020000-…-003) in Sem 2 must have at least one requirement
        .andExpect(
            jsonPath(
                    "$.semesterBuckets[?(@.recommendedSemester == 2)].courses"
                        + "[?(@.officialCode == 'ISC-S2-PROGRAMACIONI')].requirements")
                .exists())
        .andExpect(
            jsonPath(
                    "$.semesterBuckets[?(@.recommendedSemester == 2)].courses"
                        + "[?(@.officialCode == 'ISC-S2-PROGRAMACIONI')].requirements[0]")
                .exists())
        // The requirement edge must carry confidence and inferred fields (W-1 regression guard)
        .andExpect(
            jsonPath(
                    "$.semesterBuckets[?(@.recommendedSemester == 2)].courses"
                        + "[?(@.officialCode == 'ISC-S2-PROGRAMACIONI')].requirements[0].confidence")
                .value("HIGH"))
        .andExpect(
            jsonPath(
                    "$.semesterBuckets[?(@.recommendedSemester == 2)].courses"
                        + "[?(@.officialCode == 'ISC-S2-PROGRAMACIONI')].requirements[0].inferred")
                .value(false));
  }

  // ---------------------------------------------------------------------------
  // SQL insert helpers
  // ---------------------------------------------------------------------------

  private UUID insertStudyPlan(String name, String status, int credits) {
    UUID programId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.academic_program (id, name, faculty, status, version) VALUES (?, ?, ?, 'ACTIVE', 0)",
        programId,
        "Prog-" + UUID.randomUUID(),
        "Facultad");
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, version) VALUES (?, ?, ?, ?, ?, 0)",
        id,
        programId,
        name,
        status,
        credits);
    return id;
  }

  private UUID insertCourse(
      UUID planId, String name, String type, int credits, int hours, String status) {
    UUID programId =
        jdbcTemplate.queryForObject(
            "SELECT program_id FROM catalog.study_plan WHERE id = ?", UUID.class, planId);
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO curriculum.course (id, plan_id, program_id, name, type, credits, curricular_weekly_hours, status, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
        id,
        planId,
        programId,
        name,
        type,
        credits,
        hours,
        status);
    return id;
  }

  private void insertCourseRequirementCoursePassed(UUID targetCourseId, UUID requiredCourseId) {
    jdbcTemplate.update(
        "INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, status, version) VALUES (?, ?, 'COURSE_PASSED', ?, 'ACTIVE', 0)",
        UUID.randomUUID(),
        targetCourseId,
        requiredCourseId);
  }

  private void insertSequentialTrack(
      UUID planId,
      String kind,
      String label,
      boolean consecutiveSimultaneityAllowed,
      UUID gatekeeperCourseId) {
    jdbcTemplate.update(
        "INSERT INTO curriculum.sequential_track (id, plan_id, kind, label, consecutive_simultaneity_allowed, gatekeeper_course_id, version) VALUES (?, ?, ?, ?, ?, ?, 0)",
        UUID.randomUUID(),
        planId,
        kind,
        label,
        consecutiveSimultaneityAllowed,
        gatekeeperCourseId);
  }

  private void insertDegreeWorkModality(
      UUID planId, String name, Integer credits, Integer hours, String status) {
    jdbcTemplate.update(
        "INSERT INTO curriculum.degree_work_modality (id, plan_id, name, credits, curricular_weekly_hours, status, version) VALUES (?, ?, ?, ?, ?, ?, 0)",
        UUID.randomUUID(),
        planId,
        name,
        credits,
        hours,
        status);
  }

  private UUID insertCourseWithSemester(
      UUID planId,
      String name,
      String type,
      int credits,
      int hours,
      String status,
      String journeyCode,
      int semester) {
    UUID courseId = insertCourse(planId, name, type, credits, hours, status);
    jdbcTemplate.update(
        "INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES (?, ?, ?)",
        courseId,
        journeyCode,
        semester);
    return courseId;
  }
}
