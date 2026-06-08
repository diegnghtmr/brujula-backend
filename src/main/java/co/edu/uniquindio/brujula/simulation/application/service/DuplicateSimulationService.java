package co.edu.uniquindio.brujula.simulation.application.service;

import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import co.edu.uniquindio.brujula.simulation.application.dto.DuplicateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.port.in.DuplicateSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import java.util.List;
import java.util.Optional;

/**
 * Use case: duplicate a simulation (SIM-006).
 *
 * <p>Idempotent via sharedkernel {@link IdempotencyStorePort} (X-001). Operation name:
 * {@value #OPERATION}. Creates a new simulation with a copy of the original's metadata.
 * Selections are copied in WU4 (for WU3 the duplicate has zero selections).
 *
 * <p>Ownership enforced per X-003.
 *
 * <p>Framework-free — wired by {@code SimulationConfiguration}.
 */
public class DuplicateSimulationService implements DuplicateSimulationUseCase {

  public static final String OPERATION = "DUPLICATE_SIMULATION";

  private final LoadSimulationPort loadSimulationPort;
  private final SaveSimulationPort saveSimulationPort;
  private final OfferingReadPort offeringReadPort;
  private final LoadSimulationSelectionPort loadSimulationSelectionPort;
  private final SaveSimulationSelectionPort saveSimulationSelectionPort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;

  public DuplicateSimulationService(
      LoadSimulationPort loadSimulationPort,
      SaveSimulationPort saveSimulationPort,
      OfferingReadPort offeringReadPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort,
      SaveSimulationSelectionPort saveSimulationSelectionPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort) {
    this.loadSimulationPort = loadSimulationPort;
    this.saveSimulationPort = saveSimulationPort;
    this.offeringReadPort = offeringReadPort;
    this.loadSimulationSelectionPort = loadSimulationSelectionPort;
    this.saveSimulationSelectionPort = saveSimulationSelectionPort;
    this.idempotencyStorePort = idempotencyStorePort;
    this.jsonCodecPort = jsonCodecPort;
  }

  @Override
  public SimulationView duplicateSimulation(DuplicateSimulationCommand command) {
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

    // 2. Load source simulation
    Simulation source = loadSimulationPort.loadById(command.simulationId())
        .filter(s -> s.getLifecycleStatus() == SimulationLifecycle.ACTIVE)
        .orElseThrow(() -> new SimulationNotFoundException(command.simulationId()));

    // 3. Ownership guard (X-003)
    if (!source.getOwnerId().equals(command.ownerId())) {
      throw new SimulationAccessDeniedException(command.simulationId());
    }

    // 4. Determine name for duplicate
    String duplicateName = command.name() != null
        ? command.name()
        : "Copy of " + source.getName();

    // 5. Create duplicate with a new ID
    SimulationId newId = SimulationId.generate();
    Simulation duplicate = Simulation.create(
        newId,
        command.ownerId(),
        duplicateName,
        source.getPeriodId(),
        source.getProgramIds());
    Simulation saved = saveSimulationPort.save(duplicate);

    // 5b. Copy source simulation's selections into the duplicate (WU4b — SIM-006 / Finding F10)
    List<SimulationSelection> sourceSelections =
        loadSimulationSelectionPort.loadActiveBySimulationId(command.simulationId());
    for (SimulationSelection sourceSelection : sourceSelections) {
      SimulationSelection copiedSelection = SimulationSelection.reconstitute(
          SimulationSelectionId.generate(),
          saved.getId().value(),         // new simulationId
          sourceSelection.getOfferingId(),
          sourceSelection.getScheduleOptionId(),
          sourceSelection.getCourseId(),
          sourceSelection.getCourseName(),
          sourceSelection.getProgramId(),
          sourceSelection.getCredits(),
          sourceSelection.getJourneyCode(),
          sourceSelection.getOfferingStatus(),
          sourceSelection.getSelectedOfferingVersion(),
          sourceSelection.getSessions(),
          "ACTIVE",
          0L);
      saveSimulationSelectionPort.save(copiedSelection);
    }

    // 6. Resolve period name
    String periodName;
    try {
      periodName = offeringReadPort.resolvePeriodName(saved.getPeriodId());
    } catch (Exception e) {
      periodName = saved.getPeriodId().toString();
    }

    // 7. Build view and store idempotency record
    SimulationView view = new SimulationView(
        saved.getId().value(),
        saved.getName(),
        saved.getPeriodId(),
        periodName,
        saved.getProgramIds(),
        "CURRENT",
        saved.getCreatedAt(),
        saved.getUpdatedAt(),
        saved.getVersion());

    String responseBodyJson = jsonCodecPort.toJson(view);
    idempotencyStorePort.save(
        command.idempotencyKey(), OPERATION, command.requestHash(), 201, responseBodyJson);

    return view;
  }
}
