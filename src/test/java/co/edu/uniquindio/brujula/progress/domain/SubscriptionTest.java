package co.edu.uniquindio.brujula.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * WU2-T01 RED: domain tests for {@link Subscription} aggregate.
 *
 * <p>Covers: SUB-1-HP, SUB-3-PLAN-NO-ACK.
 */
class SubscriptionTest {

  private final UUID userId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID planId = UUID.randomUUID();
  private final String journeyCode = "DIURNA";

  @Test
  void create_subscription_sets_active_status_and_version_zero() {
    SubscriptionId id = SubscriptionId.generate();

    Subscription sub = Subscription.create(id, userId, programId, planId, journeyCode);

    assertThat(sub.getId()).isEqualTo(id);
    assertThat(sub.getUserId()).isEqualTo(userId);
    assertThat(sub.getProgramId()).isEqualTo(programId);
    assertThat(sub.getPlanId()).isEqualTo(planId);
    assertThat(sub.getReferenceJourneyCode()).isEqualTo(journeyCode);
    assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(sub.getVersion()).isEqualTo(0L);
  }

  @Test
  void soft_delete_transitions_status_to_deleted() {
    Subscription sub =
        Subscription.create(SubscriptionId.generate(), userId, programId, planId, journeyCode);

    sub.softDelete();

    assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.DELETED);
  }

  @Test
  void soft_delete_is_idempotent() {
    Subscription sub =
        Subscription.create(SubscriptionId.generate(), userId, programId, planId, journeyCode);
    sub.softDelete();

    // Second call must not throw
    sub.softDelete();

    assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.DELETED);
  }

  @Test
  void update_reference_journey_changes_journey_code() {
    Subscription sub =
        Subscription.create(SubscriptionId.generate(), userId, programId, planId, journeyCode);

    sub.updateReferenceJourney("NOCTURNA");

    assertThat(sub.getReferenceJourneyCode()).isEqualTo("NOCTURNA");
  }

  @Test
  void update_reference_journey_rejects_blank_code() {
    Subscription sub =
        Subscription.create(SubscriptionId.generate(), userId, programId, planId, journeyCode);

    assertThatThrownBy(() -> sub.updateReferenceJourney(""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void change_plan_without_ack_throws_when_has_attempts() {
    Subscription sub =
        Subscription.create(SubscriptionId.generate(), userId, programId, planId, journeyCode);

    // hasAttempts=true, acknowledgeHistoryImpact=false → must throw
    assertThatThrownBy(() -> sub.changePlan(UUID.randomUUID(), "NOCTURNA", true, false))
        .isInstanceOf(HistoryImpactNotAcknowledgedException.class);
  }

  @Test
  void change_plan_with_ack_succeeds_when_has_attempts() {
    Subscription sub =
        Subscription.create(SubscriptionId.generate(), userId, programId, planId, journeyCode);
    UUID newPlanId = UUID.randomUUID();

    // acknowledgeHistoryImpact=true → allowed
    sub.changePlan(newPlanId, "NOCTURNA", true, true);

    assertThat(sub.getPlanId()).isEqualTo(newPlanId);
    assertThat(sub.getReferenceJourneyCode()).isEqualTo("NOCTURNA");
  }

  @Test
  void change_plan_without_ack_succeeds_when_no_attempts() {
    Subscription sub =
        Subscription.create(SubscriptionId.generate(), userId, programId, planId, journeyCode);
    UUID newPlanId = UUID.randomUUID();

    // hasAttempts=false → no ack needed
    sub.changePlan(newPlanId, "NOCTURNA", false, false);

    assertThat(sub.getPlanId()).isEqualTo(newPlanId);
  }

  @Test
  void create_rejects_null_userId() {
    assertThatThrownBy(
            () ->
                Subscription.create(
                    SubscriptionId.generate(), null, programId, planId, journeyCode))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void create_rejects_blank_journey_code() {
    assertThatThrownBy(
            () -> Subscription.create(SubscriptionId.generate(), userId, programId, planId, ""))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
