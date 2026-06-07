package co.edu.uniquindio.brujula.progress.domain;

import java.util.UUID;

/**
 * Thrown when a student attempts to create a 3rd active subscription (cap = 2, per contract).
 *
 * <p>Maps to HTTP 409 Conflict. Contract alignment: {@code maxItems: 2} on listMySubscriptions
 * response, POST description states "Máximo dos suscripciones activas" (observation #1630).
 */
public class MaxSubscriptionsExceededException extends RuntimeException {

  private final UUID userId;
  private final int activeCount;

  public MaxSubscriptionsExceededException(UUID userId, int activeCount) {
    super(
        "Student %s has reached the maximum of 2 active subscriptions (current: %d)."
            .formatted(userId, activeCount));
    this.userId = userId;
    this.activeCount = activeCount;
  }

  public UUID getUserId() {
    return userId;
  }

  public int getActiveCount() {
    return activeCount;
  }
}
