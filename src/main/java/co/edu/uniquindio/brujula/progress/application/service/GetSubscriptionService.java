package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.ListMySubscriptionsQuery;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import java.util.List;
import java.util.UUID;

/**
 * Use case: list and get subscriptions with BOLA ownership guard.
 *
 * <p>Framework-free. Satisfies: SUB-2, CROSS-2.
 */
public class GetSubscriptionService implements ListMySubscriptionsQuery, GetSubscriptionQuery {

  private final LoadSubscriptionPort loadSubscriptionPort;

  public GetSubscriptionService(LoadSubscriptionPort loadSubscriptionPort) {
    this.loadSubscriptionPort = loadSubscriptionPort;
  }

  @Override
  public List<SubscriptionView> listMySubscriptions(UUID userId) {
    return loadSubscriptionPort.loadActiveByUserId(userId).stream().map(this::toView).toList();
  }

  @Override
  public SubscriptionView getSubscription(UUID subscriptionId, UUID userId) {
    Subscription sub =
        loadSubscriptionPort
            .loadById(subscriptionId)
            .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

    // BOLA guard: 404 if owned by different user (ownership not revealed)
    if (!sub.getUserId().equals(userId)) {
      throw new SubscriptionNotFoundException(subscriptionId);
    }

    return toView(sub);
  }

  private SubscriptionView toView(Subscription sub) {
    return new SubscriptionView(
        sub.getId().value(),
        sub.getProgramId(),
        null, // programName: resolved by REST mapper or separate query
        sub.getPlanId(),
        null, // planName: resolved by REST mapper or separate query
        sub.getReferenceJourneyCode(),
        null, // referenceJourneyName: resolved by REST mapper or separate query
        sub.getVersion(),
        sub.getCreatedAt());
  }
}
