package co.edu.uniquindio.brujula.simulation.application.port.in;

import co.edu.uniquindio.brujula.simulation.application.dto.SimulationDetailView;
import java.util.UUID;

/** Input port: get full simulation detail by ID (SIM-003, DET-001..003, STALE-001, X-003). */
public interface GetSimulationQuery {

  /**
   * Returns a fully assembled {@link SimulationDetailView} for the simulation owned by
   * {@code callerId}. Includes selections, credit summary, weekly schedule, validation snapshot,
   * and staleness status — all computed at read time (INV-004).
   *
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException if not found
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException if caller
   *     is not the owner
   */
  SimulationDetailView getSimulation(UUID simulationId, UUID callerId);
}
