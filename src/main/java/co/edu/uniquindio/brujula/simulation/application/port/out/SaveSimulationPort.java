package co.edu.uniquindio.brujula.simulation.application.port.out;

import co.edu.uniquindio.brujula.simulation.domain.Simulation;

/** Output port: persist a simulation. Returns the managed entity (carries DB-updated version). */
public interface SaveSimulationPort {

  Simulation save(Simulation simulation);
}
