package co.edu.uniquindio.brujula.simulation.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.service.RemoveSimulationSelectionService;
import co.edu.uniquindio.brujula.simulation.domain.CandidateSelection;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Application unit tests for {@link RemoveSimulationSelectionService}.
 *
 * <p>Covers: happy path (soft delete), simulation not found → 404, non-owner → 403, selection not
 * found → 404, selection belongs to different simulation → 404.
 */
class RemoveSimulationSelectionServiceTest {

  private LoadSimulationPort loadSimulationPort;
  private LoadSimulationSelectionPort loadSimulationSelectionPort;
  private SaveSimulationSelectionPort saveSimulationSelectionPort;
  private RemoveSimulationSelectionService sut;

  private final UUID ownerId = UUID.randomUUID();
  private final UUID simulationId = UUID.randomUUID();
  private final UUID selectionId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID courseId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSimulationPort = mock(LoadSimulationPort.class);
    loadSimulationSelectionPort = mock(LoadSimulationSelectionPort.class);
    saveSimulationSelectionPort = mock(SaveSimulationSelectionPort.class);
    sut =
        new RemoveSimulationSelectionService(
            loadSimulationPort, loadSimulationSelectionPort, saveSimulationSelectionPort);
  }

  // -------------------------------------------------------------------------
  // Happy path
  // -------------------------------------------------------------------------

  @Test
  void removeSelection_owner_softDeletesAndSaves() {
    Simulation simulation = buildSimulation(ownerId);
    SimulationSelection selection = buildSelection(simulationId);

    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadById(selectionId)).thenReturn(Optional.of(selection));
    when(saveSimulationSelectionPort.save(any())).thenReturn(selection);

    sut.removeSelection(simulationId, selectionId, ownerId);

    verify(saveSimulationSelectionPort).save(selection);
  }

  // -------------------------------------------------------------------------
  // Error paths
  // -------------------------------------------------------------------------

  @Test
  void removeSelection_simulationNotFound_throws404() {
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.removeSelection(simulationId, selectionId, ownerId))
        .isInstanceOf(SimulationNotFoundException.class);

    verify(saveSimulationSelectionPort, never()).save(any());
  }

  @Test
  void removeSelection_nonOwner_throws403() {
    UUID intruder = UUID.randomUUID();
    Simulation simulation = buildSimulation(ownerId);
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));

    assertThatThrownBy(() -> sut.removeSelection(simulationId, selectionId, intruder))
        .isInstanceOf(SimulationAccessDeniedException.class);

    verify(saveSimulationSelectionPort, never()).save(any());
  }

  @Test
  void removeSelection_selectionNotFound_throws404() {
    Simulation simulation = buildSimulation(ownerId);
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadById(selectionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.removeSelection(simulationId, selectionId, ownerId))
        .isInstanceOf(SimulationSelectionNotFoundException.class);
  }

  @Test
  void removeSelection_selectionBelongsToDifferentSimulation_throws404() {
    UUID otherSimulationId = UUID.randomUUID();
    Simulation simulation = buildSimulation(ownerId);
    SimulationSelection selection = buildSelection(otherSimulationId); // different simulationId

    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadById(selectionId)).thenReturn(Optional.of(selection));

    assertThatThrownBy(() -> sut.removeSelection(simulationId, selectionId, ownerId))
        .isInstanceOf(SimulationSelectionNotFoundException.class);

    verify(saveSimulationSelectionPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private Simulation buildSimulation(UUID owner) {
    return Simulation.reconstitute(
        SimulationId.of(simulationId),
        owner,
        "Test Sim",
        periodId,
        List.of(programId),
        SimulationLifecycle.ACTIVE,
        Instant.now(),
        Instant.now(),
        0L);
  }

  private SimulationSelection buildSelection(UUID ownerSimulationId) {
    CandidateSelection candidate =
        new CandidateSelection(
            UUID.randomUUID(),
            UUID.randomUUID(),
            courseId,
            "Algorithms",
            programId,
            0.0,
            "A",
            "PUBLISHED",
            1L,
            List.of());
    return SimulationSelection.create(
        SimulationSelectionId.of(selectionId), ownerSimulationId, candidate);
  }
}
