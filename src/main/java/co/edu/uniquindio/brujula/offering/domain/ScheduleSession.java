package co.edu.uniquindio.brujula.offering.domain;

import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable value object representing a single time slot within a schedule option.
 *
 * <p>The compact constructor enforces the invariant that {@code endsAt > startsAt}. Two sessions
 * overlap when they are on the same day and their time intervals intersect (touching edges — one
 * ends exactly when the other starts — do NOT count as overlap).
 */
public record ScheduleSession(UUID id, Weekday day, LocalTime startsAt, LocalTime endsAt) {

  public ScheduleSession {
    Objects.requireNonNull(id, "ScheduleSession id must not be null");
    Objects.requireNonNull(day, "ScheduleSession day must not be null");
    Objects.requireNonNull(startsAt, "ScheduleSession startsAt must not be null");
    Objects.requireNonNull(endsAt, "ScheduleSession endsAt must not be null");
    if (!endsAt.isAfter(startsAt)) {
      throw new IllegalArgumentException(
          "endsAt must be strictly after startsAt: startsAt=%s, endsAt=%s"
              .formatted(startsAt, endsAt));
    }
  }

  /**
   * Returns the duration of this session in minutes.
   *
   * @return duration in minutes (always positive per constructor invariant)
   */
  public long durationMinutes() {
    return java.time.Duration.between(startsAt, endsAt).toMinutes();
  }

  /**
   * Tests whether this session overlaps in time with {@code other}.
   *
   * <p>Two sessions overlap if and only if they are on the same day AND their time intervals
   * strictly intersect (touching at boundary = no overlap): {@code startsAt < other.endsAt AND
   * endsAt > other.startsAt}.
   *
   * @param other the other session to compare with
   * @return {@code true} if and only if both sessions are on the same day and their time intervals
   *     strictly overlap
   */
  public boolean overlaps(ScheduleSession other) {
    Objects.requireNonNull(other, "other session must not be null");
    if (this.day != other.day) {
      return false;
    }
    // Interval overlap test (strict — touching edges return false)
    return this.startsAt.isBefore(other.endsAt) && this.endsAt.isAfter(other.startsAt);
  }
}
