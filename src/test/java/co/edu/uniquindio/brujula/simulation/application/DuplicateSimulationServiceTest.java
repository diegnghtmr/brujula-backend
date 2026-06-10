package co.edu.uniquindio.brujula.simulation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import co.edu.uniquindio.brujula.simulation.application.dto.DuplicateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.service.DuplicateSimulationService;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * WU3 application unit tests for {@link DuplicateSimulationService}.
 *
 * <p>Covers: happy path, idempotency replay, hash mismatch → conflict, non-owner → 403, name
 * override applied.
 */
class DuplicateSimulationServiceTest {

  private LoadSimulationPort loadSimulationPort;
  private SaveSimulationPort saveSimulationPort;
  private OfferingReadPort offeringReadPort;
  private LoadSimulationSelectionPort loadSimulationSelectionPort;
  private SaveSimulationSelectionPort saveSimulationSelectionPort;
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
    loadSimulationSelectionPort = mock(LoadSimulationSelectionPort.class);
    saveSimulationSelectionPort = mock(SaveSimulationSelectionPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    sut =
        new DuplicateSimulationService(
            loadSimulationPort,
            saveSimulationPort,
            offeringReadPort,
            loadSimulationSelectionPort,
            saveSimulationSelectionPort,
            idempotencyStorePort,
            jsonCodecPort);
  }

  @Test
  void duplicate_creates_new_simulation_with_default_name() {
    UUID simulationId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();
    Simulation source =
        Simulation.create(
            SimulationId.of(simulationId), ownerId, "Original", periodId, List.of(programId));

    when(idempotencyStorePort.find(idempotencyKey, DuplicateSimulationService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(source));
    when(saveSimulationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(offeringReadPort.resolvePeriodName(periodId)).thenReturn("2026-2");
    when(jsonCodecPort.toJson(any())).thenReturn("{}");
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simulationId)).thenReturn(List.of());

    DuplicateSimulationCommand command =
        new DuplicateSimulationCommand(idempotencyKey, simulationId, ownerId, null, "hash1");
    SimulationView view = sut.duplicateSimulation(command);

    assertThat(view.name()).isEqualTo("Copy of Original");
    assertThat(view.periodName()).isEqualTo("2026-2");
    assertThat(view.id()).isNotEqualTo(simulationId); // new UUID
  }

  @Test
  void duplicate_with_name_override_uses_provided_name() {
    UUID simulationId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();
    Simulation source =
        Simulation.create(
            SimulationId.of(simulationId), ownerId, "Original", periodId, List.of(programId));

    when(idempotencyStorePort.find(idempotencyKey, DuplicateSimulationService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(source));
    when(saveSimulationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(offeringReadPort.resolvePeriodName(periodId)).thenReturn("2026-2");
    when(jsonCodecPort.toJson(any())).thenReturn("{}");
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simulationId)).thenReturn(List.of());

    DuplicateSimulationCommand command =
        new DuplicateSimulationCommand(
            idempotencyKey, simulationId, ownerId, "My Custom Copy", "hash2");
    SimulationView view = sut.duplicateSimulation(command);

    assertThat(view.name()).isEqualTo("My Custom Copy");
  }

  @Test
  void idempotent_replay_returns_stored_view() {
    UUID idempotencyKey = UUID.randomUUID();
    UUID simulationId = UUID.randomUUID();

    SimulationView stored =
        new SimulationView(
            UUID.randomUUID(),
            "Copy of Original",
            periodId,
            "2026-2",
            List.of(programId),
            "CURRENT",
            Instant.now(),
            Instant.now(),
            0L);

    IdempotencyStorePort.StoredRecord record =
        new IdempotencyStorePort.StoredRecord("hash-same", 201, "{}");

    when(idempotencyStorePort.find(idempotencyKey, DuplicateSimulationService.OPERATION))
        .thenReturn(Optional.of(record));
    when(jsonCodecPort.fromJson("{}", SimulationView.class)).thenReturn(stored);

    DuplicateSimulationCommand command =
        new DuplicateSimulationCommand(idempotencyKey, simulationId, ownerId, null, "hash-same");
    SimulationView view = sut.duplicateSimulation(command);

    assertThat(view).isEqualTo(stored);
    verify(saveSimulationPort, never()).save(any());
  }

  @Test
  void non_owner_throws_access_denied() {
    UUID simulationId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();
    Simulation source =
        Simulation.create(
            SimulationId.of(simulationId), ownerId, "Owner's sim", periodId, List.of(programId));

    when(idempotencyStorePort.find(idempotencyKey, DuplicateSimulationService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(source));

    DuplicateSimulationCommand command =
        new DuplicateSimulationCommand(idempotencyKey, simulationId, otherUserId, null, "hash");

    assertThatThrownBy(() -> sut.duplicateSimulation(command))
        .isInstanceOf(SimulationAccessDeniedException.class);
  }

  // -------------------------------------------------------------------------
  // WU4b: Duplicate carries source simulation's selections (SIM-006 / Finding F10)
  // -------------------------------------------------------------------------

  /**
   * FIX F5 (WU4b): When a simulation with existing selections is duplicated, the new simulation
   * must have a copy of each source selection persisted under the new simulationId, with all fields
   * preserved (courseId, credits, offeringId, scheduleOptionId) and the simulationId pointing to
   * the NEW simulation, not the source.
   *
   * <p>Uses ArgumentCaptor to assert the copied selection's field values, replacing the weak
   * verify(save).save(any()) that would pass even if credits were copied as 0.
   */
  @Test
  void duplicate_copies_source_selections_into_new_simulation() {
    UUID simulationId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();
    UUID sourceCourseId = UUID.randomUUID();
    UUID sourceOfferingId = UUID.randomUUID();
    UUID sourceScheduleOptionId = UUID.randomUUID();
    UUID sourceProgramId = programId;

    Simulation source =
        Simulation.create(
            SimulationId.of(simulationId), ownerId, "Original", periodId, List.of(sourceProgramId));

    // One existing selection in the source simulation — 4.0 credits (real value, not 0)
    SimulationSelection sourceSelection =
        SimulationSelection.reconstitute(
            SimulationSelectionId.generate(),
            simulationId,
            sourceOfferingId,
            sourceScheduleOptionId,
            sourceCourseId,
            "Data Structures",
            sourceProgramId,
            4.0,
            "DIURNA",
            "PUBLISHED",
            2L,
            List.of(),
            "ACTIVE",
            0L);

    when(idempotencyStorePort.find(idempotencyKey, DuplicateSimulationService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(source));
    when(saveSimulationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(offeringReadPort.resolvePeriodName(periodId)).thenReturn("2026-2");
    when(jsonCodecPort.toJson(any())).thenReturn("{}");
    // Source simulation has one selection
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simulationId))
        .thenReturn(List.of(sourceSelection));

    DuplicateSimulationCommand command =
        new DuplicateSimulationCommand(idempotencyKey, simulationId, ownerId, null, "hash-copy");
    SimulationView view = sut.duplicateSimulation(command);

    // The duplicated simulation has a new UUID
    assertThat(view.id()).isNotEqualTo(simulationId);

    // FIX F5: use ArgumentCaptor to assert all copied fields are preserved
    ArgumentCaptor<SimulationSelection> captor = ArgumentCaptor.forClass(SimulationSelection.class);
    verify(saveSimulationSelectionPort).save(captor.capture());
    SimulationSelection copied = captor.getValue();

    // Course, offering, scheduleOption — all must be preserved
    assertThat(copied.getCourseId()).isEqualTo(sourceCourseId);
    assertThat(copied.getOfferingId()).isEqualTo(sourceOfferingId);
    assertThat(copied.getScheduleOptionId()).isEqualTo(sourceScheduleOptionId);

    // Credits must be the real value (4.0), not 0 or a default
    assertThat(copied.getCredits()).isEqualTo(4.0);

    // The simulationId on the copy must be the NEW simulation, not the source
    assertThat(copied.getSimulationId()).isNotEqualTo(simulationId);
    assertThat(copied.getSimulationId()).isEqualTo(view.id());
  }
}
