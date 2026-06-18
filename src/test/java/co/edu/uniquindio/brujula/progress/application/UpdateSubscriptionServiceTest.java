package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import co.edu.uniquindio.brujula.progress.domain.SubscriptionStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Update subscription use case tests. */
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
    Subscription sub =
        Subscription.create(
            SubscriptionId.of(subId), userId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));
    // Stub save to return the same subscription (unit test; version increment tested in persistence
    // adapter test)
    when(saveSubscriptionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UpdateSubscriptionCommand command =
        new UpdateSubscriptionCommand(subId, userId, null, "NOCTURNA", false, 0L);

    SubscriptionView result = sut.updateSubscription(command);

    assertThat(result.referenceJourneyCode()).isEqualTo("NOCTURNA");
    verify(saveSubscriptionPort).save(any());
    verify(auditRecorder).record(any());
  }

  // Plan change with ack happy path — hasAttempts=true + ack=true → plan change succeeds,
  // version increments, no attempt rows deleted/mutated.
  @Test
  void plan_change_with_ack_and_attempts_succeeds_spec_sub3_plan_with_ack() {
    UUID subId = UUID.randomUUID();
    UUID oldPlanId = UUID.randomUUID();
    UUID newPlanId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    Subscription sub =
        Subscription.create(SubscriptionId.of(subId), userId, programId, oldPlanId, "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));
    // Mock hasAttempts=true — subscription with 3 existing attempts
    when(loadSubscriptionPort.hasAttempts(subId)).thenReturn(true);

    // Simulate persistence returning subscription with incremented version (DB @Version)
    Subscription savedWithNewPlan =
        Subscription.reconstitute(
            SubscriptionId.of(subId),
            userId,
            programId,
            newPlanId,
            "DIURNA",
            SubscriptionStatus.ACTIVE,
            sub.getCreatedAt(),
            1L);
    when(saveSubscriptionPort.save(any())).thenReturn(savedWithNewPlan);

    UpdateSubscriptionCommand command =
        new UpdateSubscriptionCommand(subId, userId, newPlanId, "DIURNA", true, 0L);

    SubscriptionView result = sut.updateSubscription(command);

    // Plan must have changed to newPlanId
    assertThat(result.planId()).isEqualTo(newPlanId);
    // Version must have incremented (DP-009: subscription saved, attempts preserved not deleted)
    assertThat(result.version()).isEqualTo(1L);
    // save was called exactly once — no attempt rows were deleted/mutated
    verify(saveSubscriptionPort).save(any());
    // audit must have been recorded
    verify(auditRecorder).record(any());
  }

  // -------------------------------------------------------------------------
  // Returned view must carry the DB-incremented version
  // -------------------------------------------------------------------------

  @Test
  void update_returns_incremented_version_from_save() {
    UUID subId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    Subscription sub =
        Subscription.create(SubscriptionId.of(subId), userId, programId, planId, "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));

    // Simulate the persistence adapter returning a reconstituted subscription with version=1
    Subscription savedWithIncrementedVersion =
        Subscription.reconstitute(
            SubscriptionId.of(subId),
            userId,
            programId,
            planId,
            "NOCTURNA",
            SubscriptionStatus.ACTIVE,
            sub.getCreatedAt(),
            1L);
    when(saveSubscriptionPort.save(any())).thenReturn(savedWithIncrementedVersion);

    UpdateSubscriptionCommand command =
        new UpdateSubscriptionCommand(subId, userId, null, "NOCTURNA", false, 0L);

    SubscriptionView result = sut.updateSubscription(command);

    // The response version MUST be the incremented value (1), not the pre-save value (0)
    assertThat(result.version()).isEqualTo(1L);
    assertThat(result.referenceJourneyCode()).isEqualTo("NOCTURNA");
  }

  @Test
  void plan_change_without_ack_rejected_when_has_attempts() {
    UUID subId = UUID.randomUUID();
    UUID newPlanId = UUID.randomUUID();
    Subscription sub =
        Subscription.create(
            SubscriptionId.of(subId), userId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
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
    Subscription sub =
        Subscription.create(
            SubscriptionId.of(subId), otherUserId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    when(loadSubscriptionPort.loadById(subId)).thenReturn(Optional.of(sub));

    UpdateSubscriptionCommand command =
        new UpdateSubscriptionCommand(subId, userId, null, "NOCTURNA", false, 0L);

    assertThatThrownBy(() -> sut.updateSubscription(command))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }
}
