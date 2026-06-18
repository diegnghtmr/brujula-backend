package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for {@link SimulationScopeJpaEntity}.
 *
 * <p>Package-private per ARQ-006b.
 */
interface SimulationScopeJpaRepository
    extends JpaRepository<SimulationScopeJpaEntity, SimulationScopeJpaEntity.SimulationScopePk> {

  /** Load all scope rows for the given simulation. */
  List<SimulationScopeJpaEntity> findByIdSimulationId(UUID simulationId);

  /** Delete all scope rows for the given simulation (used before re-inserting on update). */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM SimulationScopeJpaEntity s WHERE s.id.simulationId = :simulationId")
  void deleteBySimulationId(@Param("simulationId") UUID simulationId);
}
