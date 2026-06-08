package co.edu.uniquindio.brujula.simulation.application.port.in;

import java.util.UUID;

/** Input port: soft-delete a simulation (SIM-005). */
public interface DeleteSimulationUseCase {

  void deleteSimulation(UUID simulationId, UUID callerId);
}
