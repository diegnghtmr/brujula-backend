package co.edu.uniquindio.brujula.progress.domain;

/**
 * Status of a {@link Subscription} aggregate.
 *
 * <p>ACTIVE — the subscription is live. DELETED — soft-deleted; excluded from active counts and
 * availability computation (ADR-F3-003).
 */
public enum SubscriptionStatus {
  ACTIVE,
  DELETED
}
