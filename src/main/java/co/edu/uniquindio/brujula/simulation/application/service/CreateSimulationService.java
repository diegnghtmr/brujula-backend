package co.edu.uniquindio.brujula.simulation.application.service;

import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import co.edu.uniquindio.brujula.simulation.application.dto.CreateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationScopeEntryDto;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.port.in.CreateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationScopeEntry;
import java.util.Optional;

/**
 * Use case: create a simulation (SIM-001).
 *
 * <p>Idempotent via sharedkernel {@link IdempotencyStorePort} (X-001). Operation name: {@value
 * #OPERATION}. Resolves {@code periodName} via {@link OfferingReadPort} (ADR-F4-005). Status is
 * always "CURRENT" at creation time — never persisted (INV-004).
 *
 * <p>Framework-free — wired by {@code SimulationConfiguration}.
 */
public class CreateSimulationService implements CreateSimulationUseCase {

  public static final String OPERATION = "CREATE_SIMULATION";

  private final LoadSimulationPort loadSimulationPort;
  private final SaveSimulationPort saveSimulationPort;
  private final OfferingReadPort offeringReadPort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;

  public CreateSimulationService(
      LoadSimulationPort loadSimulationPort,
      SaveSimulationPort saveSimulationPort,
      OfferingReadPort offeringReadPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort) {
    this.loadSimulationPort = loadSimulationPort;
    this.saveSimulationPort = saveSimulationPort;
    this.offeringReadPort = offeringReadPort;
    this.idempotencyStorePort = idempotencyStorePort;
    this.jsonCodecPort = jsonCodecPort;
  }

  @Override
  public SimulationView createSimulation(CreateSimulationCommand command) {
    // 1. Idempotency check
    Optional<IdempotencyStorePort.StoredRecord> existing =
        idempotencyStorePort.find(command.idempotencyKey(), OPERATION);
    if (existing.isPresent()) {
      IdempotencyStorePort.StoredRecord stored = existing.get();
      if (!stored.requestHash().equals(command.requestHash())) {
        throw new IdempotencyKeyConflictException(command.idempotencyKey(), OPERATION);
      }
      return jsonCodecPort.fromJson(stored.responseBodyJson(), SimulationView.class);
    }

    // 2. Resolve period name from offering::api
    String periodName = offeringReadPort.resolvePeriodName(command.periodId());

    // 3. Create and persist simulation
    SimulationId id = SimulationId.generate();
    Simulation simulation =
        Simulation.create(
            id,
            command.ownerId(),
            command.name(),
            command.periodId(),
            command.scopeEntries().stream()
                .map(e -> new SimulationScopeEntry(e.programId(), e.journeyCode()))
                .toList());
    Simulation saved = saveSimulationPort.save(simulation);

    // 4. Build view; status is always CURRENT at creation (INV-004)
    SimulationView view = toView(saved, periodName, "CURRENT");

    // 5. Store idempotency record
    String responseBodyJson = jsonCodecPort.toJson(view);
    idempotencyStorePort.save(
        command.idempotencyKey(), OPERATION, command.requestHash(), 201, responseBodyJson);

    return view;
  }

  private SimulationView toView(Simulation sim, String periodName, String status) {
    return new SimulationView(
        sim.getId().value(),
        sim.getName(),
        sim.getPeriodId(),
        periodName,
        sim.getProgramIds(),
        sim.getScopeEntries().stream()
            .map(e -> new SimulationScopeEntryDto(e.programId(), e.journeyCode()))
            .toList(),
        status,
        sim.getCreatedAt(),
        sim.getUpdatedAt(),
        sim.getVersion());
  }
}
