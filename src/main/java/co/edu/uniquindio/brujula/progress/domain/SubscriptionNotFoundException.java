package co.edu.uniquindio.brujula.progress.domain;

import java.util.UUID;

/**
 * Thrown when a subscription cannot be found by ID (or belongs to a different student — BOLA).
 *
 * <p>Maps to HTTP 404 Not Found. Ownership violations return 404 (not 403) to avoid revealing
 * subscription existence to unauthorized callers (OWASP BOLA, ADR-F3-011).
 */
public class SubscriptionNotFoundException extends RuntimeException {

  private final UUID subscriptionId;

  public SubscriptionNotFoundException(UUID subscriptionId) {
    super("Subscription not found: %s".formatted(subscriptionId));
    this.subscriptionId = subscriptionId;
  }

  public UUID getSubscriptionId() {
    return subscriptionId;
  }
}
