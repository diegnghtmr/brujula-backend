package co.edu.uniquindio.brujula.simulation.application.port.out;

import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;

/**
 * Output port: persist a simulation selection.
 *
 * <p>Named by need. Implemented by {@code SimulationPersistenceAdapter}.
 */
public interface SaveSimulationSelectionPort {

  /**
   * Persist a new or updated simulation selection.
   *
   * @param selection the selection to save
   * @return the saved selection with any DB-managed fields updated (version)
   */
  SimulationSelection save(SimulationSelection selection);
}
