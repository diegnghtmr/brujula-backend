package co.edu.uniquindio.brujula.sharedkernel.application.exception;

import java.util.UUID;

/**
 * Thrown when an idempotency key is reused with a different request body (409 Conflict).
 *
 * <p>Shared across all modules that use the {@code IdempotencyStorePort} (administration,
 * progress, and future modules). Previously duplicated in each module's local package; this
 * canonical copy lives in {@code sharedkernel} so no module-to-module dependency is needed.
 *
 * <p>FIX-S1: consolidates {@code administration.application.service.IdempotencyKeyConflictException}
 * and {@code progress.application.service.ProgressIdempotencyKeyConflictException} into a single
 * shared type. Satisfies: Spec §Idempotent Create, TRD §13.
 *
 * <p>No Spring/JPA imports (sharedkernel has {@code allowedDependencies = {}}).
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
