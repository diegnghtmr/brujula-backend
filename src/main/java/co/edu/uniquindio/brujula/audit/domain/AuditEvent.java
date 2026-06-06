package co.edu.uniquindio.brujula.audit.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * AuditEvent aggregate — immutable, append-only.
 *
 * <p>Represents a single recorded audit event. No setters, no update path. Created via factory
 * methods: {@link #record} for new events, {@link #reconstitute} for loading from storage.
 *
 * <p>Satisfies: Design §2.3, Spec §Audit Log (TRD §6.3 Todos→audit, RNF-SEG-007).
 */
public final class AuditEvent {

  private final UUID id;
  private final UUID actorId;
  private final String actorRole;
  private final String action;
  private final String entityType;
  private final UUID entityId;
  private final UUID programId;
  private final OffsetDateTime occurredAt;
  private final String previousValue;
  private final String newValue;

  private AuditEvent(
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

  /**
   * Factory for recording a new audit event. Generates a fresh UUID and captures current time.
   *
   * @param actorId the internal user ID performing the action (required)
   * @param actorRole role string at time of action: STUDENT | STAFF | ADMIN (required)
   * @param action semantic action code, e.g. STAFF_CREATED, LOGIN (required)
   * @param entityType type of the affected entity, e.g. app_user (required)
   * @param entityId PK of the affected entity (nullable)
   * @param programId affected academic program (nullable)
   * @param previousValue JSON snapshot before action, sanitized — no secrets (nullable)
   * @param newValue JSON snapshot after action, sanitized — no secrets (nullable)
   * @return a new immutable AuditEvent
   */
  public static AuditEvent record(
      UUID actorId,
      String actorRole,
      String action,
      String entityType,
      UUID entityId,
      UUID programId,
      String previousValue,
      String newValue) {
    if (actorId == null) {
      throw new IllegalArgumentException("actorId must not be null");
    }
    if (actorRole == null || actorRole.isBlank()) {
      throw new IllegalArgumentException("actorRole must not be null or blank");
    }
    if (action == null || action.isBlank()) {
      throw new IllegalArgumentException("action must not be null or blank");
    }
    if (entityType == null || entityType.isBlank()) {
      throw new IllegalArgumentException("entityType must not be null or blank");
    }
    return new AuditEvent(
        UUID.randomUUID(),
        actorId,
        actorRole,
        action,
        entityType,
        entityId,
        programId,
        OffsetDateTime.now(),
        previousValue,
        newValue);
  }

  /**
   * Reconstitution factory for loading a persisted AuditEvent from storage (mapper use only).
   *
   * @param id the persisted UUID
   * @param occurredAt the original timestamp
   */
  public static AuditEvent reconstitute(
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
    return new AuditEvent(
        id,
        actorId,
        actorRole,
        action,
        entityType,
        entityId,
        programId,
        occurredAt,
        previousValue,
        newValue);
  }

  public UUID getId() {
    return id;
  }

  public UUID getActorId() {
    return actorId;
  }

  public String getActorRole() {
    return actorRole;
  }

  public String getAction() {
    return action;
  }

  public String getEntityType() {
    return entityType;
  }

  public UUID getEntityId() {
    return entityId;
  }

  public UUID getProgramId() {
    return programId;
  }

  public OffsetDateTime getOccurredAt() {
    return occurredAt;
  }

  public String getPreviousValue() {
    return previousValue;
  }

  public String getNewValue() {
    return newValue;
  }
}
