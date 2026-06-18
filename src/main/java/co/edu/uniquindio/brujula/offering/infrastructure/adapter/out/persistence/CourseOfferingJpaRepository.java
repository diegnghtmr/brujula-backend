package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link CourseOfferingJpaEntity}.
 *
 * <p>Package-private — implementation detail, not exported.
 */
interface CourseOfferingJpaRepository extends JpaRepository<CourseOfferingJpaEntity, UUID> {

  /**
   * Returns a page of published offerings (PRELIMINARY or FINAL) for a given period, optionally
   * filtered by programId and/or journeyCodes. No eager fetch needed for summary projection.
   *
   * <p>Journey filter: when {@code journeyFilterDisabled} is {@code true} no journey restriction is
   * applied. When {@code false}, only offerings that have at least one schedule option whose {@code
   * journeyCode} is in {@code journeyCodes} are returned. The {@code journeyCodes} parameter must
   * always be a non-null, non-empty collection when {@code journeyFilterDisabled = false} to avoid
   * an invalid JPQL {@code IN ()} clause.
   */
  @Query(
      """
      SELECT o FROM CourseOfferingJpaEntity o
      WHERE o.periodId = :periodId
        AND o.status IN ('PRELIMINARY', 'FINAL')
        AND (:programId IS NULL OR o.programId = :programId)
        AND (:journeyFilterDisabled = true
             OR EXISTS (SELECT 1 FROM ScheduleOptionJpaEntity so
                        WHERE so.offering = o
                          AND so.journeyCode IN :journeyCodes))
      """)
  Page<CourseOfferingJpaEntity> findPublished(
      @Param("periodId") UUID periodId,
      @Param("programId") UUID programId,
      @Param("journeyFilterDisabled") boolean journeyFilterDisabled,
      @Param("journeyCodes") Collection<String> journeyCodes,
      Pageable pageable);

  /**
   * Returns the offering with its schedule options and sessions eagerly loaded. Uses JOIN FETCH to
   * avoid N+1 on the nested collections.
   */
  @Query(
      """
      SELECT DISTINCT o FROM CourseOfferingJpaEntity o
      LEFT JOIN FETCH o.scheduleOptions opt
      LEFT JOIN FETCH opt.sessions
      WHERE o.id = :id
      """)
  Optional<CourseOfferingJpaEntity> findByIdWithOptions(@Param("id") UUID id);

  /**
   * Returns a page of offerings for privileged callers (STAFF/ADMIN). Includes DRAFT, PRELIMINARY,
   * and FINAL statuses. When {@code statusFilter} is provided, only that status is returned.
   * ARCHIVED is always excluded from staff listing. Optionally filtered by journeyCodes.
   *
   * <p>Journey filter: same contract as {@link #findPublished}.
   *
   * <p>Satisfies: OFFW-007.
   */
  @Query(
      """
      SELECT o FROM CourseOfferingJpaEntity o
      WHERE o.periodId = :periodId
        AND o.status != 'ARCHIVED'
        AND (:programId IS NULL OR o.programId = :programId)
        AND (:statusFilter IS NULL OR o.status = :statusFilter)
        AND (:journeyFilterDisabled = true
             OR EXISTS (SELECT 1 FROM ScheduleOptionJpaEntity so
                        WHERE so.offering = o
                          AND so.journeyCode IN :journeyCodes))
      """)
  Page<CourseOfferingJpaEntity> findForStaff(
      @Param("periodId") UUID periodId,
      @Param("programId") UUID programId,
      @Param("statusFilter") String statusFilter,
      @Param("journeyFilterDisabled") boolean journeyFilterDisabled,
      @Param("journeyCodes") Collection<String> journeyCodes,
      Pageable pageable);
}
