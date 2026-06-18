package co.edu.uniquindio.brujula.simulation.domain;

import java.util.Optional;

/**
 * Pure validation rule: blocks selecting a schedule option from an ARCHIVED offering (spec VAL-007
 * — SIMULATION_ARCHIVED_OFFERING).
 *
 * <p>Rule: if the candidate's resolved offering status is "ARCHIVED", the selection is blocked.
 * PRELIMINARY and FINAL offerings are allowed (PRELIMINARY may produce a different advisory in
 * future, but this rule only handles the ARCHIVED blocking path).
 *
 * <p>Package-private — only used by {@link SimulationValidationService}. Pure Java, no Spring/JPA
 * (ADR-F4-004).
 */
class ArchivedOfferingRule {

  /**
   * Returns a blocking notice if the candidate's offering is ARCHIVED.
   *
   * @param candidate the selection being added
   * @return optional blocking notice with code {@code SIMULATION_ARCHIVED_OFFERING}
   */
  Optional<ValidationNotice> evaluate(CandidateSelection candidate) {
    if ("ARCHIVED".equalsIgnoreCase(candidate.offeringStatus())) {
      return Optional.of(
          new ValidationNotice(
              "SIMULATION_ARCHIVED_OFFERING",
              "Offering is archived and cannot be selected: " + candidate.courseName()));
    }
    return Optional.empty();
  }
}
