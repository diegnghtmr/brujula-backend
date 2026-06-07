package co.edu.uniquindio.brujula.progress.domain;

import java.util.UUID;

/**
 * Thrown when a student attempts to delete their last active subscription.
 *
 * <p>Contract: "Debe quedar al menos una suscripción activa". Maps to HTTP 409 Conflict.
 */
public class LastActiveSubscriptionException extends RuntimeException {

  private final UUID userId;

  public LastActiveSubscriptionException(UUID userId) {
    super("Cannot delete the last active subscription for student %s.".formatted(userId));
    this.userId = userId;
  }

  public UUID getUserId() {
    return userId;
  }
}
