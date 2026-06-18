package co.edu.uniquindio.brujula.simulation.application.service;

import co.edu.uniquindio.brujula.simulation.application.dto.ListSimulationsCriteria;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationPageView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationScopeEntryDto;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.port.in.ListMySimulationsQuery;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import java.util.List;

/**
 * Use case: list simulations belonging to the authenticated student (SIM-002).
 *
 * <p>BOLA: always filters by ownerId from the authenticated principal. Resolves periodName from
 * offering::api for each simulation. Status is always "CURRENT" in list view (staleness only
 * computed in detail view per design — WU5).
 *
 * <p>Framework-free — wired by {@code SimulationConfiguration}.
 */
public class ListMySimulationsService implements ListMySimulationsQuery {

  private final LoadSimulationPort loadSimulationPort;
  private final OfferingReadPort offeringReadPort;

  public ListMySimulationsService(
      LoadSimulationPort loadSimulationPort, OfferingReadPort offeringReadPort) {
    this.loadSimulationPort = loadSimulationPort;
    this.offeringReadPort = offeringReadPort;
  }

  @Override
  public SimulationPageView listMySimulations(ListSimulationsCriteria criteria) {
    List<Simulation> simulations =
        loadSimulationPort.loadByOwner(
            criteria.ownerId(), criteria.periodId(), criteria.page(), criteria.size());
    long total = loadSimulationPort.countByOwner(criteria.ownerId(), criteria.periodId());
    int totalPages = criteria.size() > 0 ? (int) Math.ceil((double) total / criteria.size()) : 0;

    // Filter by status if requested (CURRENT/OUTDATED)
    // WU3: status computation is simplified — all ACTIVE simulations are "CURRENT"
    // WU5 adds staleness detection per STALE-001
    String statusFilter = criteria.status();

    List<SimulationView> views =
        simulations.stream()
            .map(sim -> toView(sim))
            .filter(v -> statusFilter == null || statusFilter.equals(v.status()))
            .toList();

    return new SimulationPageView(views, criteria.page(), criteria.size(), total, totalPages);
  }

  private SimulationView toView(Simulation sim) {
    // Resolve period name; fall back gracefully if offering temporarily unavailable
    String periodName;
    try {
      periodName = offeringReadPort.resolvePeriodName(sim.getPeriodId());
    } catch (Exception e) {
      periodName = sim.getPeriodId().toString();
    }
    // WU3: status is always CURRENT (staleness deferred to WU5)
    return new SimulationView(
        sim.getId().value(),
        sim.getName(),
        sim.getPeriodId(),
        periodName,
        sim.getProgramIds(),
        sim.getScopeEntries().stream()
            .map(e -> new SimulationScopeEntryDto(e.programId(), e.journeyCode()))
            .toList(),
        "CURRENT",
        sim.getCreatedAt(),
        sim.getUpdatedAt(),
        sim.getVersion());
  }
}
