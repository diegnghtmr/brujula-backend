package co.edu.uniquindio.brujula.administration.application;

/**
 * Test-scope marker: thrown when the same idempotency key is reused with a different request body.
 *
 * <p>Production code throws {@link
 * co.edu.uniquindio.brujula.administration.application.service.IdempotencyKeyConflictException}.
 */
public class IdempotencyConflictException extends RuntimeException {
  public IdempotencyConflictException(String message) {
    super(message);
  }
}
