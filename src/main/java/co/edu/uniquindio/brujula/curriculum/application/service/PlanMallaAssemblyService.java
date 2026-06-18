package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.curriculum.application.dto.PlanMallaView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.RecommendedSemester;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case implementation: assemble the plan-level malla (curricular grid).
 *
 * <p>Loads all courses for the plan in one indexed query, filters to ACTIVE status, then groups by
 * recommended semester for the requested journey code — entirely in-memory. Requirement edges are
 * bulk-loaded via a single {@link LoadCourseRequirementPort#loadByTargetCourseIds} call (no N+1 per
 * course) and attached to each course summary.
 *
 * <p>A static advisory {@code graduationRequirements} string is included in every response —
 * informational copy only; no graduation_gate entity is modelled.
 *
 * <p>No Spring annotations — wiring is handled in the infrastructure configuration layer.
 */
public class PlanMallaAssemblyService implements AssemblePlanMallaQuery {

  /**
   * Static graduation advisory text included in every malla response.
   *
   * <p>Applies to all programs: both Sistemas and Lenguas share the same two graduation gates
   * (suficiencia en lengua extranjera + Cátedra Multidisciplinar) according to Universidad del
   * Quindío curricular regulations. User-facing copy in neutral professional Spanish.
   */
  static final String GRADUATION_REQUIREMENTS_ADVISORY =
      "Para optar al título de grado se requiere acreditar suficiencia en lengua extranjera "
          + "(inglés u otro idioma reconocido por la Universidad del Quindío) y aprobar la "
          + "Cátedra Multidisciplinar. Consulte con la coordinación académica de su programa "
          + "para verificar los requisitos vigentes.";

  private final LoadCoursePort loadCoursePort;
  private final LoadCourseRequirementPort loadCourseRequirementPort;

  public PlanMallaAssemblyService(
      LoadCoursePort loadCoursePort, LoadCourseRequirementPort loadCourseRequirementPort) {
    this.loadCoursePort = loadCoursePort;
    this.loadCourseRequirementPort = loadCourseRequirementPort;
  }

  @Override
  public PlanMallaView assemblePlanMalla(UUID planId, String journeyCode) {
    List<Course> allCourses = loadCoursePort.loadByPlanId(planId);

    // Collect ACTIVE courses that have a semester recommendation for the requested journey
    List<Course> activeCourses =
        allCourses.stream()
            .filter(c -> c.status() == CourseStatus.ACTIVE)
            .filter(
                c ->
                    c.recommendedSemesters().stream()
                        .anyMatch(rs -> journeyCode.equals(rs.journeyCode())))
            .toList();

    // Bulk-load all requirement edges for the active courses — single query, no N+1
    List<UUID> activeCourseIds = activeCourses.stream().map(c -> c.id().value()).toList();
    List<CourseRequirement> allRequirements =
        loadCourseRequirementPort.loadByTargetCourseIds(activeCourseIds);

    // Index requirements by target course ID for O(1) lookup
    Map<UUID, List<CourseRequirement>> requirementsByCourseId =
        allRequirements.stream().collect(Collectors.groupingBy(CourseRequirement::targetCourseId));

    // Group active courses by their recommended semester for the requested journey
    Map<Integer, List<PlanMallaView.CourseSummary>> bySemester = new TreeMap<>();
    for (Course course : activeCourses) {
      course.recommendedSemesters().stream()
          .filter(rs -> journeyCode.equals(rs.journeyCode()))
          .map(RecommendedSemester::semester)
          .findFirst()
          .ifPresent(
              semester -> {
                List<CourseRequirement> courseReqs =
                    requirementsByCourseId.getOrDefault(course.id().value(), List.of());
                bySemester
                    .computeIfAbsent(semester, k -> new ArrayList<>())
                    .add(toCourseSummary(course, courseReqs));
              });
    }

    List<PlanMallaView.SemesterBucket> buckets =
        bySemester.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new PlanMallaView.SemesterBucket(e.getKey(), List.copyOf(e.getValue())))
            .toList();

    return new PlanMallaView(planId, journeyCode, buckets, GRADUATION_REQUIREMENTS_ADVISORY);
  }

  // ---------------------------------------------------------------------------
  // Domain → DTO mapper
  // ---------------------------------------------------------------------------

  private PlanMallaView.CourseSummary toCourseSummary(
      Course course, List<CourseRequirement> requirements) {
    List<PlanMallaView.RequirementSummary> reqSummaries =
        requirements.stream().map(this::toRequirementSummary).toList();
    return new PlanMallaView.CourseSummary(
        course.id().value(),
        course.officialCode(),
        course.name(),
        course.type().name(),
        course.credits(),
        reqSummaries);
  }

  private PlanMallaView.RequirementSummary toRequirementSummary(CourseRequirement req) {
    return new PlanMallaView.RequirementSummary(
        req.type().name(),
        req.requiredCourseId(),
        req.confidence() != null ? req.confidence().name() : null,
        req.inferred(),
        req.simultaneityAllowed());
  }
}
