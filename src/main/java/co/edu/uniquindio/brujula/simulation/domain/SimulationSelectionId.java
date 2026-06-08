package co.edu.uniquindio.brujula.simulation.domain;

import java.util.Objects;
import java.util.UUID;

/** Value object: unique identity for a simulation selection. */
public record SimulationSelectionId(UUID value) {

  public SimulationSelectionId {
    Objects.requireNonNull(value, "SimulationSelectionId.value must not be null");
  }

  public static SimulationSelectionId generate() {
    return new SimulationSelectionId(UUID.randomUUID());
  }

  public static SimulationSelectionId of(UUID value) {
    return new SimulationSelectionId(value);
  }
}
