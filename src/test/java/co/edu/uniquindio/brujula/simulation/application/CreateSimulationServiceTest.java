package co.edu.uniquindio.brujula.simulation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import co.edu.uniquindio.brujula.simulation.application.dto.CreateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationScopeEntryDto;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.service.CreateSimulationService;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Application unit tests for {@link CreateSimulationService}.
 *
 * <p>Covers: happy path, idempotency replay returns stored, hash mismatch → conflict, periodName
 * resolved from stub OfferingReadPort.
 */
class CreateSimulationServiceTest {

  private LoadSimulationPort loadSimulationPort;
  private SaveSimulationPort saveSimulationPort;
  private OfferingReadPort offeringReadPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private CreateSimulationService sut;

  private final UUID ownerId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSimulationPort = mock(LoadSimulationPort.class);
    saveSimulationPort = mock(SaveSimulationPort.class);
    offeringReadPort = mock(OfferingReadPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);

    sut =
        new CreateSimulationService(
            loadSimulationPort,
            saveSimulationPort,
            offeringReadPort,
            idempotencyStorePort,
            jsonCodecPort);
  }

  @Test
  void happy_path_creates_simulation_with_period_name() {
    UUID idempotencyKey = UUID.randomUUID();
    CreateSimulationCommand command =
        new CreateSimulationCommand(
            idempotencyKey,
            ownerId,
            "Sim 2026-2",
            periodId,
            List.of(new SimulationScopeEntryDto(programId, "DIURNA")),
            "hash1");

    when(idempotencyStorePort.find(idempotencyKey, CreateSimulationService.OPERATION))
        .thenReturn(Optional.empty());
    when(offeringReadPort.resolvePeriodName(periodId)).thenReturn("2026-2");
    when(saveSimulationPort.save(any()))
        .thenAnswer(
            inv -> {
              Simulation sim = inv.getArgument(0);
              return sim;
            });
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    SimulationView view = sut.createSimulation(command);

    assertThat(view.name()).isEqualTo("Sim 2026-2");
    assertThat(view.periodName()).isEqualTo("2026-2");
    assertThat(view.status()).isEqualTo("CURRENT");
    assertThat(view.programIds()).containsExactly(programId);
    verify(saveSimulationPort).save(any());
    verify(idempotencyStorePort)
        .save(
            eq(idempotencyKey), eq(CreateSimulationService.OPERATION), eq("hash1"), eq(201), any());
  }

  @Test
  void idempotent_replay_returns_stored_view_without_saving() {
    UUID idempotencyKey = UUID.randomUUID();
    CreateSimulationCommand command =
        new CreateSimulationCommand(
            idempotencyKey,
            ownerId,
            "Sim replay",
            periodId,
            List.of(new SimulationScopeEntryDto(programId, "DIURNA")),
            "hash-same");

    SimulationView stored =
        new SimulationView(
            UUID.randomUUID(),
            "Sim replay",
            periodId,
            "2026-2",
            List.of(programId),
            List.of(new SimulationScopeEntryDto(programId, "DIURNA")),
            "CURRENT",
            Instant.now(),
            Instant.now(),
            0L);

    IdempotencyStorePort.StoredRecord record =
        new IdempotencyStorePort.StoredRecord("hash-same", 201, "{...}");

    when(idempotencyStorePort.find(idempotencyKey, CreateSimulationService.OPERATION))
        .thenReturn(Optional.of(record));
    when(jsonCodecPort.fromJson("{...}", SimulationView.class)).thenReturn(stored);

    SimulationView view = sut.createSimulation(command);

    assertThat(view).isEqualTo(stored);
    verify(saveSimulationPort, never()).save(any());
  }

  @Test
  void hash_mismatch_throws_idempotency_conflict() {
    UUID idempotencyKey = UUID.randomUUID();
    CreateSimulationCommand command =
        new CreateSimulationCommand(
            idempotencyKey,
            ownerId,
            "Different name",
            periodId,
            List.of(new SimulationScopeEntryDto(programId, "DIURNA")),
            "hash-new");

    IdempotencyStorePort.StoredRecord record =
        new IdempotencyStorePort.StoredRecord("hash-original", 201, "{}");

    when(idempotencyStorePort.find(idempotencyKey, CreateSimulationService.OPERATION))
        .thenReturn(Optional.of(record));

    assertThatThrownBy(() -> sut.createSimulation(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);
  }
}
