package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Command to update a simulation's name (SIM-004).
 *
 * <p>Carries {@code version} for optimistic locking (X-002).
 */
public record UpdateSimulationCommand(
    UUID simulationId,
    UUID ownerId,
    String name,
    long version) {

  public UpdateSimulationCommand {
    Objects.requireNonNull(simulationId, "simulationId required");
    Objects.requireNonNull(ownerId, "ownerId required");
  }
}
