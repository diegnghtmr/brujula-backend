package co.edu.uniquindio.brujula.progress.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Application-layer DTO for the progress summary returned by {@link
 * co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionProgressQuery}.
 *
 * <p>This DTO is in the application package so REST adapters can use it without importing domain
 * types. The application service maps from {@link
 * co.edu.uniquindio.brujula.progress.domain.ProgressSummaryData} to this view.
 *
 * <p>{@code programName} is resolved from the programcatalog published port in the application
 * service rather than constructed as a UUID placeholder in the REST adapter.
 *
 * @param subscriptionId owning subscription UUID
 * @param programName human-readable program name (resolved via programcatalog port)
 * @param totalPlanCredits total credits in the study plan
 * @param approvedCredits credits for PASSED courses
 * @param pendingCredits credits remaining (total - approved)
 * @param progressPercentage approved / total * 100, scale 2
 * @param passedCourses number of courses with effective PASSED status
 * @param pendingFailedCourses number of courses with effective FAILED status
 * @param estimatedHistoricalAverage mean of graded (PASSED+FAILED) attempts; null if no data
 * @param averagesByPeriod per-period estimated averages; empty if no graded attempts
 * @param disclaimer mandatory autodeclared-data disclaimer
 */
public record ProgressSummaryView(
    UUID subscriptionId,
    String programName,
    double totalPlanCredits,
    double approvedCredits,
    double pendingCredits,
    BigDecimal progressPercentage,
    int passedCourses,
    int pendingFailedCourses,
    BigDecimal estimatedHistoricalAverage,
    List<PeriodAverageView> averagesByPeriod,
    String disclaimer) {}
