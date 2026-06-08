package co.edu.uniquindio.brujula.simulation.domain;

import java.util.UUID;

/** Thrown when a simulation is not found or the caller is not the owner (BOLA-safe). */
public class SimulationNotFoundException extends RuntimeException {

  public SimulationNotFoundException(UUID simulationId) {
    super("Simulation not found: " + simulationId);
  }

  public SimulationNotFoundException(String message) {
    super(message);
  }
}
