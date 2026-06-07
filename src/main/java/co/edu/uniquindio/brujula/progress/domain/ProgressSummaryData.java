package co.edu.uniquindio.brujula.progress.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Domain result of the progress summary computation by {@link ProgressSummaryCalculator}.
 *
 * <p>No Spring/JPA imports (ARQ-002). All numeric fields use {@link BigDecimal} for precision;
 * the application layer converts to {@code Double} for the generated contract response.
 *
 * @param totalPlanCredits sum of credits for all plan courses
 * @param approvedCredits sum of credits for PASSED courses
 * @param pendingCredits totalPlanCredits - approvedCredits
 * @param progressPercentage approvedCredits / totalPlanCredits * 100 (2 decimal places; 0.0 if no
 *     credits)
 * @param passedCourses count of courses with effective status = PASSED
 * @param pendingFailedCourses count of courses with at least one FAILED attempt and no PASSED
 * @param estimatedHistoricalAverage arithmetic mean of all graded (PASSED/FAILED) attempts;
 *     null when no graded attempts
 * @param averagesByPeriod list of per-period averages (only graded attempts counted)
 * @param disclaimer non-empty self-declared data disclaimer (CROSS-6)
 */
public record ProgressSummaryData(
    double totalPlanCredits,
    double approvedCredits,
    double pendingCredits,
    BigDecimal progressPercentage,
    int passedCourses,
    int pendingFailedCourses,
    BigDecimal estimatedHistoricalAverage,
    List<PeriodAverageData> averagesByPeriod,
    String disclaimer) {

  public ProgressSummaryData {
    Objects.requireNonNull(progressPercentage, "progressPercentage must not be null");
    Objects.requireNonNull(averagesByPeriod, "averagesByPeriod must not be null");
    Objects.requireNonNull(disclaimer, "disclaimer must not be null");
  }
}
