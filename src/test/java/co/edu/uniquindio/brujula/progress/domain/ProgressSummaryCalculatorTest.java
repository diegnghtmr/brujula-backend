package co.edu.uniquindio.brujula.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * WU4-T02 RED: unit tests for {@link ProgressSummaryCalculator}.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>AVL-2-HP: credits math (approved, pending, percentage), passedCourses,
 *       pendingFailedCourses.
 *   <li>AVL-2-NO-ATTEMPTS: zero attempts → 0 approved, 0%, null average.
 *   <li>CROSS-6-DISCLAIMER: disclaimer is non-empty string.
 *   <li>averagesByPeriod grouping.
 *   <li>progressPercentage=0.0 when totalPlanCredits=0.
 * </ul>
 */
class ProgressSummaryCalculatorTest {

  private final ProgressSummaryCalculator sut = new ProgressSummaryCalculator();

  // -----------------------------------------------------------------------
  // AVL-2-HP: mixed attempts
  // -----------------------------------------------------------------------

  @Test
  void credits_math_with_passed_and_failed_attempts() {
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();
    UUID c3 = UUID.randomUUID();

    // Plan: 3 courses, 10+10+10 = 30 credits
    List<PlanCourse> planCourses =
        List.of(
            new PlanCourse(c1, "C1", 10, "REQUIRED", 1),
            new PlanCourse(c2, "C2", 10, "REQUIRED", 2),
            new PlanCourse(c3, "C3", 10, "REQUIRED", 3));

    // c1 PASSED (4.0), c2 FAILED (2.0), c3 no attempts
    Map<UUID, CourseProgressStatus> effectiveByCourse =
        Map.of(
            c1, CourseProgressStatus.PASSED,
            c2, CourseProgressStatus.FAILED);

    UUID periodA = UUID.randomUUID();
    List<AttemptSummary> attempts =
        List.of(
            new AttemptSummary(c1, AttemptStatus.PASSED, new BigDecimal("4.0"), periodA),
            new AttemptSummary(c2, AttemptStatus.FAILED, new BigDecimal("2.0"), periodA));

    ProgressSummaryData result = sut.compute(planCourses, effectiveByCourse, attempts);

    assertThat(result.totalPlanCredits()).isEqualTo(30.0);
    assertThat(result.approvedCredits()).isEqualTo(10.0);
    assertThat(result.pendingCredits()).isEqualTo(20.0);
    assertThat(result.progressPercentage()).isEqualByComparingTo(BigDecimal.valueOf(33.33));
    assertThat(result.passedCourses()).isEqualTo(1);
    assertThat(result.pendingFailedCourses()).isEqualTo(1);
    assertThat(result.disclaimer()).isNotBlank();
  }

  // -----------------------------------------------------------------------
  // AVL-2-NO-ATTEMPTS: zero attempts
  // -----------------------------------------------------------------------

  @Test
  void zero_attempts_returns_zero_credits_null_average() {
    UUID c1 = UUID.randomUUID();
    List<PlanCourse> planCourses = List.of(new PlanCourse(c1, "C1", 120, "REQUIRED", 1));
    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of();
    List<AttemptSummary> attempts = List.of();

    ProgressSummaryData result = sut.compute(planCourses, effectiveByCourse, attempts);

    assertThat(result.approvedCredits()).isEqualTo(0.0);
    assertThat(result.pendingCredits()).isEqualTo(120.0);
    assertThat(result.progressPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.passedCourses()).isZero();
    assertThat(result.pendingFailedCourses()).isZero();
    assertThat(result.estimatedHistoricalAverage()).isNull();
    assertThat(result.averagesByPeriod()).isEmpty();
  }

  // -----------------------------------------------------------------------
  // CROSS-6-DISCLAIMER: disclaimer non-empty
  // -----------------------------------------------------------------------

  @Test
  void disclaimer_is_always_non_empty() {
    ProgressSummaryData result = sut.compute(List.of(), Map.of(), List.of());
    assertThat(result.disclaimer()).isNotBlank();
  }

  // -----------------------------------------------------------------------
  // progressPercentage = 0.0 when totalPlanCredits = 0
  // -----------------------------------------------------------------------

  @Test
  void progress_percentage_is_zero_when_total_plan_credits_is_zero() {
    ProgressSummaryData result = sut.compute(List.of(), Map.of(), List.of());
    assertThat(result.progressPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  // -----------------------------------------------------------------------
  // estimatedHistoricalAverage: arithmetic mean of PASSED + FAILED grades
  // -----------------------------------------------------------------------

  @Test
  void estimated_historical_average_is_mean_of_passed_and_failed_grades() {
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();

    List<PlanCourse> planCourses =
        List.of(
            new PlanCourse(c1, "C1", 3, "REQUIRED", 1), new PlanCourse(c2, "C2", 3, "REQUIRED", 2));

    Map<UUID, CourseProgressStatus> effectiveByCourse =
        Map.of(
            c1, CourseProgressStatus.PASSED,
            c2, CourseProgressStatus.FAILED);

    UUID period = UUID.randomUUID();
    List<AttemptSummary> attempts =
        List.of(
            new AttemptSummary(c1, AttemptStatus.PASSED, new BigDecimal("4.0"), period),
            new AttemptSummary(c2, AttemptStatus.FAILED, new BigDecimal("2.0"), period));

    ProgressSummaryData result = sut.compute(planCourses, effectiveByCourse, attempts);

    // mean of 4.0 and 2.0 = 3.0
    assertThat(result.estimatedHistoricalAverage()).isNotNull();
    assertThat(result.estimatedHistoricalAverage()).isEqualByComparingTo(new BigDecimal("3.0"));
  }

  @Test
  void estimated_historical_average_is_null_when_all_attempts_cancelled() {
    UUID c1 = UUID.randomUUID();

    List<PlanCourse> planCourses = List.of(new PlanCourse(c1, "C1", 3, "REQUIRED", 1));
    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of(c1, CourseProgressStatus.CANCELLED);
    // CANCELLED attempts have no grade → should not count
    List<AttemptSummary> attempts =
        List.of(new AttemptSummary(c1, AttemptStatus.CANCELLED, null, UUID.randomUUID()));

    ProgressSummaryData result = sut.compute(planCourses, effectiveByCourse, attempts);

    assertThat(result.estimatedHistoricalAverage()).isNull();
  }

  // -----------------------------------------------------------------------
  // averagesByPeriod grouping
  // -----------------------------------------------------------------------

  @Test
  void averages_by_period_groups_correctly() {
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();
    UUID periodA = UUID.randomUUID();
    UUID periodB = UUID.randomUUID();

    List<PlanCourse> planCourses =
        List.of(
            new PlanCourse(c1, "C1", 3, "REQUIRED", 1), new PlanCourse(c2, "C2", 3, "REQUIRED", 2));

    Map<UUID, CourseProgressStatus> effectiveByCourse =
        Map.of(
            c1, CourseProgressStatus.PASSED,
            c2, CourseProgressStatus.FAILED);

    List<AttemptSummary> attempts =
        List.of(
            new AttemptSummary(c1, AttemptStatus.PASSED, new BigDecimal("4.0"), periodA),
            new AttemptSummary(c2, AttemptStatus.FAILED, new BigDecimal("2.0"), periodB));

    ProgressSummaryData result = sut.compute(planCourses, effectiveByCourse, attempts);

    assertThat(result.averagesByPeriod()).hasSize(2);
    assertThat(result.averagesByPeriod())
        .anyMatch(
            pa ->
                pa.periodId().equals(periodA)
                    && pa.estimatedAverage().compareTo(new BigDecimal("4.0")) == 0)
        .anyMatch(
            pa ->
                pa.periodId().equals(periodB)
                    && pa.estimatedAverage().compareTo(new BigDecimal("2.0")) == 0);
  }

  @Test
  void averages_by_period_excludes_cancelled_attempts() {
    UUID c1 = UUID.randomUUID();
    UUID period = UUID.randomUUID();

    List<PlanCourse> planCourses = List.of(new PlanCourse(c1, "C1", 3, "REQUIRED", 1));
    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of(c1, CourseProgressStatus.CANCELLED);

    // CANCELLED has no grade → no period average
    List<AttemptSummary> attempts =
        List.of(new AttemptSummary(c1, AttemptStatus.CANCELLED, null, period));

    ProgressSummaryData result = sut.compute(planCourses, effectiveByCourse, attempts);

    assertThat(result.averagesByPeriod()).isEmpty();
  }
}
