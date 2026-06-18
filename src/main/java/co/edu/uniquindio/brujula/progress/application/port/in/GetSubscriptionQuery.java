package co.edu.uniquindio.brujula.progress.application.port.in;

import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import java.util.UUID;

/**
 * Input port: get a single subscription by ID (ownership-guarded).
 *
 * <p>Returns 404 if not found or belongs to a different student (OWASP BOLA).
 */
public interface GetSubscriptionQuery {

  /**
   * Return the subscription identified by {@code subscriptionId}, asserting ownership.
   *
   * @param subscriptionId the subscription UUID
   * @param userId the authenticated student's user ID
   * @return the subscription view
   * @throws co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException if not found or
   *     not owned by userId
   */
  SubscriptionView getSubscription(UUID subscriptionId, UUID userId);
}
