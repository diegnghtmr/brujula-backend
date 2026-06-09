package co.edu.uniquindio.brujula.offering.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** WU1-TDD RED: WeeklyHoursCalculator.calculate() sums session durations correctly. */
class WeeklyHoursCalculatorTest {

  @Test
  void singleSessionDurationIsCorrect() {
    // 2-hour session → 2.0 weekly hours
    var sessions =
        List.of(
            new ScheduleSession(
                UUID.randomUUID(), Weekday.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)));

    double hours = WeeklyHoursCalculator.calculate(sessions);

    assertThat(hours).isCloseTo(2.0, within(0.001));
  }

  @Test
  void multipleSessionsAreSummed() {
    // 2h Monday + 1.5h Wednesday = 3.5h
    var sessions =
        List.of(
            new ScheduleSession(
                UUID.randomUUID(), Weekday.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
            new ScheduleSession(
                UUID.randomUUID(), Weekday.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 30)));

    double hours = WeeklyHoursCalculator.calculate(sessions);

    assertThat(hours).isCloseTo(3.5, within(0.001));
  }

  @Test
  void emptySessionListReturnsZero() {
    double hours = WeeklyHoursCalculator.calculate(List.of());
    assertThat(hours).isCloseTo(0.0, within(0.001));
  }
}
