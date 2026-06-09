package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.curriculum;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.dto.SequentialTrackView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListCourseRequirementsQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListSequentialTracksQuery;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.domain.PrereqRequirement;
import co.edu.uniquindio.brujula.simulation.domain.SequentialPredecessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Anti-corruption adapter: bridges simulation's {@link CurriculumReadPort} to
 * curriculum's published {@code curriculum::api} named interface (INV-001).
 *
 * <p>Maps {@code CourseRequirementView} to sim-local {@link PrereqRequirement} records and
 * resolves sequential-track predecessor info for VAL-008 (SIMULATION_SEQUENTIAL_ORDER).
 * The simulation domain never sees curriculum types (ARQ-002).
 */
@Component
class CurriculumReadAdapter implements CurriculumReadPort {

  private final ListCourseRequirementsQuery listCourseRequirementsQuery;
  private final GetCourseQuery getCourseQuery;
  private final ListSequentialTracksQuery listSequentialTracksQuery;

  CurriculumReadAdapter(
      ListCourseRequirementsQuery listCourseRequirementsQuery,
      GetCourseQuery getCourseQuery,
      ListSequentialTracksQuery listSequentialTracksQuery) {
    this.listCourseRequirementsQuery = listCourseRequirementsQuery;
    this.getCourseQuery = getCourseQuery;
    this.listSequentialTracksQuery = listSequentialTracksQuery;
  }

  @Override
  public List<PrereqRequirement> requirementsForCourse(UUID courseId) {
    List<CourseRequirementView> views = listCourseRequirementsQuery.listCourseRequirements(courseId);
    return views.stream()
        .filter(v -> "ACTIVE".equals(v.status()))
        .map(v -> new PrereqRequirement(
            v.targetCourseId(),
            v.requiredCourseId(),
            v.type(),
            v.confidence(),
            v.inferred(),
            resolveCourseName(v.requiredCourseId()),
            v.requiredCredits()))
        .toList();
  }

  @Override
  public int courseCredits(UUID courseId) {
    CourseView course = getCourseQuery.getCourse(courseId);
    return course.credits();
  }

  /**
   * Batch credits lookup via curriculum::api.GetCourseQuery.getCourses (QAL-PERF-002).
   *
   * <p>Issues a single batch read to curriculum instead of one call per course ID.
   * Course IDs not found in curriculum are absent from the returned map.
   */
  @Override
  public Map<UUID, Integer> coursesCredits(Set<UUID> courseIds) {
    if (courseIds == null || courseIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, CourseView> courseViews = getCourseQuery.getCourses(courseIds);
    Map<UUID, Integer> credits = new HashMap<>();
    for (Map.Entry<UUID, CourseView> entry : courseViews.entrySet()) {
      credits.put(entry.getKey(), entry.getValue().credits());
    }
    return credits;
  }

  @Override
  public SequentialPredecessor sequentialPredecessor(UUID courseId, UUID planId) {
    // Resolve course type; use planId from CourseView if not supplied
    String courseType;
    UUID resolvedPlanId = planId;
    try {
      CourseView course = getCourseQuery.getCourse(courseId);
      courseType = course != null ? course.type() : "REQUIRED";
      if (resolvedPlanId == null && course != null) {
        resolvedPlanId = course.planId();
      }
    } catch (Exception e) {
      // Unknown course — treat as REQUIRED (safe default, no sequential gate)
      courseType = "REQUIRED";
    }

    // Only DEEPENING and PERSONAL_ELECTIVE courses are subject to sequential ordering
    boolean isSequentialKind = "DEEPENING".equalsIgnoreCase(courseType)
        || "PERSONAL_ELECTIVE".equalsIgnoreCase(courseType);
    if (!isSequentialKind) {
      return new SequentialPredecessor(courseId, courseType, null, null);
    }

    if (resolvedPlanId == null) {
      // Cannot determine plan → no sequential gate can be enforced
      return new SequentialPredecessor(courseId, courseType, null, null);
    }

    // Find the sequential track for this planId that contains this courseId
    try {
      List<SequentialTrackView> tracks =
          listSequentialTracksQuery.listSequentialTracks(resolvedPlanId, null);
      for (SequentialTrackView track : tracks) {
        boolean containsCourse = track.levels().stream()
            .flatMap(level -> level.optionCourseIds().stream())
            .anyMatch(id -> id.equals(courseId));

        if (containsCourse && track.gatekeeperCourseId() != null) {
          // Track found and has a gatekeeper
          String gatekeeperName = resolveCourseName(track.gatekeeperCourseId());
          return new SequentialPredecessor(
              courseId, courseType, track.gatekeeperCourseId(), gatekeeperName);
        }
      }
    } catch (Exception e) {
      // Sequential track query failed → treat as no gate (fail-open for availability)
      return new SequentialPredecessor(courseId, courseType, null, null);
    }

    // Course is DEEPENING/PERSONAL_ELECTIVE but no gatekeeper defined for its track
    return new SequentialPredecessor(courseId, courseType, null, null);
  }

  private String resolveCourseName(UUID courseId) {
    if (courseId == null) {
      return null;
    }
    try {
      CourseView course = getCourseQuery.getCourse(courseId);
      return course != null ? course.name() : courseId.toString();
    } catch (Exception e) {
      // If course name resolution fails, fall back to UUID string
      return courseId.toString();
    }
  }
}
