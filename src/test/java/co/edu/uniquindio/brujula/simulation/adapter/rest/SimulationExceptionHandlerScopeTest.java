package co.edu.uniquindio.brujula.simulation.adapter.rest;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.simulation.application.port.in.AddSimulationSelectionUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.CreateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.DeleteSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.DuplicateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.GetSimulationQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.ListMySimulationsQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.ListSimulationSelectionsQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.RemoveSimulationSelectionUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.ReviewSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.UpdateSimulationUseCase;
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
 * Regression scope tests for {@code SimulationExceptionHandler}.
 *
 * <p>Asserts the per-module response contract for shared exceptions (status + {@code $.code}).
 * {@code basePackages} scoping is defence-in-depth; {@code @Order(HIGHEST_PRECEDENCE)} is the
 * primary correctness guarantee. These tests catch contract regressions (missing {@code $.code},
 * wrong status), not {@code basePackages} removal while handlers remain behaviorally identical.
 *
 * <p>Verifies simulation controller → {@code SimulationExceptionHandler} maps {@link
 * IdempotencyKeyConflictException} to HTTP 409 with {@code $.code = "IDEMPOTENCY_KEY_CONFLICT"}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SimulationExceptionHandlerScopeTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;

  @MockitoBean private CreateSimulationUseCase createSimulationUseCase;
  @MockitoBean private ListMySimulationsQuery listMySimulationsQuery;
  @MockitoBean private GetSimulationQuery getSimulationQuery;
  @MockitoBean private UpdateSimulationUseCase updateSimulationUseCase;
  @MockitoBean private DeleteSimulationUseCase deleteSimulationUseCase;
  @MockitoBean private DuplicateSimulationUseCase duplicateSimulationUseCase;
  @MockitoBean private AddSimulationSelectionUseCase addSimulationSelectionUseCase;
  @MockitoBean private RemoveSimulationSelectionUseCase removeSimulationSelectionUseCase;
  @MockitoBean private ListSimulationSelectionsQuery listSimulationSelectionsQuery;
  @MockitoBean private ReviewSimulationUseCase reviewSimulationUseCase;

  private MockMvc mockMvc;
  private String studentSub;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    studentSub = "sub-scope-sim-" + UUID.randomUUID();
    AppUser student =
        AppUser.provision(
            studentSub,
            "scope-sim-" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Scope Sim Student",
            Role.STUDENT);
    saveAppUserPort.save(student);
  }

  /**
   * Simulation controller throwing {@link IdempotencyKeyConflictException} MUST return HTTP 409
   * with {@code $.code = "IDEMPOTENCY_KEY_CONFLICT"}.
   *
   * <p>Asserting {@code $.code} makes this test load-bearing: it will fail if any handler in the
   * chain omits the {@code code} property, regardless of {@code basePackages} state.
   */
  @Test
  void simulation_controller_idempotency_conflict_returns_409_with_code() throws Exception {
    UUID periodId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID idemKey = UUID.randomUUID();

    when(createSimulationUseCase.createSimulation(any()))
        .thenThrow(new IdempotencyKeyConflictException(idemKey, "CREATE_SIMULATION"));

    mockMvc
        .perform(
            post("/me/simulations")
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", idemKey.toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Scope Test Sim","periodId":"%s","programIds":["%s"]}
                    """
                        .formatted(periodId, programId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code", is("IDEMPOTENCY_KEY_CONFLICT")));
  }

  /**
   * Anti-shadowing: verifies the full response contract for simulation-specific exceptions handled
   * by {@code SimulationExceptionHandler}.
   *
   * <p>Asserts HTTP 404 status for {@code SimulationNotFoundException}. This handler does not set
   * {@code $.code} for not-found responses (by design, no code property is needed per spec);
   * asserting on status is sufficient here to confirm contract.
   */
  @Test
  void simulation_controller_unknown_sim_uses_only_simulation_handler() throws Exception {
    UUID simId = UUID.randomUUID();

    when(getSimulationQuery.getSimulation(any(), any()))
        .thenThrow(
            new co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException(simId));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/me/simulations/{id}", simId)
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isNotFound());
  }
}
