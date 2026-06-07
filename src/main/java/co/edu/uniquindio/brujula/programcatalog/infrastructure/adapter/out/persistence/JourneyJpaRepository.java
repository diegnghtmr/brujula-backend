package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link JourneyJpaEntity}.
 *
 * <p>Package-private per ARQ-006.
 */
interface JourneyJpaRepository extends JpaRepository<JourneyJpaEntity, UUID> {

  List<JourneyJpaEntity> findByActiveTrue();
}
