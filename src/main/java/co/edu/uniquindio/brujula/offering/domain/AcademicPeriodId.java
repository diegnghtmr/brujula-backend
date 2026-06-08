package co.edu.uniquindio.brujula.offering.domain;

import java.util.Objects;
import java.util.UUID;

/** Identity value object for an academic period. */
public record AcademicPeriodId(UUID value) {
  public AcademicPeriodId {
    Objects.requireNonNull(value, "AcademicPeriodId value must not be null");
  }

  public static AcademicPeriodId generate() {
    return new AcademicPeriodId(UUID.randomUUID());
  }

  public static AcademicPeriodId of(UUID value) {
    return new AcademicPeriodId(value);
  }
}
