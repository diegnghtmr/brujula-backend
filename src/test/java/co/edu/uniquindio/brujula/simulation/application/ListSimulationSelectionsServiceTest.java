package co.edu.uniquindio.brujula.simulation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.simulation.application.dto.SimulationSelectionView;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.service.ListSimulationSelectionsService;
import co.edu.uniquindio.brujula.simulation.domain.CandidateSelection;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU4a application unit tests for {@link ListSimulationSelectionsService}.
 *
 * <p>Covers: owner gets empty list, owner gets list with 2 items, non-owner → 403, simulation not
 * found → 404.
 */
class ListSimulationSelectionsServiceTest {

  private LoadSimulationPort loadSimulationPort;
  private LoadSimulationSelectionPort loadSimulationSelectionPort;
  private ListSimulationSelectionsService sut;

  private final UUID ownerId = UUID.randomUUID();
  private final UUID simulationId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSimulationPort = mock(LoadSimulationPort.class);
    loadSimulationSelectionPort = mock(LoadSimulationSelectionPort.class);
    sut = new ListSimulationSelectionsService(loadSimulationPort, loadSimulationSelectionPort);
  }

  // -------------------------------------------------------------------------
  // Happy path
  // -------------------------------------------------------------------------

  @Test
  void listSelections_ownerEmptyList_returnsEmptyList() {
    Simulation simulation = buildSimulation(ownerId);
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simulationId)).thenReturn(List.of());

    List<SimulationSelectionView> views = sut.listSelections(simulationId, ownerId);

    assertThat(views).isEmpty();
  }

  @Test
  void listSelections_ownerWithTwoSelections_returnsTwoViews() {
    Simulation simulation = buildSimulation(ownerId);
    SimulationSelection sel1 = buildSelection(simulationId, UUID.randomUUID());
    SimulationSelection sel2 = buildSelection(simulationId, UUID.randomUUID());

    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simulationId))
        .thenReturn(List.of(sel1, sel2));

    List<SimulationSelectionView> views = sut.listSelections(simulationId, ownerId);

    assertThat(views).hasSize(2);
    // Verify warnings are always empty (WU4a)
    views.forEach(v -> assertThat(v.warnings()).isEmpty());
  }

  // -------------------------------------------------------------------------
  // Error paths
  // -------------------------------------------------------------------------

  @Test
  void listSelections_simulationNotFound_throws404() {
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.listSelections(simulationId, ownerId))
        .isInstanceOf(SimulationNotFoundException.class);
  }

  @Test
  void listSelections_nonOwner_throws403() {
    UUID intruder = UUID.randomUUID();
    Simulation simulation = buildSimulation(ownerId);
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));

    assertThatThrownBy(() -> sut.listSelections(simulationId, intruder))
        .isInstanceOf(SimulationAccessDeniedException.class);
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
        List.of(),
        SimulationLifecycle.ACTIVE,
        Instant.now(),
        Instant.now(),
        0L);
  }

  private SimulationSelection buildSelection(UUID ownerSimulationId, UUID courseId) {
    CandidateSelection candidate =
        new CandidateSelection(
            UUID.randomUUID(),
            UUID.randomUUID(),
            courseId,
            "Course " + courseId,
            programId,
            3.0,
            "A",
            "PUBLISHED",
            1L,
            List.of());
    return SimulationSelection.create(
        SimulationSelectionId.generate(), ownerSimulationId, candidate);
  }
}
