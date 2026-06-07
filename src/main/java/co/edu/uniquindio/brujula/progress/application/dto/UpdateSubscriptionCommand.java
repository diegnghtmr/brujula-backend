package co.edu.uniquindio.brujula.progress.application.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Command DTO for updating a subscription (plan and/or journey change).
 *
 * <p>Self-validating (compact constructor). Includes {@code acknowledgeHistoryImpact} flag.
 */
public record UpdateSubscriptionCommand(
    UUID subscriptionId,
    UUID actorId,
    UUID newPlanId,
    String newReferenceJourneyCode,
    boolean acknowledgeHistoryImpact,
    long version) {

  public UpdateSubscriptionCommand {
    Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
  }
}
