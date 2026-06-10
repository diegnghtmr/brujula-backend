package co.edu.uniquindio.brujula.simulation.domain;

import java.util.List;
import java.util.Optional;

/**
 * Pure validation rule: blocks adding a course that is already present in the simulation (spec
 * VAL-006 — SIMULATION_DUPLICATE_SELECTION).
 *
 * <p>Duplicate detection is based on {@code courseId} only (not scheduleOptionId variant). A
 * student may not select the same course twice even via a different schedule option.
 *
 * <p>Package-private — only used by {@link SimulationValidationService}. Pure Java, no Spring/JPA
 * (ARQ-002, ADR-F4-004).
 */
class DuplicateSelectionRule {

  /**
   * Returns a blocking notice if the candidate course is already in the existing selections.
   *
   * @param candidate the selection being added
   * @param existingSelections active selections already in the simulation
   * @return optional blocking notice with code {@code SIMULATION_DUPLICATE_SELECTION}
   */
  Optional<ValidationNotice> evaluate(
      CandidateSelection candidate, List<ExistingSelection> existingSelections) {

    boolean alreadyPresent =
        existingSelections.stream().anyMatch(s -> s.courseId().equals(candidate.courseId()));

    if (alreadyPresent) {
      return Optional.of(
          new ValidationNotice(
              "SIMULATION_DUPLICATE_SELECTION",
              "Course already added to the simulation: " + candidate.courseName()));
    }
    return Optional.empty();
  }
}
