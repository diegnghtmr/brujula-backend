package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link DegreeWorkModalityJpaEntity}.
 *
 * <p>Package-private.
 */
interface DegreeWorkModalityJpaRepository extends JpaRepository<DegreeWorkModalityJpaEntity, UUID> {

  List<DegreeWorkModalityJpaEntity> findByPlanId(UUID planId);
}
