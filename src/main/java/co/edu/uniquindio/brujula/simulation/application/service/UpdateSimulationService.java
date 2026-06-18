package co.edu.uniquindio.brujula.simulation.application.service;

import co.edu.uniquindio.brujula.simulation.application.dto.SimulationScopeEntryDto;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.dto.UpdateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.port.in.UpdateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Use case: rename a simulation with optimistic locking (SIM-004, X-002, X-003).
 *
 * <p>Mirrors {@code UpdateSubscriptionService}: uses the RETURNED entity after {@link
 * SaveSimulationPort#save(Simulation)} to capture the DB-incremented version. JPA {@code @Version}
 * collision surfaces as {@link OptimisticLockingFailureException} → 409.
 *
 * <p>Ownership enforced per X-003: non-owner gets 403.
 *
 * <p>Framework-free — wired by {@code SimulationConfiguration}.
 */
public class UpdateSimulationService implements UpdateSimulationUseCase {

  private final LoadSimulationPort loadSimulationPort;
  private final SaveSimulationPort saveSimulationPort;
  private final OfferingReadPort offeringReadPort;

  public UpdateSimulationService(
      LoadSimulationPort loadSimulationPort,
      SaveSimulationPort saveSimulationPort,
      OfferingReadPort offeringReadPort) {
    this.loadSimulationPort = loadSimulationPort;
    this.saveSimulationPort = saveSimulationPort;
    this.offeringReadPort = offeringReadPort;
  }

  @Override
  public SimulationView updateSimulation(UpdateSimulationCommand command) {
    Simulation simulation =
        loadSimulationPort
            .loadById(command.simulationId())
            .filter(s -> s.getLifecycleStatus() == SimulationLifecycle.ACTIVE)
            .orElseThrow(() -> new SimulationNotFoundException(command.simulationId()));

    // BOLA ownership guard (X-003)
    if (!simulation.getOwnerId().equals(command.ownerId())) {
      throw new SimulationAccessDeniedException(command.simulationId());
    }

    // Apply update
    if (command.name() != null) {
      simulation.rename(command.name());
    }

    // Use the RETURNED entity — it carries the DB-incremented version (mirrors FIX-W4)
    Simulation saved = saveSimulationPort.save(simulation);

    String periodName;
    try {
      periodName = offeringReadPort.resolvePeriodName(saved.getPeriodId());
    } catch (Exception e) {
      periodName = saved.getPeriodId().toString();
    }

    return new SimulationView(
        saved.getId().value(),
        saved.getName(),
        saved.getPeriodId(),
        periodName,
        saved.getProgramIds(),
        saved.getScopeEntries().stream()
            .map(e -> new SimulationScopeEntryDto(e.programId(), e.journeyCode()))
            .toList(),
        "CURRENT",
        saved.getCreatedAt(),
        saved.getUpdatedAt(),
        saved.getVersion());
  }
}
