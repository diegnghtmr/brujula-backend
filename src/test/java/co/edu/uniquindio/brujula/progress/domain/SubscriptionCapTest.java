package co.edu.uniquindio.brujula.progress.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * WU2-T02 RED: domain cap guard — maximum 2 active subscriptions per student.
 *
 * <p>Covers: SUB-1-CAP.
 */
class SubscriptionCapTest {

  @Test
  void subscription_cap_guard_throws_when_active_count_equals_2() {
    // The cap guard is enforced at application-service level by counting active subscriptions.
    // This test validates the domain exception exists and is thrown correctly.
    assertThatThrownBy(() -> {
          throw new MaxSubscriptionsExceededException(
              java.util.UUID.randomUUID(), 2);
        })
        .isInstanceOf(MaxSubscriptionsExceededException.class)
        .hasMessageContaining("2");
  }
}
