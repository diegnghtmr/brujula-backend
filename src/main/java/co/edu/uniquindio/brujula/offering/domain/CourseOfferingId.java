package co.edu.uniquindio.brujula.offering.domain;

import java.util.Objects;
import java.util.UUID;

/** Identity value object for a course offering. */
public record CourseOfferingId(UUID value) {
  public CourseOfferingId {
    Objects.requireNonNull(value, "CourseOfferingId value must not be null");
  }

  public static CourseOfferingId generate() {
    return new CourseOfferingId(UUID.randomUUID());
  }

  public static CourseOfferingId of(UUID value) {
    return new CourseOfferingId(value);
  }
}
