package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link StudyPlanJpaEntity}.
 *
 * <p>Package-private.
 */
interface StudyPlanJpaRepository extends JpaRepository<StudyPlanJpaEntity, UUID> {

  List<StudyPlanJpaEntity> findByProgramId(UUID programId);
}
