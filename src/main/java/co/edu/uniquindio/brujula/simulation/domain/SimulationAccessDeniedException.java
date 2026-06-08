package co.edu.uniquindio.brujula.simulation.domain;

import java.util.UUID;

/**
 * Thrown when the authenticated user is not the owner of the simulation (BOLA — X-003).
 *
 * <p>Maps to HTTP 403 in {@code SimulationExceptionHandler}.
 */
public class SimulationAccessDeniedException extends RuntimeException {

  public SimulationAccessDeniedException(UUID simulationId) {
    super("Access denied to simulation: " + simulationId);
  }
}
