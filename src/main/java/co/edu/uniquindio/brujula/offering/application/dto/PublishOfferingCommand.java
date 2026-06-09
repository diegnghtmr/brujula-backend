package co.edu.uniquindio.brujula.offering.application.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Command DTO for publishing (state-transitioning) a course offering.
 *
 * <p>Carries the idempotency key, target status, optimistic-lock version, and the optional
 * intensity-warning acknowledgement flag. Framework-free — plain record.
 *
 * <p>Satisfies: OFFW-005.
 *
 * @param idempotencyKey idempotency key UUID from the Idempotency-Key header
 * @param offeringId offering UUID to publish/transition
 * @param actorId UUID of the authenticated STAFF/ADMIN caller
 * @param targetStatus desired new status string (PRELIMINARY, FINAL, or ARCHIVED)
 * @param expectedVersion expected optimistic-lock version; mismatch → 409
 * @param acknowledgeIntensityWarning caller-supplied flag; ADVISORY only — never blocks
 * @param requestHash SHA-256 hex digest of the request body for idempotency
 */
public record PublishOfferingCommand(
    UUID idempotencyKey,
    UUID offeringId,
    UUID actorId,
    String targetStatus,
    long expectedVersion,
    boolean acknowledgeIntensityWarning,
    String requestHash) {

  public PublishOfferingCommand {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    Objects.requireNonNull(offeringId, "offeringId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(targetStatus, "targetStatus must not be null");
    Objects.requireNonNull(requestHash, "requestHash must not be null");
  }
}
