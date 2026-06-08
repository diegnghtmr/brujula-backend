package co.edu.uniquindio.brujula.simulation.application.port.in;

import co.edu.uniquindio.brujula.simulation.application.dto.DuplicateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;

/** Input port: duplicate a simulation (SIM-006, X-001 idempotent). */
public interface DuplicateSimulationUseCase {

  SimulationView duplicateSimulation(DuplicateSimulationCommand command);
}
