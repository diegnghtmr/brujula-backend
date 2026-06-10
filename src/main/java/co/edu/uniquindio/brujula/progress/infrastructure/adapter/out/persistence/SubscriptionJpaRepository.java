package co.edu.uniquindio.brujula.progress.infrastructure.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for {@link StudentProgramSubscriptionJpaEntity}.
 *
 * <p>Package-private per ARQ-006b. Only the {@link SubscriptionPersistenceAdapter} depends on this.
 */
interface SubscriptionJpaRepository
    extends JpaRepository<StudentProgramSubscriptionJpaEntity, UUID> {

  List<StudentProgramSubscriptionJpaEntity> findByUserIdAndStatus(UUID userId, String status);

  /**
   * Acquires a row-level {@code SELECT ... FOR UPDATE} lock on all ACTIVE subscriptions of a
   * student.
   *
   * <p>FIX-W1 (TOCTOU): called inside the same {@code @Transactional} create flow, BEFORE counting.
   * Any concurrent transaction trying to INSERT a new ACTIVE row for the same userId will block
   * until this transaction commits or rolls back, serialising concurrent creates and preventing the
   * cap=2 invariant from being violated under load.
   *
   * @param userId the student whose active subscriptions are locked
   * @return the locked entities (count = current active subscriptions for this user)
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT s FROM StudentProgramSubscriptionJpaEntity s"
          + " WHERE s.userId = :userId AND s.status = 'ACTIVE'")
  List<StudentProgramSubscriptionJpaEntity> lockActiveByUserIdForUpdate(
      @Param("userId") UUID userId);

  @Query(
      "SELECT COUNT(s) FROM StudentProgramSubscriptionJpaEntity s"
          + " WHERE s.userId = :userId AND s.status = 'ACTIVE'")
  int countActiveByUserId(@Param("userId") UUID userId);

  @Query(
      "SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END"
          + " FROM StudentProgramSubscriptionJpaEntity s"
          + " WHERE s.userId = :userId AND s.programId = :programId AND s.status = 'ACTIVE'")
  boolean existsActiveByUserIdAndProgramId(
      @Param("userId") UUID userId, @Param("programId") UUID programId);

  @Query(
      "SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END"
          + " FROM CourseAttemptJpaEntity a"
          + " WHERE a.subscriptionId = :subscriptionId")
  boolean existsAttemptBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);
}
