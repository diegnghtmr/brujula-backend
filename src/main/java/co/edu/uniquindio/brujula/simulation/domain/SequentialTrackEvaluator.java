package co.edu.uniquindio.brujula.simulation.domain;

import java.util.Optional;

/**
 * Pure validation rule: blocks adding a PERSONAL_ELECTIVE or DEEPENING course when the student
 * has not passed the track's gatekeeper prerequisite (spec VAL-008 — SIMULATION_SEQUENTIAL_ORDER).
 *
 * <p>Sequential-track policy:
 * <ul>
 *   <li>Only DEEPENING and PERSONAL_ELECTIVE course types are evaluated.
 *   <li>A track gatekeeper ({@code predecessorCourseId}) is the course the student must have
 *       passed before enrolling in level-1 of the track.
 *   <li>If the course has no track or the track has no gatekeeper → rule does not fire.
 *   <li>If the gatekeeper is NOT passed → BLOCKING SIMULATION_SEQUENTIAL_ORDER.
 *   <li>REQUIRED / INSTITUTIONAL / other types → rule is not evaluated.
 * </ul>
 *
 * <p>Package-private — only used by {@link SimulationValidationService}.
 * Pure Java, no Spring/JPA (ARQ-002, ADR-F4-004).
 */
class SequentialTrackEvaluator {

  /**
   * Returns a blocking notice when the sequential ordering constraint is violated.
   *
   * @param candidate    the selection being added
   * @param predecessor  the sequential-track predecessor information resolved from curriculum
   * @param passed       the set of courses the student has effectively passed
   * @return optional blocking notice with code {@code SIMULATION_SEQUENTIAL_ORDER}
   */
  Optional<ValidationNotice> evaluate(
      CandidateSelection candidate,
      SequentialPredecessor predecessor,
      PassedCourseSet passed) {

    // Only evaluate DEEPENING and PERSONAL_ELECTIVE course types
    if (!predecessor.isSequentialTrackKind()) {
      return Optional.empty();
    }

    // If the track has no gatekeeper defined, ordering constraint is not enforced
    if (!predecessor.hasPredecessor()) {
      return Optional.empty();
    }

    // Check whether the student has passed the gatekeeper
    boolean gatekeeperPassed = passed.hasPassed(predecessor.predecessorCourseId());
    if (gatekeeperPassed) {
      return Optional.empty();
    }

    // Gatekeeper not passed → blocking
    String predecessorName = predecessor.predecessorCourseName() != null
        ? predecessor.predecessorCourseName()
        : predecessor.predecessorCourseId().toString();

    return Optional.of(new ValidationNotice(
        "SIMULATION_SEQUENTIAL_ORDER",
        "Sequential track ordering not met: must pass '" + predecessorName
            + "' before adding '" + candidate.courseName() + "'"));
  }
}
