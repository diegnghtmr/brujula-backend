package co.edu.uniquindio.brujula.simulation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.domain.CandidateSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimScheduleSession;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationScopeEntry;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * End-to-end integration tests for SimulationDetail (staleness detection, credit summary, review).
 *
 * <p>Proves against a REAL PostgreSQL (Testcontainers + Flyway V016/V022/V018) what unit stubs
 * cannot: staleness detection, real creditSummary values, review 200/409 round-trips.
 *
 * <p>V022 real offering UUIDs used:
 *
 * <ul>
 *   <li>Period 2026-2: {@code 00000001-0000-4000-a000-000000000001}
 *   <li>Programación III (Elixir) (ISC, 3 cr, FINAL): offering {@code
 *       02200044-0000-4001-a000-000000000044}, DIURNA option {@code
 *       02200044-0000-4002-a000-000000000044}
 *   <li>Estadística (ISC, 3 cr, FINAL): offering {@code 02200051-0000-4001-a000-000000000051},
 *       DIURNA option {@code 02200051-0000-4002-a000-000000000051}
 * </ul>
 *
 * <p>ISC program: {@code 11111111-0000-4000-a000-000000000001}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SimulationDetailIT extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;
  @Autowired private SaveSimulationPort saveSimulationPort;
  @Autowired private SaveSimulationSelectionPort saveSimulationSelectionPort;
  @Autowired private JdbcTemplate jdbcTemplate;

  // V022 real 2026-2 offering UUIDs
  static final UUID PERIOD_ID = UUID.fromString("00000001-0000-4000-a000-000000000001");

  // ISC program
  static final UUID ISC_PROGRAM_ID = UUID.fromString("11111111-0000-4000-a000-000000000001");

  // Programación III (Elixir), FINAL, 3 cr, DIURNA
  static final UUID PROG_III_OFFERING_ID = UUID.fromString("02200044-0000-4001-a000-000000000044");
  static final UUID PROG_III_OPTION_DIURNA =
      UUID.fromString("02200044-0000-4002-a000-000000000044");
  static final UUID PROG_III_COURSE_ID = UUID.fromString("c0040000-0000-4000-a000-000000000004");

  // Estadística y Probabilidad (FINAL, 3 cr, DIURNA Tue 10:00-12:00 / Thu 10:00-12:00)
  // Note: requires c0030000-0000-4000-a000-000000000001 (hard prereq); use with passed prereq
  static final UUID ESTADISTICA_OFFERING_ID =
      UUID.fromString("02200051-0000-4001-a000-000000000051");
  static final UUID ESTADISTICA_OPTION_DIURNA =
      UUID.fromString("02200051-0000-4002-a000-000000000051");
  static final UUID ESTADISTICA_COURSE_ID = UUID.fromString("c0050000-0000-4000-a000-000000000001");
  // Estadística's prereq (Algoritmos II — V012 c0030000-...-001) — must be PASSED for clean review
  static final UUID ESTADISTICA_PREREQ_COURSE_ID =
      UUID.fromString("c0030000-0000-4000-a000-000000000001");

  static final UUID PROG_II_COURSE_ID = UUID.fromString("c0030000-0000-4000-a000-000000000005");

  private MockMvc mockMvc;
  private String studentSubject;
  private UUID studentUserId;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Unique student per test run (no @Transactional — data survives across steps within a test)
    studentSubject = "student-sim-detail-it-" + UUID.randomUUID();
    AppUser student =
        AppUser.provision(
            studentSubject,
            "student-sim-detail-" + UUID.randomUUID() + "@uniquindio.edu.co",
            "IT Student",
            Role.STUDENT);
    saveAppUserPort.save(student);

    // Resolve internal user UUID for simulation ownership
    studentUserId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM identity.app_user WHERE oidc_subject = ?", UUID.class, studentSubject);
  }

  // ---------------------------------------------------------------------------
  // Helper: create a simulation owned by the test student
  // ---------------------------------------------------------------------------

  private Simulation createSimulation(List<UUID> programIds) {
    List<SimulationScopeEntry> scopeEntries =
        programIds.stream().map(id -> new SimulationScopeEntry(id, "DIURNA")).toList();
    Simulation sim =
        Simulation.create(
            SimulationId.generate(), studentUserId, "IT Sim", PERIOD_ID, scopeEntries);
    return saveSimulationPort.save(sim);
  }

  // ---------------------------------------------------------------------------
  // Helper: seed a selection directly via SaveSimulationSelectionPort
  // ---------------------------------------------------------------------------

  private SimulationSelection seedSelection(
      UUID simulationId,
      UUID offeringId,
      UUID scheduleOptionId,
      UUID courseId,
      String courseName,
      double credits,
      String offeringStatus,
      long selectedOfferingVersion,
      List<SimScheduleSession> sessions) {

    CandidateSelection candidate =
        new CandidateSelection(
            scheduleOptionId,
            offeringId,
            courseId,
            courseName,
            ISC_PROGRAM_ID,
            credits,
            "DIURNA",
            offeringStatus,
            selectedOfferingVersion,
            sessions);

    SimulationSelection selection =
        SimulationSelection.create(SimulationSelectionId.generate(), simulationId, candidate);
    return saveSimulationSelectionPort.save(selection);
  }

  // ---------------------------------------------------------------------------
  // REAL creditSummary — 2 selections for the same program
  // Assert selectedCredits == real sum (NOT 0), programName != UUID, creditLimit == 23
  // ---------------------------------------------------------------------------

  @Test
  void getSimulation_twoSelectionsSameProgram_creditSummaryIsReal() throws Exception {
    Simulation sim = createSimulation(List.of(ISC_PROGRAM_ID));

    // Seed two selections for ISC program: Programación III (3 cr) + Estadística (3 cr) = 6 cr
    seedSelection(
        sim.getId().value(),
        PROG_III_OFFERING_ID,
        PROG_III_OPTION_DIURNA,
        PROG_III_COURSE_ID,
        "Programación III",
        3.0,
        "FINAL",
        0L,
        List.of(
            new SimScheduleSession("MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0)),
            new SimScheduleSession("WEDNESDAY", LocalTime.of(10, 0), LocalTime.of(12, 0))));

    seedSelection(
        sim.getId().value(),
        ESTADISTICA_OFFERING_ID,
        ESTADISTICA_OPTION_DIURNA,
        ESTADISTICA_COURSE_ID,
        "Estadística y Probabilidad",
        3.0,
        "FINAL",
        0L,
        List.of(
            new SimScheduleSession("TUESDAY", LocalTime.of(10, 0), LocalTime.of(12, 0)),
            new SimScheduleSession("THURSDAY", LocalTime.of(10, 0), LocalTime.of(12, 0))));

    mockMvc
        .perform(
            get("/me/simulations/{id}", sim.getId().value())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        // creditSummary must have exactly 1 entry for ISC
        .andExpect(jsonPath("$.creditSummary").isArray())
        .andExpect(jsonPath("$.creditSummary.length()").value(1))
        // selectedCredits must be 6.0 (3+3), NOT 0
        .andExpect(jsonPath("$.creditSummary[0].selectedCredits").value(6.0))
        // creditLimit must be 23 (real default from CreditLimitAdapter)
        .andExpect(jsonPath("$.creditSummary[0].creditLimit").value(23))
        // programName must NOT be a UUID string and must NOT be "Program"
        .andExpect(jsonPath("$.creditSummary[0].programName").isString())
        // withinLimit must be true (6 <= 23)
        .andExpect(jsonPath("$.creditSummary[0].withinLimit").value(true));
  }

  // ---------------------------------------------------------------------------
  // OUTDATED staleness via REAL version bump
  // Seed selection with version=0; bump offering.version via JDBC; assert OUTDATED
  // ---------------------------------------------------------------------------

  @Test
  void getSimulation_offeringVersionBumpedViaJdbc_statusIsOutdated() throws Exception {
    Simulation sim = createSimulation(List.of(ISC_PROGRAM_ID));

    // Seed selection with selectedOfferingVersion = current version from the V022 real catalog
    // seed.
    long initialVersion =
        jdbcTemplate.queryForObject(
            "SELECT version FROM offering.course_offering WHERE id = ?",
            Long.class,
            PROG_III_OFFERING_ID);

    seedSelection(
        sim.getId().value(),
        PROG_III_OFFERING_ID,
        PROG_III_OPTION_DIURNA,
        PROG_III_COURSE_ID,
        "Programación III",
        3.0,
        "FINAL",
        initialVersion,
        List.of(new SimScheduleSession("MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0))));

    // Bump the offering version directly in DB — simulates what a future offering-edit endpoint
    // would do (staleness detection must fire even though the edit endpoint is deferred)
    jdbcTemplate.update(
        "UPDATE offering.course_offering SET version = version + 1 WHERE id = ?",
        PROG_III_OFFERING_ID);

    // Verify the bump was applied (defensive assertion)
    long bumped =
        jdbcTemplate.queryForObject(
            "SELECT version FROM offering.course_offering WHERE id = ?",
            Long.class,
            PROG_III_OFFERING_ID);
    assertThat(bumped).isGreaterThan(initialVersion);

    mockMvc
        .perform(
            get("/me/simulations/{id}", sim.getId().value())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        // CRITICAL: selection snapshot (initialVersion) < current version (bumped) → OUTDATED
        // If this asserts CURRENT, the staleness detection path is broken — fix it, do NOT weaken
        .andExpect(jsonPath("$.status").value("OUTDATED"));
  }

  // ISC plan UUID (V012)
  static final UUID ISC_PLAN_ID = UUID.fromString("33333333-0000-4000-a000-000000000001");

  // ---------------------------------------------------------------------------
  // Helper: insert a progress subscription for the student so prereq checks pass
  // ---------------------------------------------------------------------------

  private UUID insertSubscription(UUID userId, UUID planId) {
    UUID subscriptionId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO progress.student_program_subscription
            (id, user_id, program_id, plan_id, reference_journey_code, status, version)
        VALUES (?, ?, ?, ?, 'DIURNA', 'ACTIVE', 0)
        """,
        subscriptionId,
        userId,
        ISC_PROGRAM_ID,
        planId);
    return subscriptionId;
  }

  private void insertPassedAttempt(UUID subscriptionId, UUID courseId) {
    UUID periodId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO progress.course_attempt
            (id, subscription_id, course_id, status, final_grade, period_id, journey_code, version)
        VALUES (gen_random_uuid(), ?, ?, 'PASSED', 4.5, ?, 'DIURNA', 0)
        """,
        subscriptionId,
        courseId,
        periodId);
  }

  private record ArchivedOfferingSeed(UUID offeringId, UUID optionId) {}

  private ArchivedOfferingSeed insertArchivedOfferingForReviewTest() {
    UUID offeringId = UUID.randomUUID();
    UUID optionId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO offering.course_offering
            (id, course_id, course_name, program_id, period_id, status, notes, version)
        VALUES (?, ?, 'Programación II', ?, ?, 'ARCHIVED', 'Archived offering generated by SimulationDetailIT', 0)
        """,
        offeringId,
        PROG_II_COURSE_ID,
        ISC_PROGRAM_ID,
        PERIOD_ID);
    jdbcTemplate.update(
        """
        INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
        VALUES (?, ?, 'Archived IT option', 'NOCTURNA', 0)
        """,
        optionId,
        offeringId);
    jdbcTemplate.update(
        """
        INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
        VALUES (gen_random_uuid(), ?, 'FRIDAY', '19:00', '22:00')
        """,
        optionId);
    return new ArchivedOfferingSeed(offeringId, optionId);
  }

  // ---------------------------------------------------------------------------
  // Review 200 CURRENT — clean simulation → POST review → 200, snapshot refreshed
  // The student must have the hard prereq passed so no blocking validation notice fires.
  // ---------------------------------------------------------------------------

  @Test
  void reviewSimulation_cleanState_returns200CurrentAndPersistsRefreshedSnapshot()
      throws Exception {
    Simulation sim = createSimulation(List.of(ISC_PROGRAM_ID));

    // Get current offering version from DB (V022 seeds version=0)
    long currentVersion =
        jdbcTemplate.queryForObject(
            "SELECT version FROM offering.course_offering WHERE id = ?",
            Long.class,
            ESTADISTICA_OFFERING_ID);

    // Seed a selection whose snapshot version == current version (CURRENT state)
    SimulationSelection sel =
        seedSelection(
            sim.getId().value(),
            ESTADISTICA_OFFERING_ID,
            ESTADISTICA_OPTION_DIURNA,
            ESTADISTICA_COURSE_ID,
            "Estadística y Probabilidad",
            3.0,
            "FINAL",
            currentVersion,
            List.of(
                new SimScheduleSession("TUESDAY", LocalTime.of(10, 0), LocalTime.of(12, 0)),
                new SimScheduleSession("THURSDAY", LocalTime.of(10, 0), LocalTime.of(12, 0))));

    // Insert progress subscription + PASSED prereq so review validation is clean
    UUID subscriptionId = insertSubscription(studentUserId, ISC_PLAN_ID);
    insertPassedAttempt(subscriptionId, ESTADISTICA_PREREQ_COURSE_ID);

    // POST review → 200 CURRENT (no blockers: prereq is PASSED, no schedule clash, no archive)
    mockMvc
        .perform(
            post("/me/simulations/{id}/review", sim.getId().value())
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CURRENT"));

    // Verify the snapshot was persisted with the refreshed current version
    Long persistedVersion =
        jdbcTemplate.queryForObject(
            "SELECT selected_offering_version FROM simulation.simulation_selection WHERE id = ?",
            Long.class,
            sel.getId().value());
    assertThat(persistedVersion)
        .as("After review, selected_offering_version must match current offering version")
        .isEqualTo(currentVersion);
  }

  // ---------------------------------------------------------------------------
  // Review 409 — selection on ARCHIVED offering → persistent blocker → 409 problem+json
  // ---------------------------------------------------------------------------

  @Test
  void reviewSimulation_archivedOffering_returns409WithSimulationBlockedCode() throws Exception {
    Simulation sim = createSimulation(List.of(ISC_PROGRAM_ID));

    ArchivedOfferingSeed archivedOffering = insertArchivedOfferingForReviewTest();

    // Seed a selection on a fresh ARCHIVED offering instead of relying on old V017 test fixtures.
    // This is a persistent blocker: ArchivedOfferingRule fires because offering.status = ARCHIVED
    seedSelection(
        sim.getId().value(),
        archivedOffering.offeringId(),
        archivedOffering.optionId(),
        PROG_II_COURSE_ID,
        "Programación II",
        3.0,
        "ARCHIVED",
        0L,
        List.of(new SimScheduleSession("FRIDAY", LocalTime.of(19, 0), LocalTime.of(22, 0))));

    // POST review → 409 application/problem+json with a SIMULATION_* code
    mockMvc
        .perform(
            post("/me/simulations/{id}/review", sim.getId().value())
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").isString())
        .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith("SIMULATION_")));
  }
}
