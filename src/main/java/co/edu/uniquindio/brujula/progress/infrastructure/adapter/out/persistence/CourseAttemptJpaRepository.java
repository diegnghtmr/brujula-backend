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
 * <p>Package-private per ARQ-006b. Only the persistence adapter accesses this interface.
 */
interface CourseAttemptJpaRepository extends JpaRepository<CourseAttemptJpaEntity, UUID> {

  /**
   * Load all attempts for a subscription (backs the bulk-load pattern, ADR-F3-005 N+1 avoidance).
   */
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
}
