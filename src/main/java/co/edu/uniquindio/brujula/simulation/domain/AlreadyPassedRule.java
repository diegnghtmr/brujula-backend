package co.edu.uniquindio.brujula.simulation.domain;

import java.util.Optional;

/**
 * Pure validation rule: blocks adding a course the student has already passed (spec VAL-005).
 *
 * <p>Code: {@code SIMULATION_COURSE_ALREADY_PASSED}.
 *
 * <p>Package-private — only used by {@link SimulationValidationService}. Pure Java, no Spring/JPA
 * (ADR-F4-004).
 */
class AlreadyPassedRule {

  /**
   * Returns a blocking notice if the candidate course is already in the student's passed set.
   *
   * @param candidate the selection being added
   * @param passed the set of courses the student has effectively passed
   * @return optional blocking notice with code {@code SIMULATION_COURSE_ALREADY_PASSED}
   */
  Optional<ValidationNotice> evaluate(CandidateSelection candidate, PassedCourseSet passed) {
    if (passed.hasPassed(candidate.courseId())) {
      return Optional.of(
          new ValidationNotice(
              "SIMULATION_COURSE_ALREADY_PASSED",
              "Course already passed: " + candidate.courseName()));
    }
    return Optional.empty();
  }
}
