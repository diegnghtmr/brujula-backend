package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link CourseJpaEntity}.
 *
 * <p>Package-private.
 */
interface CourseJpaRepository extends JpaRepository<CourseJpaEntity, UUID> {

  List<CourseJpaEntity> findByPlanId(UUID planId);
}
