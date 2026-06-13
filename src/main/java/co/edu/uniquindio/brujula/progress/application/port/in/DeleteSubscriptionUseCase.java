package co.edu.uniquindio.brujula.progress.application.port.in;

import java.util.UUID;

/**
 * Input port: soft-delete a subscription.
 *
 * <p>Rejects deletion if it would leave the student with zero active subscriptions.
 */
public interface DeleteSubscriptionUseCase {

  /**
   * Soft-delete the subscription identified by {@code subscriptionId}.
   *
   * @param subscriptionId the subscription UUID
   * @param userId the authenticated student's user ID (ownership check)
   */
  void deleteSubscription(UUID subscriptionId, UUID userId);
}
