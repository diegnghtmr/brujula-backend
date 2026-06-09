package co.edu.uniquindio.brujula.simulation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.dto.UpdateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.service.UpdateSimulationService;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU3 application unit tests for {@link UpdateSimulationService}.
 *
 * <p>Covers: happy path rename, non-owner → 403, not-found → 404. OptimisticLockingFailureException
 * is tested at the JPA level in IT.
 */
class UpdateSimulationServiceTest {

  private LoadSimulationPort loadSimulationPort;
  private SaveSimulationPort saveSimulationPort;
  private OfferingReadPort offeringReadPort;
  private UpdateSimulationService sut;

  private final UUID ownerId = UUID.randomUUID();
  private final UUID otherUserId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSimulationPort = mock(LoadSimulationPort.class);
    saveSimulationPort = mock(SaveSimulationPort.class);
    offeringReadPort = mock(OfferingReadPort.class);
    sut = new UpdateSimulationService(loadSimulationPort, saveSimulationPort, offeringReadPort);
  }

  @Test
  void rename_updates_name_and_returns_view() {
    UUID simulationId = UUID.randomUUID();
    Simulation sim =
        Simulation.create(
            SimulationId.of(simulationId), ownerId, "Old Name", periodId, List.of(programId));

    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(sim));
    when(saveSimulationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(offeringReadPort.resolvePeriodName(periodId)).thenReturn("2026-2");

    UpdateSimulationCommand command =
        new UpdateSimulationCommand(simulationId, ownerId, "New Name", 0L);
    SimulationView view = sut.updateSimulation(command);

    assertThat(view.name()).isEqualTo("New Name");
    assertThat(view.periodName()).isEqualTo("2026-2");
  }

  @Test
  void rename_returns_persisted_incremented_version() {
    // Guards the Fase-3 repeat-bug: the returned view must carry the SAVED entity's
    // post-flush version (N+1), not the input command's stale version (N). The save port
    // simulates the JPA @Version increment by returning a reconstituted simulation at version 1.
    UUID simulationId = UUID.randomUUID();
    Simulation atVersionZero =
        Simulation.create(
            SimulationId.of(simulationId), ownerId, "Old Name", periodId, List.of(programId));
    Instant now = Instant.now();
    Simulation flushedAtVersionOne =
        Simulation.reconstitute(
            SimulationId.of(simulationId),
            ownerId,
            "New Name",
            periodId,
            List.of(programId),
            SimulationLifecycle.ACTIVE,
            now,
            now,
            1L);

    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(atVersionZero));
    when(saveSimulationPort.save(any())).thenReturn(flushedAtVersionOne);
    when(offeringReadPort.resolvePeriodName(periodId)).thenReturn("2026-2");

    UpdateSimulationCommand command =
        new UpdateSimulationCommand(simulationId, ownerId, "New Name", 0L);
    SimulationView view = sut.updateSimulation(command);

    assertThat(view.version()).isEqualTo(1L);
  }

  @Test
  void non_owner_throws_access_denied() {
    UUID simulationId = UUID.randomUUID();
    Simulation sim =
        Simulation.create(
            SimulationId.of(simulationId), ownerId, "Name", periodId, List.of(programId));

    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(sim));

    UpdateSimulationCommand command =
        new UpdateSimulationCommand(simulationId, otherUserId, "Hijack", 0L);

    assertThatThrownBy(() -> sut.updateSimulation(command))
        .isInstanceOf(SimulationAccessDeniedException.class);
  }

  @Test
  void not_found_throws() {
    UUID simulationId = UUID.randomUUID();
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.empty());

    UpdateSimulationCommand command =
        new UpdateSimulationCommand(simulationId, ownerId, "Name", 0L);

    assertThatThrownBy(() -> sut.updateSimulation(command))
        .isInstanceOf(SimulationNotFoundException.class);
  }
}
