package co.edu.uniquindio.brujula.simulation.application.service;

import co.edu.uniquindio.brujula.simulation.application.port.in.RemoveSimulationSelectionUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionNotFoundException;
import java.util.UUID;

/**
 * Application service: soft-delete a simulation selection (spec VAL-010).
 *
 * <p>Validates ownership of the simulation. Returns void (HTTP 204). Framework-free — wired by
 * {@code SimulationConfiguration}.
 */
public class RemoveSimulationSelectionService implements RemoveSimulationSelectionUseCase {

  private final LoadSimulationPort loadSimulationPort;
  private final LoadSimulationSelectionPort loadSimulationSelectionPort;
  private final SaveSimulationSelectionPort saveSimulationSelectionPort;

  public RemoveSimulationSelectionService(
      LoadSimulationPort loadSimulationPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort,
      SaveSimulationSelectionPort saveSimulationSelectionPort) {
    this.loadSimulationPort = loadSimulationPort;
    this.loadSimulationSelectionPort = loadSimulationSelectionPort;
    this.saveSimulationSelectionPort = saveSimulationSelectionPort;
  }

  @Override
  public void removeSelection(UUID simulationId, UUID selectionId, UUID ownerId) {
    // 1. Load simulation + ownership check
    Simulation simulation =
        loadSimulationPort
            .loadById(simulationId)
            .orElseThrow(() -> new SimulationNotFoundException(simulationId));
    if (!simulation.getOwnerId().equals(ownerId)) {
      throw new SimulationAccessDeniedException(simulationId);
    }

    // 2. Load selection (also validates it belongs to this simulation)
    SimulationSelection selection =
        loadSimulationSelectionPort
            .loadById(selectionId)
            .filter(s -> simulationId.equals(s.getSimulationId()))
            .orElseThrow(() -> new SimulationSelectionNotFoundException(selectionId));

    // 3. Soft delete
    selection.markDeleted();
    saveSimulationSelectionPort.save(selection);
  }
}
