package co.edu.uniquindio.brujula.progress.domain;

/**
 * Thrown when a plan change is attempted on a subscription that has existing course attempts, but
 * the student has not acknowledged the history impact.
 *
 * <p>Maps to HTTP 409 Conflict in the REST adapter.
 */
public class HistoryImpactNotAcknowledgedException extends RuntimeException {

  private final SubscriptionId subscriptionId;

  public HistoryImpactNotAcknowledgedException(SubscriptionId subscriptionId) {
    super(
        "Plan change for subscription %s requires acknowledgeHistoryImpact=true; existing"
            + " course attempts will be reclassified.".formatted(subscriptionId));
    this.subscriptionId = subscriptionId;
  }

  public SubscriptionId getSubscriptionId() {
    return subscriptionId;
  }
}
