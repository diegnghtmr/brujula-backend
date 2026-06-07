package co.edu.uniquindio.brujula.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * WU3-T02 RED: unit tests for {@link CourseAttempt} domain aggregate.
 *
 * <p>Scenarios: PRG-1-GRADE-MISMATCH, PRG-1-CANCELLED-WITH-GRADE, and grade-status coherence rules.
 */
class CourseAttemptTest {

  private final SubscriptionId subscriptionId = SubscriptionId.generate();
  private final UUID courseId = UUID.randomUUID();

  @Test
  void register_passed_attempt_with_passing_grade_succeeds() {
    Grade grade = new Grade(new BigDecimal("4.2"));
    CourseAttempt attempt = CourseAttempt.register(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.PASSED,
        grade,
        null,
        "DIURNA");

    assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.PASSED);
    assertThat(attempt.getGrade()).isNotNull();
    assertThat(attempt.getGrade().value()).isEqualByComparingTo(new BigDecimal("4.2"));
    assertThat(attempt.getVersion()).isEqualTo(0L);
  }

  @Test
  void register_failed_attempt_with_failing_grade_succeeds() {
    Grade grade = new Grade(new BigDecimal("2.0"));
    CourseAttempt attempt = CourseAttempt.register(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.FAILED,
        grade,
        null,
        "NOCTURNA");

    assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FAILED);
    assertThat(attempt.getGrade().value()).isEqualByComparingTo(new BigDecimal("2.0"));
  }

  @Test
  void register_cancelled_attempt_without_grade_succeeds() {
    CourseAttempt attempt = CourseAttempt.register(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.CANCELLED,
        null,
        null,
        "DIURNA");

    assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.CANCELLED);
    assertThat(attempt.getGrade()).isNull();
  }

  @Test
  void passed_with_failing_grade_throws_grade_status_mismatch() {
    Grade badGrade = new Grade(new BigDecimal("2.5"));
    assertThatThrownBy(() -> CourseAttempt.register(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.PASSED,
        badGrade,
        null,
        "DIURNA"))
        .isInstanceOf(GradeStatusMismatchException.class);
  }

  @Test
  void passed_with_null_grade_throws_grade_status_mismatch() {
    assertThatThrownBy(() -> CourseAttempt.register(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.PASSED,
        null,
        null,
        "DIURNA"))
        .isInstanceOf(GradeStatusMismatchException.class);
  }

  @Test
  void failed_with_passing_grade_throws_grade_status_mismatch() {
    Grade passingGrade = new Grade(new BigDecimal("3.5"));
    assertThatThrownBy(() -> CourseAttempt.register(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.FAILED,
        passingGrade,
        null,
        "DIURNA"))
        .isInstanceOf(GradeStatusMismatchException.class);
  }

  @Test
  void failed_with_null_grade_throws_grade_status_mismatch() {
    assertThatThrownBy(() -> CourseAttempt.register(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.FAILED,
        null,
        null,
        "DIURNA"))
        .isInstanceOf(GradeStatusMismatchException.class);
  }

  @Test
  void cancelled_with_grade_throws_grade_status_mismatch() {
    Grade grade = new Grade(new BigDecimal("3.0"));
    assertThatThrownBy(() -> CourseAttempt.register(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.CANCELLED,
        grade,
        null,
        "DIURNA"))
        .isInstanceOf(GradeStatusMismatchException.class);
  }

  @Test
  void opaque_period_id_is_stored_without_validation() {
    Grade grade = new Grade(new BigDecimal("4.0"));
    UUID opaquePeriodId = UUID.randomUUID();
    CourseAttempt attempt = CourseAttempt.register(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.PASSED,
        grade,
        opaquePeriodId,
        "DIURNA");

    assertThat(attempt.getPeriodId()).isEqualTo(opaquePeriodId);
  }

  @Test
  void period_id_can_be_null() {
    Grade grade = new Grade(new BigDecimal("4.0"));
    CourseAttempt attempt = CourseAttempt.register(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.PASSED,
        grade,
        null,
        "DIURNA");

    assertThat(attempt.getPeriodId()).isNull();
  }

  @Test
  void registered_at_is_set_on_create() {
    Grade grade = new Grade(new BigDecimal("3.5"));
    CourseAttempt attempt = CourseAttempt.register(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.PASSED,
        grade,
        null,
        "DIURNA");

    assertThat(attempt.getRegisteredAt()).isNotNull();
  }

  @Test
  void update_changes_status_and_grade() {
    Grade originalGrade = new Grade(new BigDecimal("2.0"));
    AttemptId id = AttemptId.generate();
    CourseAttempt attempt = CourseAttempt.reconstitute(
        id, subscriptionId, courseId, AttemptStatus.FAILED, originalGrade, null, "DIURNA",
        java.time.Instant.now(), 0L);

    Grade newGrade = new Grade(new BigDecimal("3.5"));
    attempt.update(AttemptStatus.PASSED, newGrade, null, "DIURNA");

    assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.PASSED);
    assertThat(attempt.getGrade().value()).isEqualByComparingTo(new BigDecimal("3.5"));
  }

  @Test
  void update_with_invalid_grade_throws_grade_status_mismatch() {
    Grade originalGrade = new Grade(new BigDecimal("2.0"));
    AttemptId id = AttemptId.generate();
    CourseAttempt attempt = CourseAttempt.reconstitute(
        id, subscriptionId, courseId, AttemptStatus.FAILED, originalGrade, null, "DIURNA",
        java.time.Instant.now(), 0L);

    Grade invalidGrade = new Grade(new BigDecimal("2.5")); // PASSED requires >= 3.0
    assertThatThrownBy(() -> attempt.update(AttemptStatus.PASSED, invalidGrade, null, "DIURNA"))
        .isInstanceOf(GradeStatusMismatchException.class);
  }
}
