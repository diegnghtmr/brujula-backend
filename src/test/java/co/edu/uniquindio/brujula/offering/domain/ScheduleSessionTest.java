package co.edu.uniquindio.brujula.offering.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * ScheduleSession compact constructor invariant (endsAt &gt; startsAt) and overlaps() boundary
 * tests.
 */
class ScheduleSessionTest {

  // -------------------------------------------------------------------------
  // Compact constructor invariant
  // -------------------------------------------------------------------------

  @Test
  void constructorThrowsWhenEndsAtEqualToStartsAt() {
    assertThatThrownBy(
            () ->
                new ScheduleSession(
                    UUID.randomUUID(), Weekday.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(18, 0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("endsAt");
  }

  @Test
  void constructorThrowsWhenEndsAtBeforeStartsAt() {
    assertThatThrownBy(
            () ->
                new ScheduleSession(
                    UUID.randomUUID(), Weekday.WEDNESDAY, LocalTime.of(20, 0), LocalTime.of(18, 0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("endsAt");
  }

  @Test
  void constructorSucceedsWhenEndsAtAfterStartsAt() {
    var session =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0));
    assertThat(session.startsAt()).isEqualTo(LocalTime.of(8, 0));
    assertThat(session.endsAt()).isEqualTo(LocalTime.of(10, 0));
  }

  // -------------------------------------------------------------------------
  // overlaps() boundary tests
  // -------------------------------------------------------------------------

  @Test
  void touchingEdgesDoNotClash() {
    // Session A ends at 20:00, Session B starts at 20:00 — adjacent, no overlap
    var sessionA =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(20, 0));
    var sessionB =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.WEDNESDAY, LocalTime.of(20, 0), LocalTime.of(22, 0));

    assertThat(sessionA.overlaps(sessionB)).isFalse();
    assertThat(sessionB.overlaps(sessionA)).isFalse();
  }

  @Test
  void partialOverlapClashes() {
    // A: 18:00-20:00, B: 19:00-21:00 — overlap 19:00-20:00
    var sessionA =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(20, 0));
    var sessionB =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(21, 0));

    assertThat(sessionA.overlaps(sessionB)).isTrue();
    assertThat(sessionB.overlaps(sessionA)).isTrue();
  }

  @Test
  void exactSameSlotClashes() {
    var sessionA =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(20, 0));
    var sessionB =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(20, 0));

    assertThat(sessionA.overlaps(sessionB)).isTrue();
  }

  @Test
  void differentDayDoesNotClash() {
    var sessionA =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(20, 0));
    var sessionB =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.THURSDAY, LocalTime.of(18, 0), LocalTime.of(20, 0));

    assertThat(sessionA.overlaps(sessionB)).isFalse();
  }

  @Test
  void durationMinutesIsCorrect() {
    var session =
        new ScheduleSession(
            UUID.randomUUID(), Weekday.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 30));
    assertThat(session.durationMinutes()).isEqualTo(150);
  }
}
