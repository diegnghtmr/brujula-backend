package co.edu.uniquindio.brujula.audit.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity mapping to {@code audit.audit_event} (created by Flyway V004).
 *
 * <p>Append-only: no {@code @Version} column (immutable by design), no updatable fields. JPA
 * cascade operations are limited to persist; update/delete are never called by the adapter.
 */
@Entity
@Table(name = "audit_event", schema = "audit")
class AuditEventJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "actor_id", nullable = false, updatable = false)
  private UUID actorId;

  @Column(name = "actor_role", nullable = false, updatable = false)
  private String actorRole;

  @Column(name = "action", nullable = false, updatable = false)
  private String action;

  @Column(name = "entity_type", nullable = false, updatable = false)
  private String entityType;

  @Column(name = "entity_id", updatable = false)
  private UUID entityId;

  @Column(name = "program_id", updatable = false)
  private UUID programId;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private OffsetDateTime occurredAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "previous_value", columnDefinition = "jsonb", updatable = false)
  private String previousValue;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "new_value", columnDefinition = "jsonb", updatable = false)
  private String newValue;

  /** JPA no-arg constructor. */
  protected AuditEventJpaEntity() {}

  AuditEventJpaEntity(
      UUID id,
      UUID actorId,
      String actorRole,
      String action,
      String entityType,
      UUID entityId,
      UUID programId,
      OffsetDateTime occurredAt,
      String previousValue,
      String newValue) {
    this.id = id;
    this.actorId = actorId;
    this.actorRole = actorRole;
    this.action = action;
    this.entityType = entityType;
    this.entityId = entityId;
    this.programId = programId;
    this.occurredAt = occurredAt;
    this.previousValue = previousValue;
    this.newValue = newValue;
  }

  UUID getId() {
    return id;
  }

  UUID getActorId() {
    return actorId;
  }

  String getActorRole() {
    return actorRole;
  }

  String getAction() {
    return action;
  }

  String getEntityType() {
    return entityType;
  }

  UUID getEntityId() {
    return entityId;
  }

  UUID getProgramId() {
    return programId;
  }

  OffsetDateTime getOccurredAt() {
    return occurredAt;
  }

  String getPreviousValue() {
    return previousValue;
  }

  String getNewValue() {
    return newValue;
  }
}
