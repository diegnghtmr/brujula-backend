package co.edu.uniquindio.brujula.simulation.domain;

import java.util.Objects;
import java.util.UUID;

/** Value object: unique identity for a {@link Simulation} aggregate. */
public record SimulationId(UUID value) {

  public SimulationId {
    Objects.requireNonNull(value, "SimulationId.value must not be null");
  }

  public static SimulationId generate() {
    return new SimulationId(UUID.randomUUID());
  }

  public static SimulationId of(UUID value) {
    return new SimulationId(value);
  }
}
