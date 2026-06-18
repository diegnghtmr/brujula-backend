package co.edu.uniquindio.brujula.progress.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Driven adapter: JPA-backed persistence for subscriptions.
 *
 * <p>Implements {@link LoadSubscriptionPort} and {@link SaveSubscriptionPort}. Package-private.
 * Optimistic locking is handled by JPA {@code @Version}; Spring translates {@code
 * OptimisticLockException} into {@code OptimisticLockingFailureException} which the REST exception
 * handler maps to 409.
 */
@Component
@Transactional
class SubscriptionPersistenceAdapter implements LoadSubscriptionPort, SaveSubscriptionPort {

  private final SubscriptionJpaRepository jpaRepository;
  private final SubscriptionPersistenceMapper mapper;

  SubscriptionPersistenceAdapter(SubscriptionJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = new SubscriptionPersistenceMapper();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Subscription> loadById(UUID subscriptionId) {
    return jpaRepository.findById(subscriptionId).map(mapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Subscription> loadActiveByUserId(UUID userId) {
    return jpaRepository.findByUserIdAndStatus(userId, "ACTIVE").stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public int countActiveByUserId(UUID userId) {
    return jpaRepository.countActiveByUserId(userId);
  }

  @Override
  public boolean existsActiveByUserIdAndProgramIdAndJourneyCode(
      UUID userId, UUID programId, String journeyCode) {
    return jpaRepository.existsActiveByUserIdAndProgramIdAndReferenceJourneyCode(
        userId, programId, journeyCode);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasAttempts(UUID subscriptionId) {
    return jpaRepository.existsAttemptBySubscriptionId(subscriptionId);
  }

  @Override
  @Transactional(readOnly = false)
  public List<Subscription> lockActiveByUserIdForUpdate(UUID userId) {
    // Acquires SELECT FOR UPDATE on all ACTIVE rows for userId.
    // The write lock is only effective when held inside an active read-write transaction.
    // The CALLER (CreateSubscriptionService) is responsible for opening that transaction;
    // this adapter method joins it (REQUIRED propagation) but does not create it alone.
    return jpaRepository.lockActiveByUserIdForUpdate(userId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Subscription save(Subscription subscription) {
    StudentProgramSubscriptionJpaEntity entity = mapper.toJpaEntity(subscription);
    // Use the RETURNED managed entity — JPA increments @Version at flush time.
    // The passed-in entity's version field is stale after an UPDATE; only the returned
    // managed entity carries the post-increment value.
    StudentProgramSubscriptionJpaEntity managed = jpaRepository.save(entity);
    jpaRepository.flush();
    return mapper.toDomain(managed);
  }
}
