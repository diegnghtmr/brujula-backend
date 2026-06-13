package co.edu.uniquindio.brujula.simulation.application.port.out;

import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: load simulation selections from persistence.
 *
 * <p>Named by need. Implemented by {@code SimulationPersistenceAdapter}.
 */
public interface LoadSimulationSelectionPort {

  /**
   * Load a single active selection by ID.
   *
   * @param selectionId the selection UUID
   * @return optional containing the selection, or empty if not found or soft-deleted
   */
  Optional<SimulationSelection> loadById(UUID selectionId);

  /**
   * Load all active selections for a simulation.
   *
   * @param simulationId the simulation UUID
   * @return list of active selections (may be empty)
   */
  List<SimulationSelection> loadActiveBySimulationId(UUID simulationId);
}
