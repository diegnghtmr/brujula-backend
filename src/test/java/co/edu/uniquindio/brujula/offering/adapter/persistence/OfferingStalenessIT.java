package co.edu.uniquindio.brujula.offering.adapter.persistence;

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
import co.edu.uniquindio.brujula.offering.application.dto.PublishOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.dto.UpdateOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.port.in.PublishOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.out.SaveOfferingPort;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionId;
import co.edu.uniquindio.brujula.offering.domain.ScheduleSession;
import co.edu.uniquindio.brujula.offering.domain.Weekday;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * End-to-end staleness tests that drive a {@code course_offering.version} bump through REAL
 * application services — NOT manual JDBC UPDATEs.
 *
 * <p>Three scenarios:
 *
 * <ol>
 *   <li>Version bump via {@link UpdateOfferingUseCase} → selection becomes OUTDATED via the pure
 *       version-comparison chain ({@code StalenessEvaluator} compares snapshot vs current).
 *   <li>Version bump via {@link PublishOfferingUseCase} (DRAFT→ARCHIVED on a fresh offering) →
 *       selection becomes OUTDATED via the <em>exception-sentinel</em> path: ARCHIVED fails {@code
 *       CourseOffering#isPublished()}, so {@code GetOfferingQuery.getOffering()} throws {@code
 *       OfferingNotFoundException}, and {@code GetSimulationService} injects sentinel {@code
 *       selectedVersion - 1} → OUTDATED. This proves the sentinel path is wired correctly but does
 *       NOT exercise the pure version-comparison chain for the publish use case.
 *   <li>Version bump via {@link PublishOfferingUseCase} (DRAFT→PRELIMINARY on a fresh offering with
 *       a schedule option) → selection becomes OUTDATED via the <em>pure version-comparison</em>
 *       chain. PRELIMINARY passes {@code CourseOffering#isPublished()}, so the offering stays
 *       resolvable after publish; {@code GetOfferingQuery.getOffering()} returns it, and staleness
 *       is decided by real version comparison — the sentinel branch is NOT taken. This scenario is
 *       the keystone proof that {@link PublishOfferingUseCase} activates the staleness mechanism
 *       through the same chain as scenario 1.
 * </ol>
 *
 * <p>All three assertions prove that the staleness detection path ({@code StalenessEvaluator} →
 * {@code OfferingReadAdapter.currentOfferingVersion}) is wired correctly against a real Postgres
 * container. A fake-green implementation that keeps version=0 in the DB would fail all three tests
 * because the {@code assertThat(version).isGreaterThan(initialVersion)} guard fires BEFORE the
 * MockMvc assertion.
 *
 * <p>Test isolation: Scenario 1 uses the V022 real 2026-2 catalog row {@code PROG_III_OFFERING_ID}
 * (FINAL) and only updates its notes — the offering stays FINAL. Scenarios 2 and 3 each create a
 * BRAND-NEW offering (random UUID) that is NOT a shared seed row, so no shared state is corrupted.
 *
 * <p>V022 real offering UUIDs used:
 *
 * <ul>
 *   <li>Programación III (Elixir) (FINAL, 3 cr): offering {@code
 *       02200044-0000-4001-a000-000000000044}, DIURNA option {@code
 *       02200044-0000-4002-a000-000000000044}
 *   <li>Period 2026-2: {@code 00000001-0000-4000-a000-000000000001}
 *   <li>ISC program: {@code 11111111-0000-4000-a000-000000000001}
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OfferingStalenessIT extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;
  @Autowired private SaveSimulationPort saveSimulationPort;
  @Autowired private SaveSimulationSelectionPort saveSimulationSelectionPort;
  @Autowired private SaveOfferingPort saveOfferingPort;
  @Autowired private UpdateOfferingUseCase updateOfferingUseCase;
  @Autowired private PublishOfferingUseCase publishOfferingUseCase;
  @Autowired private JdbcTemplate jdbcTemplate;

  // V022 real 2026-2 offering UUIDs
  static final UUID PERIOD_ID = UUID.fromString("00000001-0000-4000-a000-000000000001");
  static final UUID ISC_PROGRAM_ID = UUID.fromString("11111111-0000-4000-a000-000000000001");

  // Programación III (Elixir), FINAL, 3 cr, DIURNA — used ONLY for update
  static final UUID PROG_III_OFFERING_ID = UUID.fromString("02200044-0000-4001-a000-000000000044");
  static final UUID PROG_III_OPTION_DIURNA =
      UUID.fromString("02200044-0000-4002-a000-000000000044");
  static final UUID PROG_III_COURSE_ID = UUID.fromString("c0040000-0000-4000-a000-000000000004");

  private MockMvc mockMvc;
  private String studentSubject;
  private UUID studentUserId;
  private UUID staffActorId;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Unique student per test run (no @Transactional — data survives across steps)
    studentSubject = "student-staleness-it-" + UUID.randomUUID();
    AppUser student =
        AppUser.provision(
            studentSubject,
            "student-staleness-" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Staleness IT Student",
            Role.STUDENT);
    saveAppUserPort.save(student);

    studentUserId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM identity.app_user WHERE oidc_subject = ?", UUID.class, studentSubject);

    // Unique staff actor for use-case calls (actorId must resolve to a real user row)
    String staffSubject = "staff-staleness-it-" + UUID.randomUUID();
    AppUser staff =
        AppUser.provision(
            staffSubject,
            "staff-staleness-" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Staleness IT Staff",
            Role.STAFF);
    saveAppUserPort.save(staff);

    staffActorId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM identity.app_user WHERE oidc_subject = ?", UUID.class, staffSubject);
  }

  // ---------------------------------------------------------------------------
  // Helper: create a simulation owned by the test student
  // ---------------------------------------------------------------------------

  private Simulation createSimulation(List<UUID> programIds) {
    List<SimulationScopeEntry> scopeEntries =
        programIds.stream().map(id -> new SimulationScopeEntry(id, "DIURNA")).toList();
    Simulation sim =
        Simulation.create(
            SimulationId.generate(), studentUserId, "Staleness IT Sim", PERIOD_ID, scopeEntries);
    return saveSimulationPort.save(sim);
  }

  // ---------------------------------------------------------------------------
  // Helper: seed a selection pointing at a given offering at a given version
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
  // DEBT-F4-003 / scenario 1:
  //   Seed selection at version N on V022 PROG_III (FINAL).
  //   Bump offering version through UpdateOfferingUseCase (REAL service, NOT JDBC).
  //   Assert DB version > N AND simulation status = OUTDATED.
  //
  //   Isolation guarantee: notes update keeps offering at FINAL — the shared V022 row is NOT
  // mutated
  //   in a way that would affect other tests (still FINAL, just different notes).
  // ---------------------------------------------------------------------------

  @Test
  void offeringVersionBumpedViaUpdateService_simulationStatusIsOutdated() throws Exception {
    Simulation sim = createSimulation(List.of(ISC_PROGRAM_ID));

    // Read current DB version before any modification
    long initialVersion =
        jdbcTemplate.queryForObject(
            "SELECT version FROM offering.course_offering WHERE id = ?",
            Long.class,
            PROG_III_OFFERING_ID);

    // Seed a selection whose snapshot version == current version (CURRENT state before bump)
    seedSelection(
        sim.getId().value(),
        PROG_III_OFFERING_ID,
        PROG_III_OPTION_DIURNA,
        PROG_III_COURSE_ID,
        "Programación III",
        3.0,
        "FINAL",
        initialVersion,
        List.of(
            new SimScheduleSession("MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0)),
            new SimScheduleSession("WEDNESDAY", LocalTime.of(10, 0), LocalTime.of(12, 0))));

    // Bump offering version through the REAL UpdateOfferingService — NOT a direct JDBC UPDATE.
    // The staleness chain must work when the version increment is triggered by a real application
    // service call, not a test hack.
    UpdateOfferingCommand updateCmd =
        new UpdateOfferingCommand(
            PROG_III_OFFERING_ID,
            staffActorId,
            "Updated notes — staleness IT bump via REAL service",
            initialVersion);
    updateOfferingUseCase.updateOffering(updateCmd);

    // Guard: verify the DB version was actually incremented by the service
    long bumpedVersion =
        jdbcTemplate.queryForObject(
            "SELECT version FROM offering.course_offering WHERE id = ?",
            Long.class,
            PROG_III_OFFERING_ID);
    assertThat(bumpedVersion)
        .as("UpdateOfferingService MUST increment course_offering.version")
        .isGreaterThan(initialVersion);

    // CRITICAL: now the selection's selectedOfferingVersion (initialVersion) < bumpedVersion
    // → StalenessEvaluator.evaluate() must return OUTDATED → GET simulation must return OUTDATED.
    // If this asserts CURRENT, the real-service staleness activation path is broken.
    mockMvc
        .perform(
            get("/me/simulations/{id}", sim.getId().value())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OUTDATED"));
  }

  // ---------------------------------------------------------------------------
  // Scenario 2:
  //   Create a BRAND-NEW DRAFT offering (random UUID, not from V017) with a schedule option.
  //   Seed selection snapshot at version 0.
  //   Transition offering DRAFT→ARCHIVED through PublishOfferingUseCase (REAL service, NOT JDBC).
  //   Assert DB version > 0 AND simulation status = OUTDATED.
  //
  //   NOTE: OUTDATED is reached via the EXCEPTION-SENTINEL path, NOT the pure version-comparison
  //   chain. ARCHIVED fails CourseOffering#isPublished(), so GetOfferingQuery.getOffering()
  //   throws OfferingNotFoundException, and GetSimulationService injects sentinel
  //   (selectedVersion - 1) → StalenessEvaluator always returns OUTDATED. See scenario 3 for
  //   the pure version-comparison proof via PublishOfferingService.
  //
  //   Isolation guarantee: the fresh offering is NOT a V017 shared fixture, so archiving it
  //   does NOT affect other tests (SimulationDetailIT, etc.).
  // ---------------------------------------------------------------------------

  @Test
  void offeringVersionBumpedViaPublishService_simulationStatusIsOutdated() throws Exception {
    Simulation sim = createSimulation(List.of(ISC_PROGRAM_ID));

    // Create a fresh DRAFT offering with a schedule option (needed for DRAFT→ARCHIVED via real
    // service)
    // UUID for the fresh offering and its option — random, never conflicts with V017
    UUID freshOfferingId = UUID.randomUUID();
    UUID freshOptionId = UUID.randomUUID();

    // Build a schedule option so the offering has at least one real option in the DB.
    // DRAFT→ARCHIVED does NOT require options (requireSchedulableForPublish only fires for
    // PRELIMINARY/FINAL targets), but we include one so the selection snapshot is realistic.
    ScheduleSession session =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.FRIDAY, LocalTime.of(7, 0), LocalTime.of(9, 0));
    ScheduleOption option =
        ScheduleOption.reconstitute(
            ScheduleOptionId.of(freshOptionId),
            freshOfferingId,
            "Grupo IT",
            "DIURNA",
            List.of(session),
            0L);

    CourseOffering freshOffering =
        CourseOffering.create(
            CourseOfferingId.of(freshOfferingId),
            PROG_III_COURSE_ID,
            "IT Test Offering",
            ISC_PROGRAM_ID,
            PERIOD_ID,
            "Staleness IT offering",
            List.of(option));

    // Persist the fresh offering — SaveOfferingPort cascades the option
    CourseOffering saved = saveOfferingPort.save(freshOffering);
    long initialVersion = saved.getVersion();

    // Seed a selection snapshot at initialVersion
    seedSelection(
        sim.getId().value(),
        freshOfferingId,
        freshOptionId,
        PROG_III_COURSE_ID,
        "IT Test Offering",
        3.0,
        "DRAFT",
        initialVersion,
        List.of(new SimScheduleSession("FRIDAY", LocalTime.of(7, 0), LocalTime.of(9, 0))));

    // Transition DRAFT→ARCHIVED through the REAL PublishOfferingService.
    // DRAFT→ARCHIVED is legal (OfferingStatus.canTransitionTo) and does NOT require options.
    PublishOfferingCommand publishCmd =
        new PublishOfferingCommand(
            UUID.randomUUID(), // fresh idempotencyKey — no replay
            freshOfferingId,
            staffActorId,
            "ARCHIVED",
            initialVersion,
            false,
            "staleness-it-publish-hash-"
                + UUID.randomUUID()); // unique hash prevents idempotency replay

    publishOfferingUseCase.publishOffering(publishCmd);

    // Guard: verify the DB version was actually incremented by the service
    long bumpedVersion =
        jdbcTemplate.queryForObject(
            "SELECT version FROM offering.course_offering WHERE id = ?",
            Long.class,
            freshOfferingId);
    assertThat(bumpedVersion)
        .as("PublishOfferingService MUST increment course_offering.version")
        .isGreaterThan(initialVersion);

    // CRITICAL: staleness chain must fire — publish bumped the version, selection is OUTDATED.
    mockMvc
        .perform(
            get("/me/simulations/{id}", sim.getId().value())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OUTDATED"));
  }

  // ---------------------------------------------------------------------------
  // Scenario 3 (keystone proof for publish path, pure version-comparison):
  //   Create a BRAND-NEW DRAFT offering (random UUID, not from V017) with a schedule option.
  //   Seed selection snapshot at version 0.
  //   Transition offering DRAFT→PRELIMINARY through PublishOfferingUseCase (REAL service, NOT
  // JDBC).
  //
  //   PRELIMINARY passes CourseOffering#isPublished() → the offering STAYS RESOLVABLE after
  //   publish. GetOfferingQuery.getOffering() returns the offering (filter does NOT exclude it),
  //   so currentOfferingVersion() reads the post-publish version directly from the domain
  //   object. StalenessEvaluator then compares snapshot (0) vs bumped version (>0) and returns
  //   OUTDATED via the PURE VERSION-COMPARISON chain — the exception-sentinel branch is NOT taken.
  //
  //   This directly contrasts with scenario 2 (DRAFT→ARCHIVED, sentinel path) and proves that
  //   PublishOfferingService activates the same staleness mechanism as UpdateOfferingService
  //   (scenario 1). This test would FAIL if PublishOfferingService did not bump the version,
  //   because the version-comparison guard fires before the MockMvc assertion.
  //
  //   Isolation guarantee: the fresh offering is NOT a V017 shared fixture, so transitioning it
  //   to PRELIMINARY does NOT affect other tests (SimulationDetailIT, etc.).
  //
  //   Schedule option is REQUIRED: requireSchedulableForPublish enforces ≥1 option for PRELIMINARY.
  // ---------------------------------------------------------------------------

  @Test
  void
      offeringVersionBumpedViaPublishServiceToPreliminary_simulationStatusIsOutdated_viaVersionComparison()
          throws Exception {
    Simulation sim = createSimulation(List.of(ISC_PROGRAM_ID));

    // Create a fresh DRAFT offering with a schedule option.
    // DRAFT→PRELIMINARY requires at least one schedule option (requireSchedulableForPublish).
    // Use PROG_III_COURSE_ID so the intensity advisory call succeeds (course exists in V012).
    UUID freshOfferingId = UUID.randomUUID();
    UUID freshOptionId = UUID.randomUUID();

    ScheduleSession session =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.TUESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0));
    ScheduleOption option =
        ScheduleOption.reconstitute(
            ScheduleOptionId.of(freshOptionId),
            freshOfferingId,
            "Grupo Prelim IT",
            "DIURNA",
            List.of(session),
            0L);

    CourseOffering freshOffering =
        CourseOffering.create(
            CourseOfferingId.of(freshOfferingId),
            PROG_III_COURSE_ID,
            "IT Preliminary Test Offering",
            ISC_PROGRAM_ID,
            PERIOD_ID,
            "Staleness IT offering — PRELIMINARY path",
            List.of(option));

    CourseOffering saved = saveOfferingPort.save(freshOffering);
    long initialVersion = saved.getVersion();

    // Seed a selection snapshot at initialVersion (before the publish bump)
    seedSelection(
        sim.getId().value(),
        freshOfferingId,
        freshOptionId,
        PROG_III_COURSE_ID,
        "IT Preliminary Test Offering",
        3.0,
        "DRAFT",
        initialVersion,
        List.of(new SimScheduleSession("TUESDAY", LocalTime.of(14, 0), LocalTime.of(16, 0))));

    // Transition DRAFT→PRELIMINARY through the REAL PublishOfferingService.
    // PRELIMINARY requires at least one schedule option — satisfied above.
    // This is the keystone call: the real service must bump course_offering.version.
    PublishOfferingCommand publishCmd =
        new PublishOfferingCommand(
            UUID.randomUUID(), // fresh idempotencyKey — no replay
            freshOfferingId,
            staffActorId,
            "PRELIMINARY",
            initialVersion,
            false,
            "staleness-it-prelim-hash-"
                + UUID.randomUUID()); // unique hash prevents idempotency replay

    publishOfferingUseCase.publishOffering(publishCmd);

    // Guard: version was actually bumped by the service (would catch a no-op implementation)
    long bumpedVersion =
        jdbcTemplate.queryForObject(
            "SELECT version FROM offering.course_offering WHERE id = ?",
            Long.class,
            freshOfferingId);
    assertThat(bumpedVersion)
        .as("PublishOfferingService MUST increment course_offering.version for PRELIMINARY")
        .isGreaterThan(initialVersion);

    // Guard: offering must still be resolvable — isPublished() must return true for PRELIMINARY.
    // This confirms the sentinel branch will NOT be taken in GetSimulationService.
    String persistedStatus =
        jdbcTemplate.queryForObject(
            "SELECT status FROM offering.course_offering WHERE id = ?",
            String.class,
            freshOfferingId);
    assertThat(persistedStatus)
        .as(
            "Offering published to PRELIMINARY must have status PRELIMINARY (isPublished() == true)")
        .isEqualTo("PRELIMINARY");

    // CRITICAL: staleness is decided by real version comparison, NOT the exception-sentinel.
    // snapshot (initialVersion) < bumpedVersion → StalenessEvaluator returns OUTDATED.
    // If this asserts CURRENT, either the version was not bumped or the staleness chain is broken.
    mockMvc
        .perform(
            get("/me/simulations/{id}", sim.getId().value())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OUTDATED"));
  }
}
