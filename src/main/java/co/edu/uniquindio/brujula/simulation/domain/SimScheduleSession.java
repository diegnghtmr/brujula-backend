package co.edu.uniquindio.brujula.simulation.domain;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Simulation-local value object for a single time slot.
 *
 * <p>Mirrors offering's ScheduleSession shape but kept in the simulation domain to preserve
 * boundary purity. Offering domain types are NOT imported here. Mapping from {@code
 * ScheduleSessionView} happens at the application/adapter edge.
 *
 * @param day weekday string (e.g. "MONDAY", "WEDNESDAY") — stored as String to avoid coupling to
 *     offering's Weekday enum
 * @param startsAt session start time (inclusive)
 * @param endsAt session end time (exclusive end; must be strictly after startsAt)
 */
public record SimScheduleSession(String day, LocalTime startsAt, LocalTime endsAt) {

  public SimScheduleSession {
    Objects.requireNonNull(day, "day required");
    Objects.requireNonNull(startsAt, "startsAt required");
    Objects.requireNonNull(endsAt, "endsAt required");
    if (!endsAt.isAfter(startsAt)) {
      throw new IllegalArgumentException(
          "endsAt must be strictly after startsAt: " + startsAt + " >= " + endsAt);
    }
  }

  /**
   * Returns true when this session temporally overlaps with {@code other} on the same day.
   *
   * <p>Overlap condition (strict inequality): {@code startsAt < other.endsAt && endsAt >
   * other.startsAt}. Touching intervals (e.g. one ends exactly when the other starts) are NOT
   * considered overlapping ("adjacent, non-overlapping" scenario is allowed).
   *
   * @param other the other session
   * @return true iff the sessions are on the same day AND their time ranges overlap
   */
  public boolean overlaps(SimScheduleSession other) {
    if (!this.day.equals(other.day)) {
      return false;
    }
    // Strict overlap: [startsAt, endsAt) vs [other.startsAt, other.endsAt)
    return this.startsAt.isBefore(other.endsAt) && this.endsAt.isAfter(other.startsAt);
  }
}
