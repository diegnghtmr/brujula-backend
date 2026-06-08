package co.edu.uniquindio.brujula.simulation.application.port.in;

import co.edu.uniquindio.brujula.simulation.application.dto.CreateSimulationCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;

/** Input port: create a simulation (SIM-001, X-001 idempotent). */
public interface CreateSimulationUseCase {

  SimulationView createSimulation(CreateSimulationCommand command);
}
