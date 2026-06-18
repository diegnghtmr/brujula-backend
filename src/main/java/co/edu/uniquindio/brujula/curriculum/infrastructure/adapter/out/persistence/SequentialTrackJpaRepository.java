package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link SequentialTrackJpaEntity}.
 *
 * <p>Package-private.
 */
interface SequentialTrackJpaRepository extends JpaRepository<SequentialTrackJpaEntity, UUID> {

  List<SequentialTrackJpaEntity> findByPlanId(UUID planId);

  List<SequentialTrackJpaEntity> findByPlanIdAndKind(UUID planId, String kind);
}
