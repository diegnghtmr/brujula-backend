package co.edu.uniquindio.brujula.simulation.application.port.in;

import co.edu.uniquindio.brujula.simulation.application.dto.SimulationDetailView;
import java.util.UUID;

/**
 * Input port: re-validate and refresh a simulation (spec STALE-002).
 *
 * <p>Re-runs ALL validations on demand. If no blocking violations persist, refreshes each
 * selection's snapshot and returns a {@link SimulationDetailView} with statusName=CURRENT. If
 * blocking violations remain, throws {@link
 * co.edu.uniquindio.brujula.simulation.domain.SimulationStillBlockedException} → HTTP 409.
 */
public interface ReviewSimulationUseCase {

  /**
   * Review and optionally refresh the simulation.
   *
   * @param simulationId the simulation to review
   * @param callerId the authenticated user (ownership enforced)
   * @return refreshed detail view with statusName=CURRENT on success
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException if not found
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException if
   *     non-owner
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationStillBlockedException if blockers
   *     persist
   */
  SimulationDetailView reviewSimulation(UUID simulationId, UUID callerId);
}
