package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.service.DeleteSubscriptionService;
import co.edu.uniquindio.brujula.progress.domain.LastActiveSubscriptionException;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU2-T13 RED+GREEN: delete subscription use case tests.
 *
 * <p>Covers: SUB-4-HAPPY, SUB-4-LAST, BOLA.
 */
class DeleteSubscriptionServiceTest {

  private LoadSubscriptionPort loadSubscriptionPort;
  private SaveSubscriptionPort saveSubscriptionPort;
  private AuditRecorder auditRecorder;
  private DeleteSubscriptionService sut;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    saveSubscriptionPort = mock(SaveSubscriptionPort.class);
    auditRecorder = mock(AuditRecorder.class);
    sut = new DeleteSubscriptionService(loadSubscriptionPort, saveSubscriptionPort, auditRecorder);
  }

  @Test
  void delete_one_of_two_subscriptions_succeeds() {
    UUID subId = UUID.randomUUID();
    Subscription sub = Subscription.create(SubscriptionId.of(subId), userId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));
    when(loadSubscriptionPort.countActiveByUserId(userId)).thenReturn(2);

    assertThatCode(() -> sut.deleteSubscription(subId, userId)).doesNotThrowAnyException();

    verify(saveSubscriptionPort).save(any());
    verify(auditRecorder).record(any());
  }

  @Test
  void delete_last_subscription_throws_last_active_exception() {
    UUID subId = UUID.randomUUID();
    Subscription sub = Subscription.create(SubscriptionId.of(subId), userId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));
    when(loadSubscriptionPort.countActiveByUserId(userId)).thenReturn(1);

    assertThatThrownBy(() -> sut.deleteSubscription(subId, userId))
        .isInstanceOf(LastActiveSubscriptionException.class);
  }

  @Test
  void delete_throws_not_found_for_bola() {
    UUID subId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    Subscription sub = Subscription.create(SubscriptionId.of(subId), otherUserId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));

    assertThatThrownBy(() -> sut.deleteSubscription(subId, userId))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }

  @Test
  void delete_throws_not_found_for_unknown_id() {
    UUID unknownId = UUID.randomUUID();
    when(loadSubscriptionPort.loadById(unknownId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.deleteSubscription(unknownId, userId))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }
}
