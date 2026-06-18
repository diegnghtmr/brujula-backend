package co.edu.uniquindio.brujula.audit.application.dto;

import java.util.UUID;

/**
 * Command DTO for recording an audit event.
 *
 * <p>Lives in {@code application.dto} (not port.in) because port.in must contain interfaces only.
 *
 * @param actorId the internal user ID performing the action
 * @param actorRole role at time of action: STUDENT | STAFF | ADMIN
 * @param action semantic action code, e.g. STAFF_CREATED, LOGIN
 * @param entityType type of the affected entity
 * @param entityId optional PK of the affected entity
 * @param programId optional affected academic program
 * @param previousValue optional JSON snapshot before action (sanitized)
 * @param newValue optional JSON snapshot after action (sanitized)
 */
public record RecordAuditEventCommand(
    UUID actorId,
    String actorRole,
    String action,
    String entityType,
    UUID entityId,
    UUID programId,
    String previousValue,
    String newValue) {}
