package co.edu.uniquindio.brujula.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EffectiveAttemptResolver} domain service.
 *
 * <p>Scenarios: PRG-5-PASSED-WINS, PRG-5-NO-PASS, empty → Optional.empty() (FIX-W2). ADR-F3-006.
 */
class EffectiveAttemptResolverTest {

  private final EffectiveAttemptResolver resolver = new EffectiveAttemptResolver();
  private final SubscriptionId subscriptionId = SubscriptionId.generate();
  private final UUID courseId = UUID.randomUUID();

  // FIX-W2 RED: empty attempts must return Optional.empty(), not PENDING sentinel
  @Test
  void empty_attempts_yields_optional_empty() {
    Optional<AttemptStatus> result = resolver.resolve(List.of());
    assertThat(result).isEmpty();
  }

  @Test
  void single_cancelled_yields_optional_of_cancelled() {
    CourseAttempt cancelled = cancelledAttempt();
    Optional<AttemptStatus> result = resolver.resolve(List.of(cancelled));
    assertThat(result).contains(AttemptStatus.CANCELLED);
  }

  @Test
  void single_failed_yields_optional_of_failed() {
    CourseAttempt failed = failedAttempt(new BigDecimal("2.0"));
    Optional<AttemptStatus> result = resolver.resolve(List.of(failed));
    assertThat(result).contains(AttemptStatus.FAILED);
  }

  @Test
  void single_passed_yields_optional_of_passed() {
    CourseAttempt passed = passedAttempt(new BigDecimal("4.0"));
    Optional<AttemptStatus> result = resolver.resolve(List.of(passed));
    assertThat(result).contains(AttemptStatus.PASSED);
  }

  @Test
  void passed_then_failed_yields_optional_of_passed() {
    // PRG-5-PASSED-WINS: any PASSED wins regardless of subsequent status
    CourseAttempt passed = passedAttempt(new BigDecimal("4.0"));
    CourseAttempt failed = failedAttempt(new BigDecimal("2.0"));
    Optional<AttemptStatus> result = resolver.resolve(List.of(passed, failed));
    assertThat(result).contains(AttemptStatus.PASSED);
  }

  @Test
  void failed_then_passed_yields_optional_of_passed() {
    CourseAttempt failed = failedAttempt(new BigDecimal("2.0"));
    CourseAttempt passed = passedAttempt(new BigDecimal("3.5"));
    Optional<AttemptStatus> result = resolver.resolve(List.of(failed, passed));
    assertThat(result).contains(AttemptStatus.PASSED);
  }

  @Test
  void passed_then_failed_then_cancelled_yields_optional_of_passed() {
    // PRG-5-PASSED-WINS: PASSED dominates over all
    CourseAttempt passed = passedAttempt(new BigDecimal("4.0"));
    CourseAttempt failed = failedAttempt(new BigDecimal("2.0"));
    CourseAttempt cancelled = cancelledAttempt();
    Optional<AttemptStatus> result = resolver.resolve(List.of(passed, failed, cancelled));
    assertThat(result).contains(AttemptStatus.PASSED);
  }

  @Test
  void failed_then_cancelled_yields_optional_of_failed() {
    // PRG-5-NO-PASS: no PASSED attempt — most favorable non-PASSED is FAILED
    CourseAttempt failed = failedAttempt(new BigDecimal("1.5"));
    CourseAttempt cancelled = cancelledAttempt();
    Optional<AttemptStatus> result = resolver.resolve(List.of(failed, cancelled));
    assertThat(result).contains(AttemptStatus.FAILED);
  }

  @Test
  void two_failed_attempts_yields_optional_of_failed() {
    CourseAttempt failed1 = failedAttempt(new BigDecimal("2.0"));
    CourseAttempt failed2 = failedAttempt(new BigDecimal("1.5"));
    Optional<AttemptStatus> result = resolver.resolve(List.of(failed1, failed2));
    assertThat(result).contains(AttemptStatus.FAILED);
  }

  @Test
  void two_cancelled_attempts_yields_optional_of_cancelled() {
    CourseAttempt c1 = cancelledAttempt();
    CourseAttempt c2 = cancelledAttempt();
    Optional<AttemptStatus> result = resolver.resolve(List.of(c1, c2));
    assertThat(result).contains(AttemptStatus.CANCELLED);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CourseAttempt passedAttempt(BigDecimal gradeValue) {
    return CourseAttempt.reconstitute(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.PASSED,
        new Grade(gradeValue),
        null,
        "DIURNA",
        Instant.now(),
        0L);
  }

  private CourseAttempt failedAttempt(BigDecimal gradeValue) {
    return CourseAttempt.reconstitute(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.FAILED,
        new Grade(gradeValue),
        null,
        "DIURNA",
        Instant.now(),
        0L);
  }

  private CourseAttempt cancelledAttempt() {
    return CourseAttempt.reconstitute(
        AttemptId.generate(),
        subscriptionId,
        courseId,
        AttemptStatus.CANCELLED,
        null,
        null,
        "DIURNA",
        Instant.now(),
        0L);
  }
}
