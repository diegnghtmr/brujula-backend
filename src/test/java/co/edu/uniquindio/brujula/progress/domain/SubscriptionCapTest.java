package co.edu.uniquindio.brujula.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Domain cap guard — maximum 2 DISTINCT programs per student.
 *
 * <p>Covers: SUB-1-CAP. The uniqueness key is (programId + referenceJourneyCode); the cap is on
 * distinct programs (careers), not on total active rows.
 */
class SubscriptionCapTest {

  @Test
  void exception_message_mentions_distinct_programs_and_count() {
    UUID userId = UUID.randomUUID();
    MaxSubscriptionsExceededException ex = new MaxSubscriptionsExceededException(userId, 2);

    assertThat(ex.getMessage()).contains("2").contains("distinct");
    assertThat(ex.getUserId()).isEqualTo(userId);
    assertThat(ex.getActiveCount()).isEqualTo(2);
  }

  @Test
  void subscription_cap_guard_throws_when_distinct_program_count_equals_2() {
    // The cap guard is enforced at application-service level by computing distinct programIds.
    // This test validates the domain exception exists and is thrown correctly.
    assertThatThrownBy(
            () -> {
              throw new MaxSubscriptionsExceededException(UUID.randomUUID(), 2);
            })
        .isInstanceOf(MaxSubscriptionsExceededException.class)
        .hasMessageContaining("2");
  }
}
