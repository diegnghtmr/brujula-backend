package co.edu.uniquindio.brujula.progress.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain service: computes the {@link ProgressSummaryData} from plan courses, effective statuses,
 * and raw attempt summaries.
 *
 * <p>Pure function — no Spring beans, no ports, no I/O. Enforced by {@code
 * ProgressDomainPurityTest} (ARQ-002, ADR-F3-004).
 *
 * <p>Credit math:
 *
 * <ul>
 *   <li>totalPlanCredits = sum of credits for all plan courses.
 *   <li>approvedCredits = sum of credits for courses whose effective status = PASSED.
 *   <li>pendingCredits = totalPlanCredits - approvedCredits.
 *   <li>progressPercentage = (approvedCredits / totalPlanCredits) * 100, 2 decimal places; 0.0 if
 *       totalPlanCredits = 0.
 * </ul>
 *
 * <p>Average calculation:
 *
 * <ul>
 *   <li>estimatedHistoricalAverage = arithmetic mean of all PASSED and FAILED attempt grades; null
 *       when no graded attempts exist.
 *   <li>averagesByPeriod = per-period mean of graded attempts (only PASSED/FAILED with non-null
 *       grade); CANCELLED attempts are excluded.
 * </ul>
 *
 * <p>Satisfies: AVL-2-HP, AVL-2-NO-ATTEMPTS, CROSS-6-DISCLAIMER.
 */
public class ProgressSummaryCalculator {

  private static final String DISCLAIMER =
      "Los datos de progreso son autodeclarados y no constituyen información oficial de la universidad.";

  /**
   * Compute the progress summary.
   *
   * @param planCourses the full set of courses in the subscription's plan
   * @param effectiveByCourse computed effective status per courseId (from {@link
   *     CourseAvailabilityService} or {@link EffectiveAttemptResolver})
   * @param attempts all declared attempts for the subscription
   * @return the computed {@link ProgressSummaryData}
   */
  public ProgressSummaryData compute(
      List<PlanCourse> planCourses,
      Map<UUID, CourseProgressStatus> effectiveByCourse,
      List<AttemptSummary> attempts) {

    // --- Credit totals ---
    double totalPlanCredits = 0.0;
    double approvedCredits = 0.0;
    int passedCourses = 0;

    // Count courses that have at least one FAILED attempt and no PASSED
    int pendingFailedCourses = 0;

    if (planCourses != null) {
      for (PlanCourse course : planCourses) {
        totalPlanCredits += course.credits();
        CourseProgressStatus status =
            effectiveByCourse != null ? effectiveByCourse.get(course.courseId()) : null;
        if (CourseProgressStatus.PASSED.equals(status)) {
          approvedCredits += course.credits();
          passedCourses++;
        } else if (CourseProgressStatus.FAILED.equals(status)) {
          pendingFailedCourses++;
        }
      }
    }

    double pendingCredits = totalPlanCredits - approvedCredits;

    BigDecimal progressPercentage;
    if (totalPlanCredits == 0.0) {
      progressPercentage = BigDecimal.ZERO;
    } else {
      progressPercentage =
          BigDecimal.valueOf(approvedCredits)
              .divide(BigDecimal.valueOf(totalPlanCredits), 10, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .setScale(2, RoundingMode.HALF_UP);
    }

    // --- Grade averages ---
    BigDecimal estimatedHistoricalAverage = null;
    Map<UUID, List<BigDecimal>> gradesByPeriod = new HashMap<>();

    BigDecimal totalGrade = BigDecimal.ZERO;
    int gradedCount = 0;

    if (attempts != null) {
      for (AttemptSummary attempt : attempts) {
        if (attempt.grade() != null
            && (AttemptStatus.PASSED.equals(attempt.status())
                || AttemptStatus.FAILED.equals(attempt.status()))) {
          totalGrade = totalGrade.add(attempt.grade());
          gradedCount++;

          if (attempt.periodId() != null) {
            gradesByPeriod
                .computeIfAbsent(attempt.periodId(), k -> new ArrayList<>())
                .add(attempt.grade());
          }
        }
      }
    }

    if (gradedCount > 0) {
      estimatedHistoricalAverage =
          totalGrade.divide(BigDecimal.valueOf(gradedCount), 2, RoundingMode.HALF_UP);
    }

    List<PeriodAverageData> averagesByPeriod = new ArrayList<>();
    for (Map.Entry<UUID, List<BigDecimal>> entry : gradesByPeriod.entrySet()) {
      List<BigDecimal> periodGrades = entry.getValue();
      BigDecimal sum = periodGrades.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal avg = sum.divide(BigDecimal.valueOf(periodGrades.size()), 2, RoundingMode.HALF_UP);
      averagesByPeriod.add(new PeriodAverageData(entry.getKey(), avg));
    }

    return new ProgressSummaryData(
        totalPlanCredits,
        approvedCredits,
        pendingCredits,
        progressPercentage,
        passedCourses,
        pendingFailedCourses,
        estimatedHistoricalAverage,
        List.copyOf(averagesByPeriod),
        DISCLAIMER);
  }
}
