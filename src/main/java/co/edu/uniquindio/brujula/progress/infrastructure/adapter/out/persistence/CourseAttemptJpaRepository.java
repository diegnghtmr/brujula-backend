package co.edu.uniquindio.brujula.progress.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for {@link CourseAttemptJpaEntity}.
 *
 * <p>Package-private. Only the persistence adapter accesses this interface.
 */
interface CourseAttemptJpaRepository extends JpaRepository<CourseAttemptJpaEntity, UUID> {

  /** Load all attempts for a subscription (backs the bulk-load pattern; N+1 avoidance). */
  List<CourseAttemptJpaEntity> findBySubscriptionId(UUID subscriptionId);

  /**
   * Load attempts for a subscription, filtered by courseId (nullable). When courseId is null,
   * returns all attempts for the subscription.
   */
  @Query(
      "SELECT a FROM CourseAttemptJpaEntity a "
          + "WHERE a.subscriptionId = :subscriptionId "
          + "AND (:courseId IS NULL OR a.courseId = :courseId)")
  List<CourseAttemptJpaEntity> findBySubscriptionIdAndOptionalCourseId(
      @Param("subscriptionId") UUID subscriptionId,
      @Param("courseId") UUID courseId,
      Pageable pageable);

  /** Count attempts for a subscription, filtered by courseId (nullable). */
  @Query(
      "SELECT COUNT(a) FROM CourseAttemptJpaEntity a "
          + "WHERE a.subscriptionId = :subscriptionId "
          + "AND (:courseId IS NULL OR a.courseId = :courseId)")
  long countBySubscriptionIdAndOptionalCourseId(
      @Param("subscriptionId") UUID subscriptionId, @Param("courseId") UUID courseId);

  // ---------------------------------------------------------------------------
  // Program-level (cross-journey) reads: aggregate attempts across all ACTIVE
  // subscriptions of the same (user, program). Academic history is shared across
  // journeys (DIURNA/NOCTURNA) because a grade belongs to the program, not the journey.
  // ---------------------------------------------------------------------------

  /** Load all attempts across the student's ACTIVE subscriptions for a program. */
  @Query(
      "SELECT a FROM CourseAttemptJpaEntity a "
          + "WHERE a.subscriptionId IN ("
          + "SELECT s.id FROM StudentProgramSubscriptionJpaEntity s "
          + "WHERE s.userId = :userId AND s.programId = :programId AND s.status = 'ACTIVE')")
  List<CourseAttemptJpaEntity> findByUserIdAndProgramId(
      @Param("userId") UUID userId, @Param("programId") UUID programId);

  /** Paged load across the student's ACTIVE subscriptions for a program, optional courseId. */
  @Query(
      "SELECT a FROM CourseAttemptJpaEntity a "
          + "WHERE a.subscriptionId IN ("
          + "SELECT s.id FROM StudentProgramSubscriptionJpaEntity s "
          + "WHERE s.userId = :userId AND s.programId = :programId AND s.status = 'ACTIVE') "
          + "AND (:courseId IS NULL OR a.courseId = :courseId) "
          + "ORDER BY a.registeredAt DESC")
  List<CourseAttemptJpaEntity> findByUserIdAndProgramIdAndOptionalCourseId(
      @Param("userId") UUID userId,
      @Param("programId") UUID programId,
      @Param("courseId") UUID courseId,
      Pageable pageable);

  /** Count across the student's ACTIVE subscriptions for a program, optional courseId. */
  @Query(
      "SELECT COUNT(a) FROM CourseAttemptJpaEntity a "
          + "WHERE a.subscriptionId IN ("
          + "SELECT s.id FROM StudentProgramSubscriptionJpaEntity s "
          + "WHERE s.userId = :userId AND s.programId = :programId AND s.status = 'ACTIVE') "
          + "AND (:courseId IS NULL OR a.courseId = :courseId)")
  long countByUserIdAndProgramIdAndOptionalCourseId(
      @Param("userId") UUID userId,
      @Param("programId") UUID programId,
      @Param("courseId") UUID courseId);
}
