package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link SimulationSelectionJpaEntity}. Package-private. */
interface SimulationSelectionJpaRepository
    extends JpaRepository<SimulationSelectionJpaEntity, UUID> {

  /**
   * Load all selections for a simulation with a given lifecycle status.
   *
   * @param simulationId the simulation UUID
   * @param status the lifecycle status (e.g. "ACTIVE")
   * @return list of matching entities
   */
  List<SimulationSelectionJpaEntity> findBySimulationIdAndStatus(UUID simulationId, String status);
}
