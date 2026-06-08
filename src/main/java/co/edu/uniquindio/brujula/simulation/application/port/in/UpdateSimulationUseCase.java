package co.edu.uniquindio.brujula.simulation.application.port.in;

import co.edu.uniquindio.brujula.simulation.application.dto.SimulationView;
import co.edu.uniquindio.brujula.simulation.application.dto.UpdateSimulationCommand;

/** Input port: rename/update a simulation with optimistic locking (SIM-004, X-002). */
public interface UpdateSimulationUseCase {

  SimulationView updateSimulation(UpdateSimulationCommand command);
}
