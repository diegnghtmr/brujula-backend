package co.edu.uniquindio.brujula.simulation.application.port.out;

import co.edu.uniquindio.brujula.simulation.domain.PrereqRequirement;
import co.edu.uniquindio.brujula.simulation.domain.SequentialPredecessor;

import java.util.List;
import java.util.UUID;

/**
 * Output port: read prerequisite requirements, course credits, and sequential-track ordering
 * from the curriculum module.
 *
 * <p>Delegates to {@code curriculum::api.ListCourseRequirementsQuery},
 * {@code curriculum::api.GetCourseQuery}, and {@code curriculum::api.ListSequentialTracksQuery}.
 * Maps curriculum DTOs to sim-local records, keeping the simulation domain boundary pure
 * (ARQ-002, INV-001).
 *
 * <p>Named by need (ARQ-001). Implemented by {@code CurriculumReadAdapter}.
 */
public interface CurriculumReadPort {

  /**
   * Return the prerequisite requirements for a course.
   *
   * <p>Includes all requirement types (COURSE_PASSED, MINIMUM_CREDITS, SEQUENCE, CONFIGURABLE)
   * so each evaluator can filter the types it handles explicitly.
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
   * Return the sequential-track predecessor information for a course.
   *
   * <p>Resolves: course type (DEEPENING, PERSONAL_ELECTIVE, REQUIRED, etc.) and the
   * gatekeeper course UUID from the track (if any) via curriculum::api.ListSequentialTracksQuery
   * and GetCourseQuery.
   *
   * <p>If the course is not part of a sequential track or its track has no gatekeeper,
   * the returned {@link SequentialPredecessor} will have {@code predecessorCourseId = null}.
   *
   * @param courseId the course UUID
   * @param planId   the study plan UUID (needed to query sequential tracks)
   * @return the sequential predecessor info for this course
   */
  SequentialPredecessor sequentialPredecessor(UUID courseId, UUID planId);
}
