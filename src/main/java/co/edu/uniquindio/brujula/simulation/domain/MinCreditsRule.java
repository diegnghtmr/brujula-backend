package co.edu.uniquindio.brujula.simulation.domain;

import java.util.List;
import java.util.Optional;

/**
 * Pure validation rule: blocks adding a course when a per-course minimum-credits prerequisite is
 * not met (SIMULATION_MIN_CREDITS_NOT_MET).
 *
 * <p>Scope: per-course prerequisite MINIMUM_CREDITS edges only (spec VAL-008, design
 * MinCreditsRule). NOT the graduation MINIMUM_CREDITS gate (DEBT-F4-002 — kept out of scope).
 *
 * <p>Rule: for each MINIMUM_CREDITS requirement edge associated with the candidate course, if the
 * student's total accumulated passed credits (across all programs) is less than the required
 * minimum, the selection is blocked.
 *
 * <p>Data gap note: As of WU4b, the seed data in V012/V013 contains COURSE_PASSED and SEQUENCE
 * prerequisite types but NO MINIMUM_CREDITS edges for individual courses. This rule is wired and
 * functionally correct but will fire zero violations against the current seed data. A
 * synthetic-edge unit test below demonstrates genuine rule firing. When real MINIMUM_CREDITS
 * per-course seed data is added (future migration), this rule will fire against it without code
 * changes.
 *
 * <p>Package-private — only used by {@link SimulationValidationService}. Pure Java, no Spring/JPA
 * (ARQ-002, ADR-F4-004).
 */
class MinCreditsRule {

  /**
   * Returns a blocking notice when a MINIMUM_CREDITS prerequisite requirement is not satisfied.
   *
   * <p>Checks all MINIMUM_CREDITS edges in the requirements list. Returns the first unmet edge.
   * {@code accumulatedPassedCredits} is the student's total accumulated credits across all passed
   * courses (NOT per-program — this is a global accumulated-credits gate).
   *
   * @param candidate the selection being added
   * @param requirements all prerequisite requirement edges for the candidate course
   * @param accumulatedPassedCredits total credits the student has passed (across all programs)
   * @return optional blocking notice with code {@code SIMULATION_MIN_CREDITS_NOT_MET}
   */
  Optional<ValidationNotice> evaluate(
      CandidateSelection candidate,
      List<PrereqRequirement> requirements,
      int accumulatedPassedCredits) {

    for (PrereqRequirement req : requirements) {
      if (!"MINIMUM_CREDITS".equalsIgnoreCase(req.type())) {
        continue;
      }
      if (req.minimumCredits() == null) {
        continue;
      }
      int required = req.minimumCredits();
      if (accumulatedPassedCredits < required) {
        return Optional.of(
            new ValidationNotice(
                "SIMULATION_MIN_CREDITS_NOT_MET",
                "Minimum accumulated credits not met for '"
                    + candidate.courseName()
                    + "': required "
                    + required
                    + " but student has "
                    + accumulatedPassedCredits));
      }
    }
    return Optional.empty();
  }
}
