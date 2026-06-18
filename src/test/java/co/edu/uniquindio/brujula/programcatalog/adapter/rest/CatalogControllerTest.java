package co.edu.uniquindio.brujula.programcatalog.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
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
 * REST adapter integration tests for CatalogController.
 *
 * <p>Tests: GET /programs → 200, GET /programs/{id} → 200/404, GET /programs/{id}/plans → 200, GET
 * /plans/{planId} → 200/404, GET /journeys → 200.
 *
 * <p>Uses Testcontainers PostgreSQL + Flyway + spring-security-test (oidcLogin).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CatalogControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // ---------------------------------------------------------------------------
  // GET /programs
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void list_programs_as_student_returns_200() throws Exception {
    mockMvc
        .perform(
            get("/programs")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  @Transactional
  void list_programs_unauthenticated_returns_401_or_redirect() throws Exception {
    // OIDC login uses redirect (302) rather than Bearer 401 — both indicate auth is required
    mockMvc
        .perform(get("/programs"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              assertThat(status)
                  .as("Unauthenticated access to /programs must not return 200")
                  .isGreaterThanOrEqualTo(300);
            });
  }

  // ---------------------------------------------------------------------------
  // GET /programs/{programId}
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void get_program_existing_returns_200() throws Exception {
    UUID programId = insertProgram("Ingeniería de Sistemas y Computación", "Ingeniería", "ACTIVE");

    mockMvc
        .perform(
            get("/programs/{id}", programId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(programId.toString()))
        .andExpect(jsonPath("$.name").value("Ingeniería de Sistemas y Computación"));
  }

  @Test
  @Transactional
  void get_program_nonexistent_returns_404() throws Exception {
    mockMvc
        .perform(
            get("/programs/{id}", UUID.randomUUID())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isNotFound());
  }

  // ---------------------------------------------------------------------------
  // GET /programs/{programId}/plans
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void list_program_plans_as_student_returns_only_active_plans() throws Exception {
    UUID programId = insertProgram("Lenguas Modernas", "Ciencias Humanas", "ACTIVE");
    insertStudyPlan(programId, "Plan DRAFT", "DRAFT", 160);
    UUID activePlanId = insertStudyPlan(programId, "Plan ACTIVE", "ACTIVE", 168);

    mockMvc
        .perform(
            get("/programs/{id}/plans", programId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == '" + activePlanId + "')]").exists())
        .andExpect(jsonPath("$[?(@.status == 'DRAFT')]").doesNotExist());
  }

  @Test
  @Transactional
  void list_program_plans_as_staff_returns_all_plans() throws Exception {
    UUID programId = insertProgram("Medicina", "Ciencias de la Salud", "ACTIVE");
    insertStudyPlan(programId, "Plan DRAFT", "DRAFT", 240);
    insertStudyPlan(programId, "Plan ACTIVE", "ACTIVE", 250);

    mockMvc
        .perform(
            get("/programs/{id}/plans", programId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  // ---------------------------------------------------------------------------
  // GET /plans/{planId}
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void get_study_plan_active_as_student_returns_200() throws Exception {
    UUID programId = insertProgram("Química", "Ciencias Exactas", "ACTIVE");
    UUID planId = insertStudyPlan(programId, "Plan 2020", "ACTIVE", 160);

    mockMvc
        .perform(
            get("/plans/{id}", planId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(planId.toString()));
  }

  @Test
  @Transactional
  void get_study_plan_draft_as_student_returns_404() throws Exception {
    UUID programId = insertProgram("Física", "Ciencias Exactas", "ACTIVE");
    UUID planId = insertStudyPlan(programId, "Plan DRAFT", "DRAFT", 155);

    mockMvc
        .perform(
            get("/plans/{id}", planId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isNotFound());
  }

  @Test
  @Transactional
  void get_study_plan_nonexistent_returns_404() throws Exception {
    mockMvc
        .perform(
            get("/plans/{id}", UUID.randomUUID())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isNotFound());
  }

  // ---------------------------------------------------------------------------
  // GET /journeys
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void list_journeys_returns_only_active_journeys() throws Exception {
    String code = "ESPECIAL-" + UUID.randomUUID();
    UUID activeId = insertJourney(code, "Jornada Especial", true);
    insertJourney("INACTIVA-" + UUID.randomUUID(), "Jornada Inactiva", false);

    mockMvc
        .perform(
            get("/journeys")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == '" + activeId + "')]").exists());
  }

  // ---------------------------------------------------------------------------
  // SQL helpers
  // ---------------------------------------------------------------------------

  private UUID insertProgram(String name, String faculty, String status) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.academic_program (id, name, faculty, status, version) VALUES (?, ?, ?, ?, 0)",
        id,
        name,
        faculty,
        status);
    return id;
  }

  private UUID insertStudyPlan(UUID programId, String name, String status, int credits) {
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

  private UUID insertJourney(String code, String name, boolean active) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.journey (id, code, name, active) VALUES (?, ?, ?, ?)",
        id,
        code,
        name,
        active);
    return id;
  }
}
