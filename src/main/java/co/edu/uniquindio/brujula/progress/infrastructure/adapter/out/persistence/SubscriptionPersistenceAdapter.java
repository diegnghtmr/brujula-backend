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
 * <p>Implements {@link LoadSubscriptionPort} and {@link SaveSubscriptionPort}. Package-private per
 * ARQ-006b. Optimistic locking is handled by JPA {@code @Version}; Spring translates {@code
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
  @Transactional(readOnly = true)
  public boolean existsActiveByUserIdAndProgramId(UUID userId, UUID programId) {
    return jpaRepository.existsActiveByUserIdAndProgramId(userId, programId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasAttempts(UUID subscriptionId) {
    return jpaRepository.existsAttemptBySubscriptionId(subscriptionId);
  }

  @Override
  public List<Subscription> lockActiveByUserIdForUpdate(UUID userId) {
    // FIX-W1: acquires SELECT FOR UPDATE on all ACTIVE rows for userId.
    // Must be called within an existing @Transactional write transaction (the class-level
    // @Transactional annotation on this adapter satisfies that requirement).
    return jpaRepository.lockActiveByUserIdForUpdate(userId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Subscription save(Subscription subscription) {
    StudentProgramSubscriptionJpaEntity entity = mapper.toJpaEntity(subscription);
    // Use the RETURNED managed entity — JPA increments @Version at flush time.
    // The passed-in entity's version field is stale after an UPDATE; only the returned
    // managed entity carries the post-increment value (FIX-W4).
    StudentProgramSubscriptionJpaEntity managed = jpaRepository.save(entity);
    jpaRepository.flush();
    return mapper.toDomain(managed);
  }
}
