package co.edu.uniquindio.brujula.simulation.application.port.out;

import co.edu.uniquindio.brujula.simulation.domain.PrereqRequirement;
import co.edu.uniquindio.brujula.simulation.domain.SequentialPredecessor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Output port: read prerequisite requirements, course credits, and sequential-track ordering from
 * the curriculum module.
 *
 * <p>Delegates to {@code curriculum::api.ListCourseRequirementsQuery}, {@code
 * curriculum::api.GetCourseQuery}, and {@code curriculum::api.ListSequentialTracksQuery}. Maps
 * curriculum DTOs to sim-local records, keeping the simulation domain boundary pure.
 *
 * <p>Named by need. Implemented by {@code CurriculumReadAdapter}.
 */
public interface CurriculumReadPort {

  /**
   * Return the prerequisite requirements for a course.
   *
   * <p>Includes all requirement types (COURSE_PASSED, MINIMUM_CREDITS, SEQUENCE, CONFIGURABLE) so
   * each evaluator can filter the types it handles explicitly.
   *
   * @param courseId the target course UUID
   * @return list of prerequisite requirement edges; may be empty
   */
  List<PrereqRequirement> requirementsForCourse(UUID courseId);

  /**
   * Return the number of credits for a course.
   *
   * <p>Delegates to {@code curriculum::api.GetCourseQuery.getCourse(courseId).credits()}.
   *
   * @param courseId the course UUID
   * @return the credit count (≥ 0)
   * @throws co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException if not found
   */
  int courseCredits(UUID courseId);

  /**
   * Return credits for a set of course IDs in a single batch call (QAL-PERF-002).
   *
   * <p>Replaces the per-course {@link #courseCredits(UUID)} loop in {@code
   * GetSimulationService.buildValidationSnapshot} with a single batch read, reducing N courses from
   * N round-trips to exactly 1.
   *
   * <p>Missing course IDs (not found in curriculum) are absent from the returned map. Callers MUST
   * treat a missing entry as 0 credits or use a default as appropriate.
   *
   * @param courseIds the course UUIDs to resolve; must not be null
   * @return map from courseId to credit count for all found courses
   */
  Map<UUID, Integer> coursesCredits(Set<UUID> courseIds);

  /**
   * Return the sequential-track predecessor information for a course.
   *
   * <p>Resolves: course type (DEEPENING, PERSONAL_ELECTIVE, REQUIRED, etc.) and the gatekeeper
   * course UUID from the track (if any) via curriculum::api.ListSequentialTracksQuery and
   * GetCourseQuery.
   *
   * <p>If the course is not part of a sequential track or its track has no gatekeeper, the returned
   * {@link SequentialPredecessor} will have {@code predecessorCourseId = null}.
   *
   * @param courseId the course UUID
   * @param planId the study plan UUID (needed to query sequential tracks)
   * @return the sequential predecessor info for this course
   */
  SequentialPredecessor sequentialPredecessor(UUID courseId, UUID planId);
}
