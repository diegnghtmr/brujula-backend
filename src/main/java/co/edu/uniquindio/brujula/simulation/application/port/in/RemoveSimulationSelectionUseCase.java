package co.edu.uniquindio.brujula.simulation.application.port.in;

import java.util.UUID;

/**
 * Input port: remove a selection from a simulation (VAL-010).
 *
 * <p>Returns void (HTTP 204). Validates ownership. Non-owner gets 403.
 * Non-existent selection gets 404.
 */
public interface RemoveSimulationSelectionUseCase {

  /**
   * Soft-delete a selection from the simulation.
   *
   * @param simulationId target simulation UUID
   * @param selectionId  the selection to remove
   * @param ownerId      caller UUID (from SecurityContext, BOLA-safe)
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException           if simulation not found (404)
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException       if caller does not own simulation (403)
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionNotFoundException  if selection not found (404)
   */
  void removeSelection(UUID simulationId, UUID selectionId, UUID ownerId);
}
