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
import co.edu.uniquindio.brujula.simulation.application.dto.ProgramCreditSummaryView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationDetailView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationPageView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationScopeEntryDto;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationSelectionView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationValidationView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
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
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationStillBlockedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationValidationBlockedException;
import co.edu.uniquindio.brujula.simulation.domain.ValidationNotice;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
  // WU4a
  @MockitoBean private AddSimulationSelectionUseCase addSimulationSelectionUseCase;
  @MockitoBean private RemoveSimulationSelectionUseCase removeSimulationSelectionUseCase;
  @MockitoBean private ListSimulationSelectionsQuery listSimulationSelectionsQuery;
  // WU5
  @MockitoBean private ReviewSimulationUseCase reviewSimulationUseCase;

  private MockMvc mockMvc;
  private String studentSub;
  private UUID studentUserId;
  private String staffSub;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Provision a STUDENT user in DB (needed for resolveCallerId → getCurrentUserQuery)
    studentSub = "sub-sim-student-" + UUID.randomUUID();
    AppUser student =
        AppUser.provision(
            studentSub,
            "sim-student-" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Sim Student",
            Role.STUDENT);
    saveAppUserPort.save(student);
    studentUserId = student.id();

    // Provision a STAFF user (should be blocked by @PreAuthorize)
    staffSub = "sub-sim-staff-" + UUID.randomUUID();
    AppUser staff =
        AppUser.provision(
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

    SimulationView view =
        new SimulationView(
            simId,
            "Mi Sim 2026",
            periodId,
            "2026-2",
            List.of(programId),
            List.of(new SimulationScopeEntryDto(programId, "DIURNA")),
            "CURRENT",
            Instant.now(),
            Instant.now(),
            0L);

    when(createSimulationUseCase.createSimulation(any())).thenReturn(view);

    mockMvc
        .perform(
            post("/me/simulations")
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Mi Sim 2026","periodId":"%s","selections":[{"programId":"%s","journeyCode":"DIURNA"}]}
                    """
                        .formatted(periodId, programId)))
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
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                // No Idempotency-Key header
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Test","periodId":"%s","selections":[{"programId":"%s","journeyCode":"DIURNA"}]}
                    """
                        .formatted(periodId, programId)))
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

    SimulationView view =
        new SimulationView(
            simId,
            "Sim A",
            periodId,
            "2026-2",
            List.of(programId),
            List.of(new SimulationScopeEntryDto(programId, "DIURNA")),
            "CURRENT",
            Instant.now(),
            Instant.now(),
            0L);

    SimulationPageView pageView = new SimulationPageView(List.of(view), 0, 20, 1L, 1);

    when(listMySimulationsQuery.listMySimulations(any())).thenReturn(pageView);

    mockMvc
        .perform(
            get("/me/simulations")
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].id").value(simId.toString()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  // -------------------------------------------------------------------------
  // SIM-003: GET /me/simulations/{id} → 200 full detail (WU5)
  // -------------------------------------------------------------------------

  @Test
  void get_simulation_returns_200_full_detail() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    // Full detail view with all WU5 sub-objects populated
    ProgramCreditSummaryView cs =
        new ProgramCreditSummaryView(programId, "Systems Engineering", 12.0, 23, true);

    SimulationDetailView detailView =
        new SimulationDetailView(
            simId,
            "Sim Detail",
            periodId,
            "2026-2",
            List.of(programId),
            List.of(new SimulationScopeEntryDto(programId, "DIURNA")),
            "CURRENT",
            Instant.now(),
            Instant.now(),
            0L,
            List.of(), // selections empty
            List.of(cs), // creditSummary with real data
            List.of(), // weeklySchedule empty
            0.0, // totalWeeklyHours
            new SimulationValidationView(List.of(), List.of())); // validation clean

    when(getSimulationQuery.getSimulation(eq(simId), any())).thenReturn(detailView);

    mockMvc
        .perform(
            get("/me/simulations/{id}", simId)
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(simId.toString()))
        .andExpect(jsonPath("$.name").value("Sim Detail"))
        .andExpect(jsonPath("$.status").value("CURRENT"))
        .andExpect(jsonPath("$.creditSummary").isArray())
        .andExpect(jsonPath("$.creditSummary[0].programId").value(programId.toString()))
        .andExpect(jsonPath("$.creditSummary[0].programName").value("Systems Engineering"))
        .andExpect(jsonPath("$.creditSummary[0].selectedCredits").value(12.0))
        .andExpect(jsonPath("$.creditSummary[0].creditLimit").value(23))
        .andExpect(jsonPath("$.creditSummary[0].withinLimit").value(true))
        .andExpect(jsonPath("$.weeklySchedule").isArray())
        .andExpect(jsonPath("$.validation.blocking").isArray())
        .andExpect(jsonPath("$.validation.warnings").isArray());
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
                .with(
                    oidcLogin()
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
                .with(
                    oidcLogin()
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
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
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
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
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
                .with(
                    oidcLogin()
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
        .when(deleteSimulationUseCase)
        .deleteSimulation(eq(simId), any());

    mockMvc
        .perform(
            delete("/me/simulations/{id}", simId)
                .with(csrf())
                .with(
                    oidcLogin()
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
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(staffSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden());
  }

  // -------------------------------------------------------------------------
  // WU4a: addSimulationSelection → 201 (implemented in WU4a)
  // -------------------------------------------------------------------------

  @Test
  void add_selection_returns_201() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID scheduleOptionId = UUID.randomUUID();
    UUID selectionId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    SimulationSelectionView view =
        new SimulationSelectionView(
            selectionId,
            UUID.randomUUID(),
            scheduleOptionId,
            courseId,
            "Algorithms",
            programId,
            3.0,
            "A",
            "PUBLISHED",
            1L,
            List.of(),
            List.of());

    when(addSimulationSelectionUseCase.addSelection(any())).thenReturn(view);

    mockMvc
        .perform(
            post("/me/simulations/{id}/selections", simId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"scheduleOptionId":"%s"}
                    """
                        .formatted(scheduleOptionId)))
        .andExpect(status().isCreated())
        .andExpect(
            header().string("Location", "/me/simulations/" + simId + "/selections/" + selectionId))
        .andExpect(jsonPath("$.id").value(selectionId.toString()));
  }

  // -------------------------------------------------------------------------
  // WU4a: removeSimulationSelection → 204 (implemented in WU4a)
  // -------------------------------------------------------------------------

  @Test
  void remove_selection_returns_204() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID selectionId = UUID.randomUUID();

    doNothing()
        .when(removeSimulationSelectionUseCase)
        .removeSelection(eq(simId), eq(selectionId), any());

    mockMvc
        .perform(
            delete("/me/simulations/{id}/selections/{selId}", simId, selectionId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isNoContent());
  }

  // -------------------------------------------------------------------------
  // WU4a: listSimulationSelections → 200 (implemented in WU4a)
  // -------------------------------------------------------------------------

  @Test
  void list_selections_returns_200() throws Exception {
    UUID simId = UUID.randomUUID();

    when(listSimulationSelectionsQuery.listSelections(eq(simId), any())).thenReturn(List.of());

    mockMvc
        .perform(
            get("/me/simulations/{id}/selections", simId)
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // WARNING-1 fix: Real HTTP 409 problem+json for SIMULATION_SCHEDULE_CLASH
  // -------------------------------------------------------------------------

  /**
   * Asserts that POST /me/simulations/{id}/selections returns 409 application/problem+json with the
   * expected code, title, status, detail, and metadata.conflictingCourseName fields when the
   * selection service throws SimulationValidationBlockedException.
   *
   * <p>This test exercises the REAL exception handler (SimulationExceptionHandler) end-to-end
   * through the HTTP stack, closing the gap that the masked unit-level tests left (WARNING-1).
   */
  @Test
  void add_selection_scheduleClash_returns_409_problem_json() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID scheduleOptionId = UUID.randomUUID();

    ValidationNotice clashNotice =
        new ValidationNotice(
            "SIMULATION_SCHEDULE_CLASH",
            "Schedule clash detected with: Algorithms",
            Map.of(
                "conflictingCourseName",
                "Algorithms",
                "day",
                "MONDAY",
                "startsAt",
                "08:00",
                "endsAt",
                "10:00"));

    when(addSimulationSelectionUseCase.addSelection(any()))
        .thenThrow(new SimulationValidationBlockedException(List.of(clashNotice)));

    mockMvc
        .perform(
            post("/me/simulations/{id}/selections", simId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"scheduleOptionId":"%s"}
                    """
                        .formatted(scheduleOptionId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIMULATION_SCHEDULE_CLASH"))
        .andExpect(jsonPath("$.metadata.conflictingCourseName").value("Algorithms"))
        .andExpect(jsonPath("$.status").value(409));
  }

  /**
   * Asserts that POST /me/simulations/{id}/selections returns 409 application/problem+json with
   * code SIMULATION_CREDIT_LIMIT_EXCEEDED and metadata.programName present.
   */
  @Test
  void add_selection_creditLimitExceeded_returns_409_problem_json() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID scheduleOptionId = UUID.randomUUID();

    ValidationNotice creditNotice =
        new ValidationNotice(
            "SIMULATION_CREDIT_LIMIT_EXCEEDED",
            "Adding this course would exceed the credit limit for program: Systems Engineering",
            Map.of(
                "programName", "Systems Engineering", "attemptedCredits", 24.0, "creditLimit", 23));

    when(addSimulationSelectionUseCase.addSelection(any()))
        .thenThrow(new SimulationValidationBlockedException(List.of(creditNotice)));

    mockMvc
        .perform(
            post("/me/simulations/{id}/selections", simId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"scheduleOptionId":"%s"}
                    """
                        .formatted(scheduleOptionId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIMULATION_CREDIT_LIMIT_EXCEEDED"))
        .andExpect(jsonPath("$.metadata.programName").value("Systems Engineering"))
        .andExpect(jsonPath("$.status").value(409));
  }

  // -------------------------------------------------------------------------
  // FIX F4: HTTP 409 for WU4b code SIMULATION_DUPLICATE_SELECTION
  // -------------------------------------------------------------------------

  /**
   * FIX F4: Asserts that POST /me/simulations/{id}/selections returns 409 application/problem+json
   * with code SIMULATION_DUPLICATE_SELECTION when the use case throws
   * SimulationValidationBlockedException carrying that code (WU4b duplicate rule).
   *
   * <p>Mirrors the add_selection_scheduleClash_returns_409_problem_json pattern (WU4a), confirming
   * the exception handler maps WU4b codes correctly through the HTTP stack.
   */
  @Test
  void add_selection_duplicateSelection_returns_409_problem_json() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID scheduleOptionId = UUID.randomUUID();

    ValidationNotice duplicateNotice =
        new ValidationNotice(
            "SIMULATION_DUPLICATE_SELECTION",
            "This course is already in the simulation",
            Map.of("courseId", UUID.randomUUID().toString(), "courseName", "Data Structures"));

    when(addSimulationSelectionUseCase.addSelection(any()))
        .thenThrow(new SimulationValidationBlockedException(List.of(duplicateNotice)));

    mockMvc
        .perform(
            post("/me/simulations/{id}/selections", simId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"scheduleOptionId":"%s"}
                    """
                        .formatted(scheduleOptionId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIMULATION_DUPLICATE_SELECTION"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.metadata").exists());
  }

  // -------------------------------------------------------------------------
  // WU5: reviewSimulation → 200 CURRENT (clean, no blockers)
  // -------------------------------------------------------------------------

  @Test
  void review_simulation_clean_returns_200_current() throws Exception {
    UUID simId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    SimulationDetailView detailView =
        new SimulationDetailView(
            simId,
            "Reviewed Sim",
            periodId,
            "2026-2",
            List.of(programId),
            List.of(new SimulationScopeEntryDto(programId, "DIURNA")),
            "CURRENT",
            Instant.now(),
            Instant.now(),
            0L,
            List.of(),
            List.of(),
            List.of(),
            0.0,
            new SimulationValidationView(List.of(), List.of()));

    when(reviewSimulationUseCase.reviewSimulation(eq(simId), any())).thenReturn(detailView);

    mockMvc
        .perform(
            post("/me/simulations/{id}/review", simId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(simId.toString()))
        .andExpect(jsonPath("$.status").value("CURRENT"))
        .andExpect(jsonPath("$.validation.blocking").isArray());
  }

  // -------------------------------------------------------------------------
  // WU5: reviewSimulation → 409 when blockers persist (STALE-002)
  // -------------------------------------------------------------------------

  @Test
  void review_simulation_with_persistent_blocker_returns_409() throws Exception {
    UUID simId = UUID.randomUUID();

    ValidationNotice clashNotice =
        new ValidationNotice(
            "SIMULATION_SCHEDULE_CLASH",
            "Clash still exists after review",
            Map.of("day", "WEDNESDAY", "conflictingCourseName", "Databases"));

    when(reviewSimulationUseCase.reviewSimulation(eq(simId), any()))
        .thenThrow(new SimulationStillBlockedException(List.of(clashNotice)));

    mockMvc
        .perform(
            post("/me/simulations/{id}/review", simId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIMULATION_SCHEDULE_CLASH"))
        .andExpect(jsonPath("$.status").value(409));
  }

  // -------------------------------------------------------------------------
  // WU5: reviewSimulation non-owner → 403 (X-003)
  // -------------------------------------------------------------------------

  @Test
  void review_simulation_non_owner_returns_403() throws Exception {
    UUID simId = UUID.randomUUID();

    when(reviewSimulationUseCase.reviewSimulation(eq(simId), any()))
        .thenThrow(new SimulationAccessDeniedException(simId));

    mockMvc
        .perform(
            post("/me/simulations/{id}/review", simId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isForbidden());
  }
}
