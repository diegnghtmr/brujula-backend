package co.edu.uniquindio.brujula.simulation.domain;

import java.util.List;

/**
 * Aggregate result of running all validation rules against a candidate selection.
 *
 * <p>Partitions the collected notices into blocking and warning lists. Immutable.
 *
 * @param blocking notices that prevent the selection (SIMULATION_* codes, INV-003)
 * @param warnings advisory notices that ride in the response body without blocking
 */
public record SimulationValidationResult(
    List<ValidationNotice> blocking, List<ValidationNotice> warnings) {

  public SimulationValidationResult {
    blocking = blocking == null ? List.of() : List.copyOf(blocking);
    warnings = warnings == null ? List.of() : List.copyOf(warnings);
  }

  /** True when there is at least one blocking notice. */
  public boolean hasBlockers() {
    return !blocking.isEmpty();
  }

  /** Convert to the typed {@link ValidationOutcome} sealed hierarchy. */
  public ValidationOutcome toOutcome() {
    if (hasBlockers()) {
      return new ValidationOutcome.Blocked(blocking, warnings);
    }
    return new ValidationOutcome.Allowed(warnings);
  }
}
