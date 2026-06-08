package co.edu.uniquindio.brujula.simulation.domain;

import java.util.UUID;

/**
 * Thrown when a simulation selection cannot be found (→ HTTP 404).
 *
 * <p>Also used for BOLA enforcement: a non-owner gets the same 404 to avoid resource enumeration.
 */
public class SimulationSelectionNotFoundException extends RuntimeException {

  private final UUID selectionId;

  public SimulationSelectionNotFoundException(UUID selectionId) {
    super("Simulation selection not found: " + selectionId);
    this.selectionId = selectionId;
  }

  public UUID getSelectionId() {
    return selectionId;
  }
}
