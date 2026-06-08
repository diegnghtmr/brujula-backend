package co.edu.uniquindio.brujula.simulation.application.service;

import co.edu.uniquindio.brujula.simulation.application.dto.SimulationSelectionView;
import co.edu.uniquindio.brujula.simulation.application.port.in.ListSimulationSelectionsQuery;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import java.util.List;
import java.util.UUID;

/**
 * Application service: list active selections for a simulation (spec VAL-011).
 *
 * <p>Validates ownership. Returns the list (may be empty). Framework-free.
 */
public class ListSimulationSelectionsService implements ListSimulationSelectionsQuery {

  private final LoadSimulationPort loadSimulationPort;
  private final LoadSimulationSelectionPort loadSimulationSelectionPort;

  public ListSimulationSelectionsService(
      LoadSimulationPort loadSimulationPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort) {
    this.loadSimulationPort = loadSimulationPort;
    this.loadSimulationSelectionPort = loadSimulationSelectionPort;
  }

  @Override
  public List<SimulationSelectionView> listSelections(UUID simulationId, UUID ownerId) {
    Simulation simulation = loadSimulationPort.loadById(simulationId)
        .orElseThrow(() -> new SimulationNotFoundException(simulationId));
    if (!simulation.getOwnerId().equals(ownerId)) {
      throw new SimulationAccessDeniedException(simulationId);
    }

    List<SimulationSelection> selections =
        loadSimulationSelectionPort.loadActiveBySimulationId(simulationId);

    return selections.stream().map(this::toView).toList();
  }

  private SimulationSelectionView toView(SimulationSelection selection) {
    List<SimulationSelectionView.SessionView> sessions = selection.getSessions().stream()
        .map(s -> new SimulationSelectionView.SessionView(
            s.day(), s.startsAt().toString(), s.endsAt().toString()))
        .toList();

    return new SimulationSelectionView(
        selection.getId().value(),
        selection.getOfferingId(),
        selection.getScheduleOptionId(),
        selection.getCourseId(),
        selection.getCourseName(),
        selection.getProgramId(),
        selection.getCredits(),
        selection.getJourneyCode(),
        selection.getOfferingStatus(),
        selection.getSelectedOfferingVersion(),
        sessions,
        List.of()); // WU4a: warnings always empty
  }
}
