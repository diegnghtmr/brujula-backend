package co.edu.uniquindio.brujula.audit.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Application-layer DTO for a single audit event record.
 *
 * <p>Used in read-side queries. Keeps the REST adapter free of domain type imports.
 */
public record AuditEventItem(
    UUID id,
    UUID actorId,
    String actorRole,
    String action,
    String entityType,
    UUID entityId,
    UUID programId,
    String previousValue,
    String newValue,
    OffsetDateTime occurredAt) {}
