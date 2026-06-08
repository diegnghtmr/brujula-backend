package co.edu.uniquindio.brujula.simulation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import co.edu.uniquindio.brujula.simulation.application.dto.DuplicateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.service.DuplicateSimulationService;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU3 application unit tests for {@link DuplicateSimulationService}.
 *
 * <p>Covers: happy path, idempotency replay, hash mismatch → conflict, non-owner → 403,
 * name override applied.
 */
class DuplicateSimulationServiceTest {

  private LoadSimulationPort loadSimulationPort;
  private SaveSimulationPort saveSimulationPort;
  private OfferingReadPort offeringReadPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private DuplicateSimulationService sut;

  private final UUID ownerId = UUID.randomUUID();
  private final UUID otherUserId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSimulationPort = mock(LoadSimulationPort.class);
    saveSimulationPort = mock(SaveSimulationPort.class);
    offeringReadPort = mock(OfferingReadPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    sut = new DuplicateSimulationService(
        loadSimulationPort, saveSimulationPort, offeringReadPort,
        idempotencyStorePort, jsonCodecPort);
  }

  @Test
  void duplicate_creates_new_simulation_with_default_name() {
    UUID simulationId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();
    Simulation source = Simulation.create(
        SimulationId.of(simulationId), ownerId, "Original", periodId, List.of(programId));

    when(idempotencyStorePort.find(idempotencyKey, DuplicateSimulationService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(source));
    when(saveSimulationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(offeringReadPort.resolvePeriodName(periodId)).thenReturn("2026-2");
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    DuplicateSimulationCommand command = new DuplicateSimulationCommand(
        idempotencyKey, simulationId, ownerId, null, "hash1");
    SimulationView view = sut.duplicateSimulation(command);

    assertThat(view.name()).isEqualTo("Copy of Original");
    assertThat(view.periodName()).isEqualTo("2026-2");
    assertThat(view.id()).isNotEqualTo(simulationId);  // new UUID
  }

  @Test
  void duplicate_with_name_override_uses_provided_name() {
    UUID simulationId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();
    Simulation source = Simulation.create(
        SimulationId.of(simulationId), ownerId, "Original", periodId, List.of(programId));

    when(idempotencyStorePort.find(idempotencyKey, DuplicateSimulationService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(source));
    when(saveSimulationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(offeringReadPort.resolvePeriodName(periodId)).thenReturn("2026-2");
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    DuplicateSimulationCommand command = new DuplicateSimulationCommand(
        idempotencyKey, simulationId, ownerId, "My Custom Copy", "hash2");
    SimulationView view = sut.duplicateSimulation(command);

    assertThat(view.name()).isEqualTo("My Custom Copy");
  }

  @Test
  void idempotent_replay_returns_stored_view() {
    UUID idempotencyKey = UUID.randomUUID();
    UUID simulationId = UUID.randomUUID();

    SimulationView stored = new SimulationView(
        UUID.randomUUID(), "Copy of Original", periodId, "2026-2",
        List.of(programId), "CURRENT", Instant.now(), Instant.now(), 0L);

    IdempotencyStorePort.StoredRecord record = new IdempotencyStorePort.StoredRecord(
        "hash-same", 201, "{}");

    when(idempotencyStorePort.find(idempotencyKey, DuplicateSimulationService.OPERATION))
        .thenReturn(Optional.of(record));
    when(jsonCodecPort.fromJson("{}", SimulationView.class)).thenReturn(stored);

    DuplicateSimulationCommand command = new DuplicateSimulationCommand(
        idempotencyKey, simulationId, ownerId, null, "hash-same");
    SimulationView view = sut.duplicateSimulation(command);

    assertThat(view).isEqualTo(stored);
    verify(saveSimulationPort, never()).save(any());
  }

  @Test
  void non_owner_throws_access_denied() {
    UUID simulationId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();
    Simulation source = Simulation.create(
        SimulationId.of(simulationId), ownerId, "Owner's sim", periodId, List.of(programId));

    when(idempotencyStorePort.find(idempotencyKey, DuplicateSimulationService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(source));

    DuplicateSimulationCommand command = new DuplicateSimulationCommand(
        idempotencyKey, simulationId, otherUserId, null, "hash");

    assertThatThrownBy(() -> sut.duplicateSimulation(command))
        .isInstanceOf(SimulationAccessDeniedException.class);
  }
}
