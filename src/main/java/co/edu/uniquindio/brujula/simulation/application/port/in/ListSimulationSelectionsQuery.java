package co.edu.uniquindio.brujula.simulation.application.port.in;

import co.edu.uniquindio.brujula.simulation.application.dto.SimulationSelectionView;
import java.util.List;
import java.util.UUID;

/**
 * Input port: list all active selections for a simulation (VAL-011).
 *
 * <p>Returns HTTP 200 with an array. Non-owner gets 403. Non-existent simulation gets 404.
 */
public interface ListSimulationSelectionsQuery {

  /**
   * List active selections for the given simulation.
   *
   * @param simulationId target simulation UUID
   * @param ownerId caller UUID (from SecurityContext, BOLA-safe)
   * @return list of selection views (may be empty)
   */
  List<SimulationSelectionView> listSelections(UUID simulationId, UUID ownerId);
}
