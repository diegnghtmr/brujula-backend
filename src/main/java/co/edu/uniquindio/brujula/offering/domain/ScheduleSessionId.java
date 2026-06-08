package co.edu.uniquindio.brujula.offering.domain;

import java.util.Objects;
import java.util.UUID;

/** Identity value object for a schedule session. */
public record ScheduleSessionId(UUID value) {
  public ScheduleSessionId {
    Objects.requireNonNull(value, "ScheduleSessionId value must not be null");
  }

  public static ScheduleSessionId generate() {
    return new ScheduleSessionId(UUID.randomUUID());
  }

  public static ScheduleSessionId of(UUID value) {
    return new ScheduleSessionId(value);
  }
}
