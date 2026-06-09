package co.edu.uniquindio.brujula.simulation.adapter.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * QAL-SEC-001 — BOLA HTTP-layer ownership tests for simulation resources.
 *
 * <p>Uses {@code @SpringBootTest} + Testcontainers PostgreSQL (via {@link AbstractPostgresIntegrationTest})
 * + {@code spring-security-test} {@code oidcLogin()} with TWO distinct OIDC subjects.
 *
 * <p>Principal-B creates a simulation via the real persistence port (no service mock).
 * Principal-A then calls GET / PATCH / DELETE on that simulation via the real HTTP layer,
 * asserting HTTP 403 with {@code application/problem+json} and a {@code $.code} field.
 *
 * <p>ANTI-FAKE-GREEN: NO {@code @MockitoBean} on GetSimulationQuery, UpdateSimulationUseCase, or
 * DeleteSimulationUseCase. The real service + real persistence exercises the ownership logic
 * end-to-end through the HTTP adapter.
 *
 * <p>Scenarios covered:
 * <ul>
 *   <li>T-WU2.1 — cross-owner GET → 403 with application/problem+json and $.code
 *   <li>T-WU2.2 — cross-owner PATCH → 403 with application/problem+json and $.code
 *   <li>T-WU2.3 — cross-owner DELETE → 403 with application/problem+json and $.code
 * </ul>
 *
 * <p>Satisfies: QAL-SEC-001, QAL-SEC-001-S1, QAL-SEC-001-S2.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SimulationBolaTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;
  @Autowired private SaveSimulationPort saveSimulationPort;

  private MockMvc mockMvc;

  /** Principal-A: the attacker — attempts cross-owner access. */
  private String principalASubject;

  /** Principal-B: the legitimate owner of the simulation seeded in @BeforeEach. */
  private String principalBSubject;
  private UUID principalBUserId;

  /** The simulation owned by principal-B. */
  private UUID ownedSimulationId;
  private final UUID periodId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Principal-A: attacker (distinct sub + userId)
    principalASubject = "bola-sub-attacker-" + UUID.randomUUID();
    AppUser principalA = AppUser.provision(
        principalASubject,
        "attacker-" + UUID.randomUUID() + "@uniquindio.edu.co",
        "Principal A Attacker",
        Role.STUDENT);
    saveAppUserPort.save(principalA);

    // Principal-B: legitimate owner
    principalBSubject = "bola-sub-owner-" + UUID.randomUUID();
    AppUser principalB = AppUser.provision(
        principalBSubject,
        "owner-" + UUID.randomUUID() + "@uniquindio.edu.co",
        "Principal B Owner",
        Role.STUDENT);
    saveAppUserPort.save(principalB);
    principalBUserId = principalB.id();

    // Seed: create a simulation owned by principal-B using the real persistence port.
    // This exercises the real DB, so the real ownership check in the service fires.
    Simulation simOwnedByB = Simulation.create(
        SimulationId.generate(),
        principalBUserId,
        "Simulation Owned By B",
        periodId,
        List.of(programId));
    Simulation saved = saveSimulationPort.save(simOwnedByB);
    ownedSimulationId = saved.getId().value();
  }

  // -------------------------------------------------------------------------
  // T-WU2.1 — QAL-SEC-001-S1: cross-owner GET → 403
  // -------------------------------------------------------------------------

  /**
   * T-WU2.1: principal-A sends GET on principal-B's simulation.
   * Real GetSimulationService checks ownership → SimulationAccessDeniedException → 403.
   * The response body must contain application/problem+json with $.code.
   */
  @Test
  void cross_owner_get_simulation_returns_403_with_problem_json() throws Exception {
    mockMvc
        .perform(
            get("/me/simulations/{id}", ownedSimulationId)
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(principalASubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/problem+json")))
        .andExpect(jsonPath("$.code").value("SIMULATION_ACCESS_DENIED"));
  }

  // -------------------------------------------------------------------------
  // T-WU2.2 — QAL-SEC-001-S2: cross-owner PATCH → 403
  // -------------------------------------------------------------------------

  /**
   * T-WU2.2: principal-A sends PATCH on principal-B's simulation.
   * Real UpdateSimulationService checks ownership → SimulationAccessDeniedException → 403.
   */
  @Test
  void cross_owner_patch_simulation_returns_403_with_problem_json() throws Exception {
    mockMvc
        .perform(
            patch("/me/simulations/{id}", ownedSimulationId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(principalASubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Hijacked Name","version":0}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/problem+json")))
        .andExpect(jsonPath("$.code").value("SIMULATION_ACCESS_DENIED"));
  }

  // -------------------------------------------------------------------------
  // T-WU2.3 — QAL-SEC-001-S2: cross-owner DELETE → 403
  // -------------------------------------------------------------------------

  /**
   * T-WU2.3: principal-A sends DELETE on principal-B's simulation.
   * Real DeleteSimulationService checks ownership → SimulationAccessDeniedException → 403.
   */
  @Test
  void cross_owner_delete_simulation_returns_403_with_problem_json() throws Exception {
    mockMvc
        .perform(
            delete("/me/simulations/{id}", ownedSimulationId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(principalASubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/problem+json")))
        .andExpect(jsonPath("$.code").value("SIMULATION_ACCESS_DENIED"));
  }

  // -------------------------------------------------------------------------
  // T-WU2.5 — owner GET with missing catalog entry → 200 (graceful fallback)
  // -------------------------------------------------------------------------

  /**
   * T-WU2.5 (W-2 fix): principal-B (the owner) GETs their own simulation.
   *
   * <p>The simulation was seeded with a random {@code programId} that has NO entry in the
   * program catalog. Before the W-2 fix, {@code GetSimulationService} step 7 called
   * {@code programNameReadPort.programName(programId)} without a try-catch, which threw
   * {@code ProgramNotFoundException} → 500 for a legitimate owner whose referenced program
   * was absent from the catalog.
   *
   * <p>After the fix the call is wrapped in a try-catch that falls back to the programId string
   * (same pattern already used in {@code buildValidationSnapshot}). The owner must receive 200.
   *
   * <p>Locks the regression fix: if the catch is ever removed, this test goes RED.
   */
  @Test
  void owner_can_get_own_simulation_returns_200() throws Exception {
    // programId (seeded in @BeforeEach) has NO catalog entry — triggers the previously-uncaught
    // ProgramNotFoundException path in GetSimulationService step 7.
    mockMvc
        .perform(
            get("/me/simulations/{id}", ownedSimulationId)
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(principalBSubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // Cross-owner DELETE on already-deleted sim — extra robustness check
  // -------------------------------------------------------------------------

  /**
   * Extra: principal-A attempts DELETE after principal-A's own earlier attempt already failed.
   * Verifies that repeated attempts still return 403 (no state mutation from the failed attempt).
   *
   * <p>This exercises the simulation still being ACTIVE in the DB (not erroneously deleted
   * by principal-A's earlier BOLA attempt).
   */
  @Test
  void repeated_cross_owner_delete_still_returns_403() throws Exception {
    // First attempt
    mockMvc
        .perform(
            delete("/me/simulations/{id}", ownedSimulationId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(principalASubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isForbidden());

    // Second attempt — must still be 403 (simulation is NOT deleted by A's attempt)
    mockMvc
        .perform(
            delete("/me/simulations/{id}", ownedSimulationId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(principalASubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isForbidden());
  }
}
