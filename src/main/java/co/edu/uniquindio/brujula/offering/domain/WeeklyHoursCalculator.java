package co.edu.uniquindio.brujula.offering.domain;

import java.util.List;
import java.util.Objects;

/**
 * Pure domain service that computes the weekly hours for a schedule option.
 *
 * <p>Weekly hours = sum of all session durations (in minutes) / 60.0.
 * This is the "Suma semanal calculada" in the contract.
 */
public final class WeeklyHoursCalculator {

  private WeeklyHoursCalculator() {
    // utility class
  }

  /**
   * Calculates the total weekly hours by summing session durations.
   *
   * @param sessions the list of schedule sessions (may be empty)
   * @return total hours as a double (≥ 0)
   */
  public static double calculate(List<ScheduleSession> sessions) {
    Objects.requireNonNull(sessions, "sessions must not be null");
    long totalMinutes = sessions.stream()
        .mapToLong(ScheduleSession::durationMinutes)
        .sum();
    return totalMinutes / 60.0;
  }
}
