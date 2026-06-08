package co.edu.uniquindio.brujula.simulation.application.service;

import co.edu.uniquindio.brujula.simulation.application.port.in.DeleteSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import java.util.UUID;

/**
 * Use case: soft-delete a simulation (SIM-005).
 *
 * <p>Ownership enforced per X-003: non-owner gets 403. Soft delete: audit row preserved.
 *
 * <p>Framework-free — wired by {@code SimulationConfiguration}.
 */
public class DeleteSimulationService implements DeleteSimulationUseCase {

  private final LoadSimulationPort loadSimulationPort;
  private final SaveSimulationPort saveSimulationPort;

  public DeleteSimulationService(
      LoadSimulationPort loadSimulationPort,
      SaveSimulationPort saveSimulationPort) {
    this.loadSimulationPort = loadSimulationPort;
    this.saveSimulationPort = saveSimulationPort;
  }

  @Override
  public void deleteSimulation(UUID simulationId, UUID callerId) {
    Simulation simulation = loadSimulationPort.loadById(simulationId)
        .filter(s -> s.getLifecycleStatus() == SimulationLifecycle.ACTIVE)
        .orElseThrow(() -> new SimulationNotFoundException(simulationId));

    // BOLA ownership guard (X-003)
    if (!simulation.getOwnerId().equals(callerId)) {
      throw new SimulationAccessDeniedException(simulationId);
    }

    simulation.markDeleted();
    saveSimulationPort.save(simulation);
  }
}
