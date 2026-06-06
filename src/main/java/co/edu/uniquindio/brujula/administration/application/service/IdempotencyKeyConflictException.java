package co.edu.uniquindio.brujula.administration.application.service;

import java.util.UUID;

/**
 * Thrown when an idempotency key is reused with a different request body (409 Conflict).
 *
 * <p>Satisfies: Spec §Idempotent Create (scenario "Duplicate idempotency key + different body
 * returns 409"), TRD §13.
 */
public class IdempotencyKeyConflictException extends RuntimeException {

  private final UUID idempotencyKey;
  private final String operation;

  public IdempotencyKeyConflictException(UUID idempotencyKey, String operation) {
    super(
        "Idempotency key %s for operation %s already used with a different request body"
            .formatted(idempotencyKey, operation));
    this.idempotencyKey = idempotencyKey;
    this.operation = operation;
  }

  public UUID getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getOperation() {
    return operation;
  }
}
