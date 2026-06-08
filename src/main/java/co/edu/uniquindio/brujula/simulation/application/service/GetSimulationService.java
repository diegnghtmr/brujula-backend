package co.edu.uniquindio.brujula.simulation.application.service;

import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.port.in.GetSimulationQuery;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import java.util.UUID;

/**
 * Use case: get a single simulation (SIM-003 SKELETON).
 *
 * <p>WU3: returns a basic {@link SimulationView}. Full SimulationDetail assembly (selections,
 * credit summary, weekly schedule, validation snapshot) is deferred to WU5.
 *
 * <p>Ownership enforced per X-003: non-owner gets 403.
 *
 * <p>Framework-free — wired by {@code SimulationConfiguration}.
 */
public class GetSimulationService implements GetSimulationQuery {

  private final LoadSimulationPort loadSimulationPort;
  private final OfferingReadPort offeringReadPort;

  public GetSimulationService(
      LoadSimulationPort loadSimulationPort,
      OfferingReadPort offeringReadPort) {
    this.loadSimulationPort = loadSimulationPort;
    this.offeringReadPort = offeringReadPort;
  }

  @Override
  public SimulationView getSimulation(UUID simulationId, UUID callerId) {
    Simulation simulation = loadSimulationPort.loadById(simulationId)
        .filter(s -> s.getLifecycleStatus() == SimulationLifecycle.ACTIVE)
        .orElseThrow(() -> new SimulationNotFoundException(simulationId));

    if (!simulation.getOwnerId().equals(callerId)) {
      throw new SimulationAccessDeniedException(simulationId);
    }

    String periodName;
    try {
      periodName = offeringReadPort.resolvePeriodName(simulation.getPeriodId());
    } catch (Exception e) {
      periodName = simulation.getPeriodId().toString();
    }

    // WU3: status always CURRENT (staleness deferred to WU5)
    return new SimulationView(
        simulation.getId().value(),
        simulation.getName(),
        simulation.getPeriodId(),
        periodName,
        simulation.getProgramIds(),
        "CURRENT",
        simulation.getCreatedAt(),
        simulation.getUpdatedAt(),
        simulation.getVersion());
  }
}
