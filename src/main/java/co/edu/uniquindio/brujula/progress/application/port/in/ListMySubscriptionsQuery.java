package co.edu.uniquindio.brujula.progress.application.port.in;

import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import java.util.List;
import java.util.UUID;

/**
 * Input port: list the authenticated student's subscriptions.
 *
 * <p>Returns only ACTIVE subscriptions (soft-deleted excluded). ARQ-003: interface only.
 */
public interface ListMySubscriptionsQuery {

  /**
   * List all active subscriptions for the given student.
   *
   * @param userId the authenticated student's user ID
   * @return list of active subscription views (0–2 items per contract)
   */
  List<SubscriptionView> listMySubscriptions(UUID userId);
}
