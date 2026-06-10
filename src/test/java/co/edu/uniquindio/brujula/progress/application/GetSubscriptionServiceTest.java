package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.service.GetSubscriptionService;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU2-T11 RED+GREEN: list and get subscription use case tests.
 *
 * <p>Covers: SUB-2-LIST, SUB-2-GET, SUB-2-BOLA.
 */
class GetSubscriptionServiceTest {

  private LoadSubscriptionPort loadSubscriptionPort;
  private GetSubscriptionService sut;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    sut = new GetSubscriptionService(loadSubscriptionPort);
  }

  @Test
  void list_my_subscriptions_returns_active_subscriptions_for_user() {
    UUID planId1 = UUID.randomUUID();
    UUID planId2 = UUID.randomUUID();
    Subscription s1 =
        Subscription.create(
            SubscriptionId.generate(), userId, UUID.randomUUID(), planId1, "DIURNA");
    Subscription s2 =
        Subscription.create(
            SubscriptionId.generate(), userId, UUID.randomUUID(), planId2, "NOCTURNA");

    when(loadSubscriptionPort.loadActiveByUserId(userId)).thenReturn(List.of(s1, s2));

    List<SubscriptionView> result = sut.listMySubscriptions(userId);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(v -> v != null);
  }

  @Test
  void get_subscription_returns_view_for_owner() {
    UUID subId = UUID.randomUUID();
    Subscription sub =
        Subscription.create(
            SubscriptionId.of(subId), userId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");

    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));

    SubscriptionView view = sut.getSubscription(subId, userId);

    assertThat(view.id()).isEqualTo(subId);
  }

  @Test
  void get_subscription_throws_not_found_for_unknown_id() {
    UUID unknownId = UUID.randomUUID();
    when(loadSubscriptionPort.loadById(unknownId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.getSubscription(unknownId, userId))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }

  @Test
  void get_subscription_throws_not_found_for_different_owner_bola() {
    UUID subId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    Subscription sub =
        Subscription.create(
            SubscriptionId.of(subId), otherUserId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");

    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));

    // BOLA: authenticated userId != subscription.userId → 404 (ownership not revealed)
    assertThatThrownBy(() -> sut.getSubscription(subId, userId))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }
}
