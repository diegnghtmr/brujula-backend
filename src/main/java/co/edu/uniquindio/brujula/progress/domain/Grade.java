package co.edu.uniquindio.brujula.progress.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a course final grade in the range [0.0, 5.0].
 *
 * <p>A grade is considered passing ({@link #passing()}) when its value is ≥ 3.0.
 *
 * <p>No Spring/JPA imports.
 */
public record Grade(BigDecimal value) {

  public Grade {
    Objects.requireNonNull(value, "Grade value must not be null");
    if (value.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Grade must not be negative, got: " + value);
    }
    if (value.compareTo(new BigDecimal("5.0")) > 0) {
      throw new IllegalArgumentException("Grade must not exceed 5.0, got: " + value);
    }
  }

  /**
   * Returns {@code true} if this grade is a passing grade (≥ 3.0).
   *
   * @return true when value ≥ 3.0
   */
  public boolean passing() {
    return value.compareTo(new BigDecimal("3.0")) >= 0;
  }
}
