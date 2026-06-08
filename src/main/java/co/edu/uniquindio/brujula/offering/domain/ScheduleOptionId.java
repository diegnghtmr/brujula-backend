package co.edu.uniquindio.brujula.offering.domain;

import java.util.Objects;
import java.util.UUID;

/** Identity value object for a schedule option. */
public record ScheduleOptionId(UUID value) {
  public ScheduleOptionId {
    Objects.requireNonNull(value, "ScheduleOptionId value must not be null");
  }

  public static ScheduleOptionId generate() {
    return new ScheduleOptionId(UUID.randomUUID());
  }

  public static ScheduleOptionId of(UUID value) {
    return new ScheduleOptionId(value);
  }
}
