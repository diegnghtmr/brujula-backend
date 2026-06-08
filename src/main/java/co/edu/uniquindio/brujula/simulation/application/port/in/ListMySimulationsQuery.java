package co.edu.uniquindio.brujula.simulation.application.port.in;

import co.edu.uniquindio.brujula.simulation.application.dto.ListSimulationsCriteria;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationPageView;

/** Input port: list the caller's simulations with optional filters (SIM-002). */
public interface ListMySimulationsQuery {

  SimulationPageView listMySimulations(ListSimulationsCriteria criteria);
}
