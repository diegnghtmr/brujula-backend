package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link AcademicPeriodJpaEntity}.
 *
 * <p>Package-private — implementation detail, not exported.
 */
interface AcademicPeriodJpaRepository extends JpaRepository<AcademicPeriodJpaEntity, UUID> {

  /** Returns all periods whose status is NOT 'ARCHIVED', ordered by start_date descending. */
  List<AcademicPeriodJpaEntity> findByStatusNotOrderByStartDateDesc(String status);
}
