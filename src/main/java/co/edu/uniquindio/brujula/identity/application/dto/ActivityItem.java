package co.edu.uniquindio.brujula.identity.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single activity entry returned to the API caller.
 *
 * <p>Lives in {@code application.dto} (not {@code application.port.in}) because port.in must
 * contain interfaces only.
 *
 * @param id event UUID
 * @param actorId user UUID who performed the action
 * @param actorRole string role at time of event
 * @param action action code (e.g. USER_LOGGED_IN)
 * @param entityType entity type string
 * @param entityId optional entity UUID
 * @param programId optional program UUID
 * @param occurredAt timestamp in UTC
 */
public record ActivityItem(
    UUID id,
    UUID actorId,
    String actorRole,
    String action,
    String entityType,
    UUID entityId,
    UUID programId,
    OffsetDateTime occurredAt) {}
