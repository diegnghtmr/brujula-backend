package co.edu.uniquindio.brujula.progress.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionStatus;

/**
 * Mapper: {@link StudentProgramSubscriptionJpaEntity} ↔ {@link Subscription} domain aggregate.
 *
 * <p>Package-private per ARQ-006b. Pure translation — no business logic.
 */
class SubscriptionPersistenceMapper {

  Subscription toDomain(StudentProgramSubscriptionJpaEntity entity) {
    return Subscription.reconstitute(
        SubscriptionId.of(entity.getId()),
        entity.getUserId(),
        entity.getProgramId(),
        entity.getPlanId(),
        entity.getReferenceJourneyCode(),
        SubscriptionStatus.valueOf(entity.getStatus()),
        entity.getCreatedAt(),
        entity.getVersion());
  }

  StudentProgramSubscriptionJpaEntity toJpaEntity(Subscription subscription) {
    StudentProgramSubscriptionJpaEntity entity = new StudentProgramSubscriptionJpaEntity();
    entity.setId(subscription.getId().value());
    entity.setUserId(subscription.getUserId());
    entity.setProgramId(subscription.getProgramId());
    entity.setPlanId(subscription.getPlanId());
    entity.setReferenceJourneyCode(subscription.getReferenceJourneyCode());
    entity.setStatus(subscription.getStatus().name());
    entity.setCreatedAt(subscription.getCreatedAt());
    entity.setVersion(subscription.getVersion());
    return entity;
  }
}
