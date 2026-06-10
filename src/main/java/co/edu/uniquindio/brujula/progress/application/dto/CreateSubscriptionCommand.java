package co.edu.uniquindio.brujula.progress.application.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Command DTO for creating a subscription.
 *
 * <p>Self-validating (compact constructor). Passed from the REST adapter to {@link
 * co.edu.uniquindio.brujula.progress.application.port.in.CreateSubscriptionUseCase}.
 */
public record CreateSubscriptionCommand(
    UUID idempotencyKey,
    UUID actorId,
    UUID programId,
    UUID planId,
    String referenceJourneyCode,
    String requestHash) {

  public CreateSubscriptionCommand {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(programId, "programId must not be null");
    Objects.requireNonNull(planId, "planId must not be null");
    if (referenceJourneyCode == null || referenceJourneyCode.isBlank()) {
      throw new IllegalArgumentException("referenceJourneyCode must not be blank");
    }
    Objects.requireNonNull(requestHash, "requestHash must not be null");
  }
}
