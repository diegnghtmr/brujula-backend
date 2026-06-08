package co.edu.uniquindio.brujula.simulation.domain;

import java.util.List;
import java.util.Objects;

/**
 * Thrown when {@code reviewSimulation} re-runs all validations and finds persistent blocking
 * violations (spec STALE-002). Maps to HTTP 409 application/problem+json via
 * {@code SimulationExceptionHandler}.
 *
 * <p>Distinct from {@link SimulationValidationBlockedException} (add-selection 409) to allow
 * different HTTP responses or titles if needed. Same structure: holds the blocking notices list.
 */
public class SimulationStillBlockedException extends RuntimeException {

  private final List<ValidationNotice> blocking;

  public SimulationStillBlockedException(List<ValidationNotice> blocking) {
    super("Simulation still has " + blocking.size() + " blocking validation(s) after review");
    this.blocking = Objects.requireNonNull(blocking, "blocking list required");
  }

  public List<ValidationNotice> getBlocking() {
    return blocking;
  }

  /**
   * Returns the primary (first) blocking notice, used by the exception handler to set
   * the problem+json code and detail fields.
   */
  public ValidationNotice primaryNotice() {
    return blocking.isEmpty()
        ? new ValidationNotice("SIMULATION_REVIEW_BLOCKED", "Blocking violations remain", null)
        : blocking.getFirst();
  }
}
