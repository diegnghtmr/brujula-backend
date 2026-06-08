package co.edu.uniquindio.brujula.simulation.application.port.out;

import co.edu.uniquindio.brujula.simulation.domain.PrereqRequirement;

import java.util.List;
import java.util.UUID;

/**
 * Output port: read prerequisite requirements and course credits from the curriculum module.
 *
 * <p>Delegates to {@code curriculum::api.ListCourseRequirementsQuery} and
 * {@code curriculum::api.GetCourseQuery}. Maps the {@code CourseRequirementView} to sim-local
 * {@link PrereqRequirement} records, keeping the simulation domain boundary pure (ARQ-002, INV-001).
 *
 * <p>Named by need (ARQ-001). Implemented by {@code CurriculumReadAdapter}.
 */
public interface CurriculumReadPort {

  /**
   * Return the prerequisite requirements for a course.
   *
   * <p>Only returns ACTIVE, COURSE_PASSED type requirements in WU4a. Other types
   * (MINIMUM_CREDITS, SEQUENCE, CONFIGURABLE) are included in the list so the evaluator
   * can skip them explicitly (making the filtering intent clear).
   *
   * @param courseId the target course UUID
   * @return list of prerequisite requirement edges; may be empty
   */
  List<PrereqRequirement> requirementsForCourse(UUID courseId);

  /**
   * Return the number of credits for a course.
   *
   * <p>Delegates to {@code curriculum::api.GetCourseQuery.getCourse(courseId).credits()}.
   * Used by {@code AddSimulationSelectionService} to build a {@code CandidateSelection} with
   * real credits so the credit-limit rule (VAL-004) actually fires.
   *
   * @param courseId the course UUID
   * @return the credit count (≥ 0)
   * @throws co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException if not found
   */
  int courseCredits(UUID courseId);
}
