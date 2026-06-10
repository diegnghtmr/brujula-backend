package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.curriculum.application.dto.PlanMallaView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetProgramQuery;
import co.edu.uniquindio.brujula.progress.application.dto.PeriodAverageView;
import co.edu.uniquindio.brujula.progress.application.dto.ProgressSummaryView;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionProgressQuery;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.AttemptSummary;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.CourseProgressStatus;
import co.edu.uniquindio.brujula.progress.domain.EffectiveAttemptResolver;
import co.edu.uniquindio.brujula.progress.domain.PlanCourse;
import co.edu.uniquindio.brujula.progress.domain.ProgressSummaryCalculator;
import co.edu.uniquindio.brujula.progress.domain.ProgressSummaryData;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service: computes the progress summary for a subscription.
 *
 * <p>Implements AVL-2. Bulk-loads the plan malla via the curriculum published port (ONE call),
 * bulk-loads attempts (ONE call), resolves effective statuses, delegates to {@link
 * ProgressSummaryCalculator}. Zero N+1 queries (ADR-F3-005). BOLA-guarded (CROSS-2). Includes
 * CROSS-6 disclaimer.
 *
 * <p>Maps domain {@link ProgressSummaryData} to application {@link ProgressSummaryView} before
 * returning, so REST adapters do not reference domain types (ARQ-001).
 *
 * <p>FIX-W2: resolves the human-readable program name via {@link GetProgramQuery} so that {@link
 * ProgressSummaryView#programName()} contains the real program name instead of a UUID.
 *
 * <p>No Spring/framework imports — wired via ProgressConfiguration.
 */
public class GetSubscriptionProgressService implements GetSubscriptionProgressQuery {

  private final LoadSubscriptionPort loadSubscriptionPort;
  private final LoadCourseAttemptPort loadCourseAttemptPort;
  private final AssemblePlanMallaQuery assemblePlanMallaQuery;
  private final GetProgramQuery getProgramQuery;
  private final EffectiveAttemptResolver effectiveAttemptResolver;
  private final ProgressSummaryCalculator progressSummaryCalculator;

  public GetSubscriptionProgressService(
      LoadSubscriptionPort loadSubscriptionPort,
      LoadCourseAttemptPort loadCourseAttemptPort,
      AssemblePlanMallaQuery assemblePlanMallaQuery,
      GetProgramQuery getProgramQuery) {
    this.loadSubscriptionPort = loadSubscriptionPort;
    this.loadCourseAttemptPort = loadCourseAttemptPort;
    this.assemblePlanMallaQuery = assemblePlanMallaQuery;
    this.getProgramQuery = getProgramQuery;
    this.effectiveAttemptResolver = new EffectiveAttemptResolver();
    this.progressSummaryCalculator = new ProgressSummaryCalculator();
  }

  @Override
  public ProgressSummaryView getSubscriptionProgress(UUID subscriptionId, UUID userId) {
    // Ownership guard (BOLA)
    Subscription subscription =
        loadSubscriptionPort
            .loadById(subscriptionId)
            .filter(s -> s.getUserId().equals(userId))
            .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

    // FIX-W2: resolve human-readable program name via programcatalog published port
    String programName = resolveProgramName(subscription.getProgramId());

    // --- Bulk-load 1: Plan malla (ONE call, no N+1) ---
    PlanMallaView malla =
        assemblePlanMallaQuery.assemblePlanMalla(
            subscription.getPlanId(), subscription.getReferenceJourneyCode());

    // --- Bulk-load 2: All attempts (ONE call, no N+1) ---
    List<CourseAttempt> attempts = loadCourseAttemptPort.loadBySubscriptionId(subscriptionId);

    // Build plan courses list
    List<PlanCourse> planCourses = new ArrayList<>();
    for (PlanMallaView.SemesterBucket bucket : malla.semesterBuckets()) {
      for (PlanMallaView.CourseSummary cs : bucket.courses()) {
        planCourses.add(
            new PlanCourse(
                cs.id(), cs.name(), cs.credits(), cs.type(), bucket.recommendedSemester()));
      }
    }

    // Group attempts by courseId for effective-status resolution
    Map<UUID, List<CourseAttempt>> attemptsByCourse = new HashMap<>();
    for (CourseAttempt attempt : attempts) {
      attemptsByCourse.computeIfAbsent(attempt.getCourseId(), k -> new ArrayList<>()).add(attempt);
    }

    // Resolve effective status per course
    Map<UUID, CourseProgressStatus> effectiveByCourse = new HashMap<>();
    for (Map.Entry<UUID, List<CourseAttempt>> entry : attemptsByCourse.entrySet()) {
      Optional<AttemptStatus> effective = effectiveAttemptResolver.resolve(entry.getValue());
      effective.ifPresent(
          status -> {
            CourseProgressStatus progressStatus =
                switch (status) {
                  case PASSED -> CourseProgressStatus.PASSED;
                  case FAILED -> CourseProgressStatus.FAILED;
                  case CANCELLED -> CourseProgressStatus.CANCELLED;
                };
            effectiveByCourse.put(entry.getKey(), progressStatus);
          });
    }

    // Build attempt summaries for the calculator
    List<AttemptSummary> attemptSummaries =
        attempts.stream()
            .map(
                a ->
                    new AttemptSummary(
                        a.getCourseId(),
                        a.getStatus(),
                        a.getGrade() != null ? a.getGrade().value() : null,
                        a.getPeriodId()))
            .toList();

    ProgressSummaryData data =
        progressSummaryCalculator.compute(planCourses, effectiveByCourse, attemptSummaries);

    return toView(subscriptionId, programName, data);
  }

  // -------------------------------------------------------------------------
  // Mapping: domain ProgressSummaryData → application ProgressSummaryView (ARQ-001)
  // -------------------------------------------------------------------------

  private ProgressSummaryView toView(
      UUID subscriptionId, String programName, ProgressSummaryData data) {
    List<PeriodAverageView> periodAverages =
        data.averagesByPeriod() != null
            ? data.averagesByPeriod().stream()
                .map(p -> new PeriodAverageView(p.periodId(), p.estimatedAverage()))
                .toList()
            : List.of();

    return new ProgressSummaryView(
        subscriptionId,
        programName,
        data.totalPlanCredits(),
        data.approvedCredits(),
        data.pendingCredits(),
        data.progressPercentage(),
        data.passedCourses(),
        data.pendingFailedCourses(),
        data.estimatedHistoricalAverage(),
        periodAverages,
        data.disclaimer());
  }

  // -------------------------------------------------------------------------
  // Helper: resolve program name from programcatalog
  // -------------------------------------------------------------------------

  /**
   * FIX-W2: resolves the human-readable program name from the programcatalog published port. Falls
   * back to the programId string representation if the program cannot be found (defensive: avoids a
   * hard 500 for a non-critical display field).
   */
  private String resolveProgramName(UUID programId) {
    try {
      return getProgramQuery.getProgram(programId).name();
    } catch (Exception e) {
      // Defensive fallback: programId as string if catalog lookup fails
      return programId.toString();
    }
  }
}
