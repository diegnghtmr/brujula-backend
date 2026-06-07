package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.progress.domain.SubscriptionStatus;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.dto.UpdateSubscriptionCommand;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.service.UpdateSubscriptionService;
import co.edu.uniquindio.brujula.progress.domain.HistoryImpactNotAcknowledgedException;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU2-T12 RED+GREEN: update subscription use case tests.
 *
 * <p>Covers: SUB-3-JOURNEY, SUB-3-PLAN-WITH-ACK, SUB-3-PLAN-NO-ACK, BOLA.
 */
class UpdateSubscriptionServiceTest {

  private LoadSubscriptionPort loadSubscriptionPort;
  private SaveSubscriptionPort saveSubscriptionPort;
  private AuditRecorder auditRecorder;
  private UpdateSubscriptionService sut;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    saveSubscriptionPort = mock(SaveSubscriptionPort.class);
    auditRecorder = mock(AuditRecorder.class);
    sut = new UpdateSubscriptionService(loadSubscriptionPort, saveSubscriptionPort, auditRecorder);
  }

  @Test
  void journey_only_update_succeeds() {
    UUID subId = UUID.randomUUID();
    Subscription sub = Subscription.create(SubscriptionId.of(subId), userId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));
    // Stub save to return the same subscription (unit test; version increment tested in persistence adapter test)
    when(saveSubscriptionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UpdateSubscriptionCommand command =
        new UpdateSubscriptionCommand(subId, userId, null, "NOCTURNA", false, 0L);

    SubscriptionView result = sut.updateSubscription(command);

    assertThat(result.referenceJourneyCode()).isEqualTo("NOCTURNA");
    verify(saveSubscriptionPort).save(any());
    verify(auditRecorder).record(any());
  }

  @Test
  void plan_change_with_ack_and_attempts_succeeds() {
    UUID subId = UUID.randomUUID();
    UUID newPlanId = UUID.randomUUID();
    Subscription sub = Subscription.create(SubscriptionId.of(subId), userId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));
    when(saveSubscriptionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UpdateSubscriptionCommand command =
        new UpdateSubscriptionCommand(subId, userId, newPlanId, "DIURNA", true, 0L);

    // Simulate hasAttempts=true by providing acknowledgeHistoryImpact=true
    SubscriptionView result = sut.updateSubscription(command);

    assertThat(result.planId()).isEqualTo(newPlanId);
  }

  // -------------------------------------------------------------------------
  // FIX-W4 RED: returned view must carry the DB-incremented version (SUB-3-JOURNEY)
  // -------------------------------------------------------------------------

  @Test
  void update_returns_incremented_version_from_save() {
    UUID subId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    Subscription sub = Subscription.create(SubscriptionId.of(subId), userId, programId, planId, "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));

    // Simulate the persistence adapter returning a reconstituted subscription with version=1
    Subscription savedWithIncrementedVersion =
        Subscription.reconstitute(
            SubscriptionId.of(subId), userId, programId, planId, "NOCTURNA",
            SubscriptionStatus.ACTIVE, sub.getCreatedAt(), 1L);
    when(saveSubscriptionPort.save(any())).thenReturn(savedWithIncrementedVersion);

    UpdateSubscriptionCommand command =
        new UpdateSubscriptionCommand(subId, userId, null, "NOCTURNA", false, 0L);

    SubscriptionView result = sut.updateSubscription(command);

    // FIX-W4: the response version MUST be the incremented value (1), not the pre-save value (0)
    assertThat(result.version()).isEqualTo(1L);
    assertThat(result.referenceJourneyCode()).isEqualTo("NOCTURNA");
  }

  @Test
  void plan_change_without_ack_rejected_when_has_attempts() {
    UUID subId = UUID.randomUUID();
    UUID newPlanId = UUID.randomUUID();
    Subscription sub = Subscription.create(SubscriptionId.of(subId), userId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));
    when(saveSubscriptionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    // Simulate that there are attempts for this subscription
    when(loadSubscriptionPort.hasAttempts(subId)).thenReturn(true);

    UpdateSubscriptionCommand command =
        new UpdateSubscriptionCommand(subId, userId, newPlanId, "DIURNA", false, 0L);

    assertThatThrownBy(() -> sut.updateSubscription(command))
        .isInstanceOf(HistoryImpactNotAcknowledgedException.class);
  }

  @Test
  void update_throws_not_found_for_bola() {
    UUID subId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    Subscription sub = Subscription.create(SubscriptionId.of(subId), otherUserId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));

    UpdateSubscriptionCommand command =
        new UpdateSubscriptionCommand(subId, userId, null, "NOCTURNA", false, 0L);

    assertThatThrownBy(() -> sut.updateSubscription(command))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }
}
