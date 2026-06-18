package co.edu.uniquindio.brujula.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Grade} value object.
 *
 * <p>Scenarios: valid bounds [0.0–5.0], {@code passing()} ≥ 3.0, rejects negatives and &gt; 5.0.
 */
class GradeTest {

  @Test
  void grade_at_minimum_zero_is_valid() {
    Grade grade = new Grade(BigDecimal.ZERO);
    assertThat(grade.value()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void grade_at_maximum_five_is_valid() {
    Grade grade = new Grade(new BigDecimal("5.0"));
    assertThat(grade.value()).isEqualByComparingTo(new BigDecimal("5.0"));
  }

  @Test
  void grade_3_0_is_passing() {
    Grade grade = new Grade(new BigDecimal("3.0"));
    assertThat(grade.passing()).isTrue();
  }

  @Test
  void grade_4_2_is_passing() {
    Grade grade = new Grade(new BigDecimal("4.2"));
    assertThat(grade.passing()).isTrue();
  }

  @Test
  void grade_2_9_is_not_passing() {
    Grade grade = new Grade(new BigDecimal("2.9"));
    assertThat(grade.passing()).isFalse();
  }

  @Test
  void grade_0_0_is_not_passing() {
    Grade grade = new Grade(BigDecimal.ZERO);
    assertThat(grade.passing()).isFalse();
  }

  @Test
  void negative_grade_is_rejected() {
    assertThatThrownBy(() -> new Grade(new BigDecimal("-0.1")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void grade_above_5_0_is_rejected() {
    assertThatThrownBy(() -> new Grade(new BigDecimal("5.1")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void null_grade_value_is_rejected() {
    assertThatThrownBy(() -> new Grade(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void grade_2_5_is_in_failed_range() {
    Grade grade = new Grade(new BigDecimal("2.5"));
    assertThat(grade.passing()).isFalse();
    assertThat(grade.value()).isEqualByComparingTo(new BigDecimal("2.5"));
  }
}
