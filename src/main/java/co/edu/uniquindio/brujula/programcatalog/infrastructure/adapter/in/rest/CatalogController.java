package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.CatalogApi;
import co.edu.uniquindio.brujula.generated.model.Journey;
import co.edu.uniquindio.brujula.generated.model.PlanStatus;
import co.edu.uniquindio.brujula.generated.model.Program;
import co.edu.uniquindio.brujula.generated.model.ProgramPage;
import co.edu.uniquindio.brujula.generated.model.ProgramStatus;
import co.edu.uniquindio.brujula.generated.model.StudyPlan;
import co.edu.uniquindio.brujula.programcatalog.application.dto.JourneyRef;
import co.edu.uniquindio.brujula.programcatalog.application.dto.PagedResult;
import co.edu.uniquindio.brujula.programcatalog.application.dto.ProgramView;
import co.edu.uniquindio.brujula.programcatalog.application.dto.StudyPlanView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetProgramQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetStudyPlanQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListJourneysQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListProgramPlansQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListProgramsQuery;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter implementing the generated {@link CatalogApi} interface (contract-first, ARQ-001).
 *
 * <p>Implements the read-only catalog endpoints for Phase 3 WU4:
 *
 * <ul>
 *   <li>GET /programs — listPrograms (CAT-001)
 *   <li>GET /programs/{programId} — getProgram (CAT-001)
 *   <li>GET /programs/{programId}/plans — listProgramPlans (CAT-002)
 *   <li>GET /plans/{planId} — getStudyPlan (CAT-002)
 *   <li>GET /journeys — listJourneys (CAT-003)
 * </ul>
 *
 * <p>ARQ-001: no domain types imported — only application DTOs and generated model types.
 * Package-private per ARQ-006.
 */
@RestController
class CatalogController implements CatalogApi {

  private final ListProgramsQuery listProgramsQuery;
  private final GetProgramQuery getProgramQuery;
  private final ListProgramPlansQuery listProgramPlansQuery;
  private final GetStudyPlanQuery getStudyPlanQuery;
  private final ListJourneysQuery listJourneysQuery;

  CatalogController(
      ListProgramsQuery listProgramsQuery,
      GetProgramQuery getProgramQuery,
      ListProgramPlansQuery listProgramPlansQuery,
      GetStudyPlanQuery getStudyPlanQuery,
      ListJourneysQuery listJourneysQuery) {
    this.listProgramsQuery = listProgramsQuery;
    this.getProgramQuery = getProgramQuery;
    this.listProgramPlansQuery = listProgramPlansQuery;
    this.getStudyPlanQuery = getStudyPlanQuery;
    this.listJourneysQuery = listJourneysQuery;
  }

  // ---------------------------------------------------------------------------
  // CatalogApi implementations
  // ---------------------------------------------------------------------------

  /** CAT-001: list programs with optional status filter and pagination. */
  @Override
  public ResponseEntity<ProgramPage> listPrograms(
      @Nullable ProgramStatus status, Integer page, Integer size, @Nullable List<String> sort) {
    String statusFilter = status != null ? status.getValue() : null;
    PagedResult<ProgramView> result = listProgramsQuery.listPrograms(statusFilter, page, size);
    ProgramPage programPage = toProgramPage(result);
    return ResponseEntity.ok(programPage);
  }

  /** CAT-001: get a single program by ID. */
  @Override
  public ResponseEntity<Program> getProgram(UUID programId) {
    ProgramView view = getProgramQuery.getProgram(programId);
    return ResponseEntity.ok(toProgramDto(view));
  }

  /** CAT-002: list study plans for a program (with role-based visibility). */
  @Override
  public ResponseEntity<List<StudyPlan>> listProgramPlans(
      UUID programId, @Nullable PlanStatus status) {
    String callerRole = resolveCallerRole();
    List<StudyPlanView> views = listProgramPlansQuery.listProgramPlans(programId, callerRole);
    List<StudyPlan> result = views.stream().map(this::toStudyPlanDto).toList();
    return ResponseEntity.ok(result);
  }

  /** CAT-002: get a single study plan by ID (with role-based visibility). */
  @Override
  public ResponseEntity<StudyPlan> getStudyPlan(UUID planId) {
    String callerRole = resolveCallerRole();
    StudyPlanView view = getStudyPlanQuery.getStudyPlan(planId, callerRole);
    return ResponseEntity.ok(toStudyPlanDto(view));
  }

  /** CAT-003: list all active journeys. */
  @Override
  public ResponseEntity<List<Journey>> listJourneys() {
    List<JourneyRef> refs = listJourneysQuery.listJourneys();
    List<Journey> journeys = refs.stream().map(this::toJourneyDto).toList();
    return ResponseEntity.ok(journeys);
  }

  // ---------------------------------------------------------------------------
  // DTO mappers: application DTOs → generated model (ARQ-005)
  // ---------------------------------------------------------------------------

  private ProgramPage toProgramPage(PagedResult<ProgramView> result) {
    List<Program> programs = result.content().stream().map(this::toProgramDto).toList();
    ProgramPage page = new ProgramPage();
    page.setPage(result.page());
    page.setSize(result.size());
    page.setTotalElements(result.totalElements());
    page.setTotalPages(result.totalPages());
    page.setContent(programs);
    return page;
  }

  private Program toProgramDto(ProgramView view) {
    Program program = new Program();
    program.setId(view.id());
    program.setName(view.name());
    program.setFaculty(view.faculty());
    program.setStatus(ProgramStatus.fromValue(view.status()));
    program.setVersion(view.version());
    // availableJourneys: populated via separate listJourneys query (not included in ProgramView)
    program.setAvailableJourneys(List.of());
    return program;
  }

  private StudyPlan toStudyPlanDto(StudyPlanView view) {
    StudyPlan plan = new StudyPlan();
    plan.setId(view.id());
    plan.setProgramId(view.programId());
    plan.setName(view.name());
    plan.setStatus(PlanStatus.fromValue(view.status()));
    plan.setTotalCredits(view.totalCredits());
    plan.setReferenceYear(view.referenceYear());
    plan.setVersion(view.version());
    // applicableJourneys: not loaded in ProgramView scope
    plan.setApplicableJourneys(List.of());
    return plan;
  }

  private Journey toJourneyDto(JourneyRef ref) {
    Journey journey = new Journey();
    journey.setId(ref.id());
    journey.setCode(ref.code());
    journey.setName(ref.name());
    journey.setActive(true); // listJourneys returns only active ones (CAT-003)
    return journey;
  }

  // ---------------------------------------------------------------------------
  // Security helpers
  // ---------------------------------------------------------------------------

  /**
   * Extract the caller's highest-privilege role from the Spring Security context.
   *
   * <p>Role strings follow the {@code ROLE_} prefix convention. Returns the role without the prefix
   * (e.g. "STAFF" for {@code ROLE_STAFF}). Defaults to {@code null} (student-level visibility) if
   * no principal or no recognized role is found.
   */
  private String resolveCallerRole() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return null;
    }
    // ALLOWLIST: privileged roles take precedence
    for (GrantedAuthority authority : auth.getAuthorities()) {
      String a = authority.getAuthority();
      if ("ROLE_ADMIN".equals(a)) return "ADMIN";
      if ("ROLE_STAFF".equals(a)) return "STAFF";
    }
    return "STUDENT";
  }
}
