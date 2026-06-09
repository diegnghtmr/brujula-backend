package co.edu.uniquindio.brujula.imports.application.dto;

import java.util.UUID;

/**
 * Command DTO for confirming a validated ImportBatch (IMP-005).
 *
 * <p>Carries all fields needed for the atomic confirm pipeline. No framework imports.
 *
 * @param importId the ImportBatch UUID to confirm
 * @param actorEmail OIDC-resolved email of the authenticated actor (for STAFF assignment check)
 * @param actorRole role string ("STAFF" or "ADMIN")
 * @param actorId actor UUID for audit
 * @param expectedVersion the optimistic-lock version the caller expects; if the stored version
 *     differs, a 409 OPTIMISTIC_LOCK_CONFLICT is returned (ADR-010)
 * @param idempotencyKey per-request UUID for exactly-once semantics
 * @param requestHash canonical hash of the confirm request for idempotency conflict detection
 */
public record ConfirmImportCommand(
    UUID importId,
    String actorEmail,
    String actorRole,
    UUID actorId,
    long expectedVersion,
    UUID idempotencyKey,
    String requestHash) {}
