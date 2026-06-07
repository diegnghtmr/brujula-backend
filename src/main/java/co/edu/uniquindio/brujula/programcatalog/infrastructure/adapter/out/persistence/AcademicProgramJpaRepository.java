package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link AcademicProgramJpaEntity}.
 *
 * <p>Package-private per ARQ-006. Only the persistence adapter layer accesses this directly.
 */
interface AcademicProgramJpaRepository extends JpaRepository<AcademicProgramJpaEntity, UUID> {
}
