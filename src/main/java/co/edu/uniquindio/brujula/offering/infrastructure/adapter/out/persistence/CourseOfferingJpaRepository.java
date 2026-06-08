package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link CourseOfferingJpaEntity}.
 *
 * <p>Package-private — implementation detail, not exported.
 */
interface CourseOfferingJpaRepository extends JpaRepository<CourseOfferingJpaEntity, UUID> {

  /**
   * Returns a page of published offerings (PRELIMINARY or FINAL) for a given period,
   * optionally filtered by programId. No eager fetch needed for summary projection.
   */
  @Query("""
      SELECT o FROM CourseOfferingJpaEntity o
      WHERE o.periodId = :periodId
        AND o.status IN ('PRELIMINARY', 'FINAL')
        AND (:programId IS NULL OR o.programId = :programId)
      """)
  Page<CourseOfferingJpaEntity> findPublished(
      @Param("periodId") UUID periodId,
      @Param("programId") UUID programId,
      Pageable pageable);

  /**
   * Returns the offering with its schedule options and sessions eagerly loaded.
   * Uses JOIN FETCH to avoid N+1 on the nested collections.
   */
  @Query("""
      SELECT DISTINCT o FROM CourseOfferingJpaEntity o
      LEFT JOIN FETCH o.scheduleOptions opt
      LEFT JOIN FETCH opt.sessions
      WHERE o.id = :id
      """)
  Optional<CourseOfferingJpaEntity> findByIdWithOptions(@Param("id") UUID id);
}
