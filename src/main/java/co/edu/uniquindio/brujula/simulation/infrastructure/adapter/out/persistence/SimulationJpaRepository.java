package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link SimulationJpaEntity}. Package-private. */
interface SimulationJpaRepository extends JpaRepository<SimulationJpaEntity, UUID> {

  Page<SimulationJpaEntity> findByOwnerIdAndStatus(UUID ownerId, String status, Pageable pageable);

  Page<SimulationJpaEntity> findByOwnerIdAndStatusAndPeriodId(
      UUID ownerId, String status, UUID periodId, Pageable pageable);

  long countByOwnerIdAndStatus(UUID ownerId, String status);

  long countByOwnerIdAndStatusAndPeriodId(UUID ownerId, String status, UUID periodId);
}
