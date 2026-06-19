package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.curriculum.application.dto.PlanMallaView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery;
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptPageView;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import co.edu.uniquindio.brujula.progress.application.dto.ListCourseAttemptsQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.GetCourseAttemptQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.ListMyCourseAttemptsQuery;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.AttemptNotFoundException;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use case: read course attempts for the authenticated student.
 *
 * <p>Implements both {@link GetCourseAttemptQuery} and {@link ListMyCourseAttemptsQuery}. Enforces
 * BOLA ownership via subscription lookup.
 *
 * <p>Enriches each attempt with the human-readable course name (from the curriculum plan malla) and
 * period name (from the offering period catalog). Names are resolved in bulk (one call each) to
 * avoid N+1 queries.
 *
 * <p>Framework-free — wired by {@code ProgressConfiguration}.
 */
public class GetCourseAttemptService implements GetCourseAttemptQuery, ListMyCourseAttemptsQuery {

  private final LoadCourseAttemptPort loadCourseAttemptPort;
  private final LoadSubscriptionPort loadSubscriptionPort;
  private final AssemblePlanMallaQuery assemblePlanMallaQuery;
  private final ListPeriodsQuery listPeriodsQuery;

  public GetCourseAttemptService(
      LoadCourseAttemptPort loadCourseAttemptPort,
      LoadSubscriptionPort loadSubscriptionPort,
      AssemblePlanMallaQuery assemblePlanMallaQuery,
      ListPeriodsQuery listPeriodsQuery) {
    this.loadCourseAttemptPort = loadCourseAttemptPort;
    this.loadSubscriptionPort = loadSubscriptionPort;
    this.assemblePlanMallaQuery = assemblePlanMallaQuery;
    this.listPeriodsQuery = listPeriodsQuery;
  }

  @Override
  public CourseAttemptView getCourseAttempt(UUID attemptId, UUID actorId) {
    CourseAttempt attempt =
        loadCourseAttemptPort
            .loadById(attemptId)
            .orElseThrow(() -> new AttemptNotFoundException(attemptId));

    // BOLA: the subscription must belong to actorId. Reused for name enrichment.
    Subscription subscription =
        loadSubscriptionPort
            .loadById(attempt.getSubscriptionId().value())
            .filter(s -> s.getUserId().equals(actorId))
            .orElseThrow(() -> new AttemptNotFoundException(attemptId));

    return toView(attempt, courseNamesFor(subscription), periodNames());
  }

  @Override
  public CourseAttemptPageView listMyCourseAttempts(ListCourseAttemptsQuery query) {
    // When subscriptionId is provided, verify ownership first and reuse it for enrichment.
    Subscription subscription = null;
    if (query.subscriptionId() != null) {
      subscription =
          loadSubscriptionPort
              .loadById(query.subscriptionId())
              .orElseThrow(() -> new SubscriptionNotFoundException(query.subscriptionId()));
      if (!subscription.getUserId().equals(query.actorId())) {
        // BOLA: return 404 to not reveal existence.
        throw new SubscriptionNotFoundException(query.subscriptionId());
      }
    }

    List<CourseAttempt> attempts;
    long total;
    if (subscription != null) {
      // Shared history: aggregate across ALL the student's active subscriptions for this program,
      // so grades show under any journey (DIURNA/NOCTURNA) of the same career.
      attempts =
          loadCourseAttemptPort.loadByUserIdAndProgramIdAndCourseId(
              subscription.getUserId(),
              subscription.getProgramId(),
              query.courseId(),
              query.page(),
              query.size());
      total =
          loadCourseAttemptPort.countByUserIdAndProgramIdAndCourseId(
              subscription.getUserId(), subscription.getProgramId(), query.courseId());
    } else {
      // No subscription filter: fall back to the caller's raw attempt list.
      attempts =
          loadCourseAttemptPort.loadBySubscriptionIdAndCourseId(
              query.subscriptionId(), query.courseId(), query.page(), query.size());
      total =
          loadCourseAttemptPort.countBySubscriptionIdAndCourseId(
              query.subscriptionId(), query.courseId());
    }

    Map<UUID, String> courseNames = courseNamesFor(subscription);
    Map<UUID, String> periodNames = periodNames();

    List<CourseAttemptView> views =
        attempts.stream().map(attempt -> toView(attempt, courseNames, periodNames)).toList();
    int totalPages = query.size() > 0 ? (int) Math.ceil((double) total / query.size()) : 0;

    return new CourseAttemptPageView(views, query.page(), query.size(), total, totalPages);
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /**
   * Builds a {@code courseId -> name} map from the subscription's plan malla. Returns an empty map
   * when the subscription is unknown (cross-subscription listing) so course names fall back to
   * "N/A" at the REST layer.
   */
  private Map<UUID, String> courseNamesFor(Subscription subscription) {
    Map<UUID, String> names = new HashMap<>();
    if (subscription == null) {
      return names;
    }
    PlanMallaView malla =
        assemblePlanMallaQuery.assemblePlanMalla(
            subscription.getPlanId(), subscription.getReferenceJourneyCode());
    for (PlanMallaView.SemesterBucket bucket : malla.semesterBuckets()) {
      for (PlanMallaView.CourseSummary course : bucket.courses()) {
        names.put(course.id(), course.name());
      }
    }
    return names;
  }

  /** Builds a {@code periodId -> name} map from the offering period catalog (one call). */
  private Map<UUID, String> periodNames() {
    Map<UUID, String> names = new HashMap<>();
    for (AcademicPeriodView period : listPeriodsQuery.listPeriods(null)) {
      names.put(period.id(), period.name());
    }
    return names;
  }

  CourseAttemptView toView(
      CourseAttempt attempt, Map<UUID, String> courseNames, Map<UUID, String> periodNames) {
    BigDecimal gradeValue = attempt.getGrade() != null ? attempt.getGrade().value() : null;
    String courseName = courseNames.get(attempt.getCourseId());
    String periodName =
        attempt.getPeriodId() != null ? periodNames.get(attempt.getPeriodId()) : null;
    return new CourseAttemptView(
        attempt.getId().value(),
        attempt.getSubscriptionId().value(),
        attempt.getCourseId(),
        courseName, // nullable → REST layer maps to "N/A" for off-plan courses
        attempt.getPeriodId(),
        periodName, // nullable → frontend falls back to the raw period id
        attempt.getJourneyCode(),
        attempt.getStatus().name(), // plain String, no domain type in application DTO
        gradeValue,
        attempt.getRegisteredAt(),
        attempt.getVersion());
  }
}
