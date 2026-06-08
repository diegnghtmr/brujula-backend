package co.edu.uniquindio.brujula.simulation.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationPageView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.port.in.CreateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.DeleteSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.DuplicateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.in.GetSimulationQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.ListMySimulationsQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.UpdateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * WU3 REST slice tests for simulation endpoints.
 *
 * <p>Uses {@code @SpringBootTest} + real security chain (same pattern as {@code
 * SubscriptionControllerTest}). Application-layer ports are mocked via {@code @MockitoBean}.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>SIM-001: POST /me/simulations → 201 + Location header
 *   <li>SIM-002: GET /me/simulations → 200 + page structure
 *   <li>SIM-003: GET /me/simulations/{id} → 200 skeleton detail
 *   <li>SIM-004: PATCH /me/simulations/{id} stale version → 409
 *   <li>SIM-005: DELETE /me/simulations/{id} → 204
 *   <li>X-003: non-owner GET → 403
 *   <li>X-003: non-owner DELETE → 403
 *   <li>SIM-004: unknown simulation → 404
 *   <li>WU4/WU5 stubs: addSelection, removeSelection, listSelections, review → 501
 * </ul>
 *
 * <p>Satisfies: SIM-001..006, X-001..004, INV-004. ARQ-001.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SimulationControllerSliceTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;

  @MockitoBean private CreateSimulationUseCase createSimulationUseCase;
  @MockitoBean private ListMySimulationsQuery listMySimulationsQuery;
  @MockitoBean private GetSimulationQuery getSimulationQuery;
  @MockitoBean private UpdateSimulationUseCase updateSimulationUseCase;
  @MockitoBean private DeleteSimulationUseCase deleteSimulationUseCase;
  @MockitoBean private DuplicateSimulationUseCase duplicateSimulationUseCase;

  private MockMvc mockMvc;
  private String studentSub;
  private UUID studentUserId;
  private String staffSub;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Provision a STUDENT user in DB (needed for resolveCallerId → getCurrentUserQuery)
    studentSub = "sub-sim-student-" + UUID.randomUUID();
    AppUser student = AppUser.provision(
        studentSub,
        "sim-student-" + UUID.randomUUID() + "@uniquindio.edu.co",
        "Sim Student",
        Role.STUDENT);
    saveAppUserPort.save(student);
    studentUserId = student.id();

    // Provision a STAFF user (should be blocked by @PreAuthorize)
    staffSub = "sub-sim-staff-" + UUID.randomUUID();
    AppUser staff = AppUser.provision(
        staffSub,
        "sim-staff-" + UUID.randomUUID() + "@uniquindio.edu.co",
        "Sim Staff",
        Role.STAFF);
    saveAppUserPort.save(staff);
  }

  // -------------------------------------------------------------------------
  // SIM-001: POST /me/simulations → 201 + Location header
  // -------------------------------------------------------------------------

  @Test
  void create_simulation_returns_201_with_location() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    SimulationView view = new SimulationView(
        simId, "Mi Sim 2026", periodId, "2026-2",
        List.of(programId), "CURRENT", Instant.now(), Instant.now(), 0L);

    when(createSimulationUseCase.createSimulation(any())).thenReturn(view);

    mockMvc
        .perform(
            post("/me/simulations")
                .with(csrf())
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Mi Sim 2026","periodId":"%s","programIds":["%s"]}
                    """.formatted(periodId, programId)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/me/simulations/" + simId))
        .andExpect(jsonPath("$.id").value(simId.toString()))
        .andExpect(jsonPath("$.name").value("Mi Sim 2026"));
  }

  // -------------------------------------------------------------------------
  // SIM-001: POST without Idempotency-Key → 400
  // -------------------------------------------------------------------------

  @Test
  void create_simulation_without_idempotency_key_returns_400() throws Exception {
    UUID periodId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/me/simulations")
                .with(csrf())
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                // No Idempotency-Key header
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Test","periodId":"%s","programIds":["%s"]}
                    """.formatted(periodId, programId)))
        .andExpect(status().isBadRequest());
  }

  // -------------------------------------------------------------------------
  // SIM-002: GET /me/simulations → 200 + page
  // -------------------------------------------------------------------------

  @Test
  void list_simulations_returns_200_with_page() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    SimulationView view = new SimulationView(
        simId, "Sim A", periodId, "2026-2",
        List.of(programId), "CURRENT", Instant.now(), Instant.now(), 0L);

    SimulationPageView pageView = new SimulationPageView(List.of(view), 0, 20, 1L, 1);

    when(listMySimulationsQuery.listMySimulations(any())).thenReturn(pageView);

    mockMvc
        .perform(
            get("/me/simulations")
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].id").value(simId.toString()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  // -------------------------------------------------------------------------
  // SIM-003: GET /me/simulations/{id} → 200 skeleton detail
  // -------------------------------------------------------------------------

  @Test
  void get_simulation_returns_200_skeleton_detail() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    SimulationView view = new SimulationView(
        simId, "Sim Detail", periodId, "2026-2",
        List.of(programId), "CURRENT", Instant.now(), Instant.now(), 0L);

    when(getSimulationQuery.getSimulation(eq(simId), any())).thenReturn(view);

    mockMvc
        .perform(
            get("/me/simulations/{id}", simId)
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(simId.toString()))
        .andExpect(jsonPath("$.name").value("Sim Detail"))
        .andExpect(jsonPath("$.status").value("CURRENT"));
  }

  // -------------------------------------------------------------------------
  // X-003: GET non-owner → 403
  // -------------------------------------------------------------------------

  @Test
  void get_simulation_non_owner_returns_403() throws Exception {
    UUID simId = UUID.randomUUID();

    when(getSimulationQuery.getSimulation(eq(simId), any()))
        .thenThrow(new SimulationAccessDeniedException(simId));

    mockMvc
        .perform(
            get("/me/simulations/{id}", simId)
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  // -------------------------------------------------------------------------
  // GET unknown simulation → 404
  // -------------------------------------------------------------------------

  @Test
  void get_unknown_simulation_returns_404() throws Exception {
    UUID unknownId = UUID.randomUUID();

    when(getSimulationQuery.getSimulation(eq(unknownId), any()))
        .thenThrow(new SimulationNotFoundException(unknownId));

    mockMvc
        .perform(
            get("/me/simulations/{id}", unknownId)
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isNotFound());
  }

  // -------------------------------------------------------------------------
  // SIM-004: PATCH stale version → 409
  // -------------------------------------------------------------------------

  @Test
  void patch_simulation_stale_version_returns_409() throws Exception {
    UUID simId = UUID.randomUUID();

    when(updateSimulationUseCase.updateSimulation(any()))
        .thenThrow(new OptimisticLockingFailureException("Stale version"));

    mockMvc
        .perform(
            patch("/me/simulations/{id}", simId)
                .with(csrf())
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Updated","version":0}
                    """))
        .andExpect(status().isConflict());
  }

  // -------------------------------------------------------------------------
  // X-003: PATCH non-owner → 403
  // -------------------------------------------------------------------------

  @Test
  void patch_simulation_non_owner_returns_403() throws Exception {
    UUID simId = UUID.randomUUID();

    when(updateSimulationUseCase.updateSimulation(any()))
        .thenThrow(new SimulationAccessDeniedException(simId));

    mockMvc
        .perform(
            patch("/me/simulations/{id}", simId)
                .with(csrf())
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Hijack","version":0}
                    """))
        .andExpect(status().isForbidden());
  }

  // -------------------------------------------------------------------------
  // SIM-005: DELETE /me/simulations/{id} → 204
  // -------------------------------------------------------------------------

  @Test
  void delete_simulation_returns_204() throws Exception {
    UUID simId = UUID.randomUUID();

    doNothing().when(deleteSimulationUseCase).deleteSimulation(eq(simId), any());

    mockMvc
        .perform(
            delete("/me/simulations/{id}", simId)
                .with(csrf())
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isNoContent());
  }

  // -------------------------------------------------------------------------
  // X-003: DELETE non-owner → 403
  // -------------------------------------------------------------------------

  @Test
  void delete_simulation_non_owner_returns_403() throws Exception {
    UUID simId = UUID.randomUUID();

    doThrow(new SimulationAccessDeniedException(simId))
        .when(deleteSimulationUseCase).deleteSimulation(eq(simId), any());

    mockMvc
        .perform(
            delete("/me/simulations/{id}", simId)
                .with(csrf())
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isForbidden());
  }

  // -------------------------------------------------------------------------
  // STAFF role on /me/** → 403
  // -------------------------------------------------------------------------

  @Test
  void staff_get_simulations_returns_403() throws Exception {
    mockMvc
        .perform(
            get("/me/simulations")
                .with(oidcLogin()
                    .idToken(t -> t.subject(staffSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden());
  }

  // -------------------------------------------------------------------------
  // WU4 stub: addSimulationSelection → 501
  // -------------------------------------------------------------------------

  @Test
  void add_selection_returns_501() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID offeringId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/me/simulations/{id}/selections", simId)
                .with(csrf())
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"scheduleOptionId":"%s"}
                    """.formatted(offeringId)))
        .andExpect(status().isNotImplemented());
  }

  // -------------------------------------------------------------------------
  // WU4 stub: removeSimulationSelection → 501
  // -------------------------------------------------------------------------

  @Test
  void remove_selection_returns_501() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID selectionId = UUID.randomUUID();

    mockMvc
        .perform(
            delete("/me/simulations/{id}/selections/{selId}", simId, selectionId)
                .with(csrf())
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isNotImplemented());
  }

  // -------------------------------------------------------------------------
  // WU4 stub: listSimulationSelections → 501
  // -------------------------------------------------------------------------

  @Test
  void list_selections_returns_501() throws Exception {
    UUID simId = UUID.randomUUID();

    mockMvc
        .perform(
            get("/me/simulations/{id}/selections", simId)
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isNotImplemented());
  }

  // -------------------------------------------------------------------------
  // WU5 stub: reviewSimulation → 501
  // -------------------------------------------------------------------------

  @Test
  void review_simulation_returns_501() throws Exception {
    UUID simId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/me/simulations/{id}/review", simId)
                .with(csrf())
                .with(oidcLogin()
                    .idToken(t -> t.subject(studentSub))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isNotImplemented());
  }
}
