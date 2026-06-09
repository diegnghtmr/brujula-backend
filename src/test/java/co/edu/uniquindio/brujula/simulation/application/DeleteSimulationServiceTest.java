package co.edu.uniquindio.brujula.simulation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.service.DeleteSimulationService;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * WU3 application unit tests for {@link DeleteSimulationService}.
 *
 * <p>Covers: happy path soft-delete, non-owner → 403, not-found → 404.
 */
class DeleteSimulationServiceTest {

  private LoadSimulationPort loadSimulationPort;
  private SaveSimulationPort saveSimulationPort;
  private DeleteSimulationService sut;

  private final UUID ownerId = UUID.randomUUID();
  private final UUID otherUserId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSimulationPort = mock(LoadSimulationPort.class);
    saveSimulationPort = mock(SaveSimulationPort.class);
    sut = new DeleteSimulationService(loadSimulationPort, saveSimulationPort);
  }

  @Test
  void soft_delete_marks_simulation_as_deleted() {
    UUID simulationId = UUID.randomUUID();
    Simulation sim =
        Simulation.create(
            SimulationId.of(simulationId), ownerId, "To Delete", periodId, List.of(programId));

    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(sim));
    when(saveSimulationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

    sut.deleteSimulation(simulationId, ownerId);

    ArgumentCaptor<Simulation> captor = ArgumentCaptor.forClass(Simulation.class);
    verify(saveSimulationPort).save(captor.capture());
    assertThat(captor.getValue().getLifecycleStatus()).isEqualTo(SimulationLifecycle.DELETED);
  }

  @Test
  void non_owner_throws_access_denied() {
    UUID simulationId = UUID.randomUUID();
    Simulation sim =
        Simulation.create(
            SimulationId.of(simulationId), ownerId, "Name", periodId, List.of(programId));

    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(sim));

    assertThatThrownBy(() -> sut.deleteSimulation(simulationId, otherUserId))
        .isInstanceOf(SimulationAccessDeniedException.class);
  }

  @Test
  void not_found_throws() {
    UUID simulationId = UUID.randomUUID();
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.deleteSimulation(simulationId, ownerId))
        .isInstanceOf(SimulationNotFoundException.class);
  }
}
