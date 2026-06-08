package co.edu.uniquindio.brujula.simulation.application.port.in;

import co.edu.uniquindio.brujula.simulation.application.dto.AddSelectionCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationSelectionView;

/**
 * Input port: add a schedule option selection to a simulation (VAL-001, spec WU4a).
 *
 * <p>Idempotent: same Idempotency-Key replays the stored 201 response.
 * Blocking validation fails with {@link co.edu.uniquindio.brujula.simulation.domain.SimulationValidationBlockedException}.
 */
public interface AddSimulationSelectionUseCase {

  /**
   * Add a schedule option to the simulation, running all WU4a validation rules.
   *
   * @param command the add selection command
   * @return the persisted selection view (with warnings in the body — empty in WU4a)
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException    if simulation not found (404)
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException if caller does not own the simulation (403)
   * @throws co.edu.uniquindio.brujula.simulation.domain.SimulationValidationBlockedException if blocking validation fires (409)
   * @throws co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException if key reused with different body (409)
   */
  SimulationSelectionView addSelection(AddSelectionCommand command);
}
