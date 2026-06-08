package co.edu.uniquindio.brujula.simulation.application.port.out;

import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Output port: load simulations from persistence. */
public interface LoadSimulationPort {

  Optional<Simulation> loadById(UUID simulationId);

  /**
   * Load all ACTIVE simulations for an owner, with optional period filter.
   *
   * @param ownerId owner UUID (BOLA scope)
   * @param periodId optional period filter (null = no filter)
   * @param page 0-based page number
   * @param size page size
   * @return list of matching simulations
   */
  List<Simulation> loadByOwner(UUID ownerId, UUID periodId, int page, int size);

  long countByOwner(UUID ownerId, UUID periodId);
}
