package co.edu.uniquindio.brujula.simulation.application.port.in;

import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import java.util.UUID;

/** Input port: get a single simulation by ID (SIM-003 skeleton, X-003 ownership). */
public interface GetSimulationQuery {

  /**
   * Returns a basic {@code SimulationView} for the simulation owned by {@code callerId}.
   *
   * <p>WU3: returns a skeleton view. Full SimulationDetail assembly (selections, credits,
   * validation) is deferred to WU5.
   *
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException if not found
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException if caller
   *     is not the owner
   */
  SimulationView getSimulation(UUID simulationId, UUID callerId);
}
