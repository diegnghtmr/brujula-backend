package co.edu.uniquindio.brujula.progress.domain;

import java.util.UUID;

/**
 * Thrown when a student attempts to subscribe to a 3rd distinct program (cap = 2 distinct
 * programs per student, per contract).
 *
 * <p>Within each program, both jornadas (Diurna and Nocturna) are allowed as separate
 * subscriptions. The cap applies only to distinct programs (careers).
 *
 * <p>Maps to HTTP 409 Conflict.
 */
public class MaxSubscriptionsExceededException extends RuntimeException {

  private final UUID userId;
  private final int activeCount;

  public MaxSubscriptionsExceededException(UUID userId, int activeCount) {
    super(
        "Student %s has reached the maximum of 2 distinct active programs (current distinct: %d)."
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
