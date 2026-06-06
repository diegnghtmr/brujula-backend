package co.edu.uniquindio.brujula.audit.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.audit.domain.AuditEvent;

/**
 * Stateless mapper between {@link AuditEvent} domain and {@link AuditEventJpaEntity}.
 *
 * <p>Package-private; only the persistence adapter uses it.
 */
final class AuditEventMapper {

  private AuditEventMapper() {}

  static AuditEventJpaEntity toEntity(AuditEvent event) {
    return new AuditEventJpaEntity(
        event.getId(),
        event.getActorId(),
        event.getActorRole(),
        event.getAction(),
        event.getEntityType(),
        event.getEntityId(),
        event.getProgramId(),
        event.getOccurredAt(),
        event.getPreviousValue(),
        event.getNewValue());
  }

  static AuditEvent toDomain(AuditEventJpaEntity entity) {
    return AuditEvent.reconstitute(
        entity.getId(),
        entity.getActorId(),
        entity.getActorRole(),
        entity.getAction(),
        entity.getEntityType(),
        entity.getEntityId(),
        entity.getProgramId(),
        entity.getOccurredAt(),
        entity.getPreviousValue(),
        entity.getNewValue());
  }
}
