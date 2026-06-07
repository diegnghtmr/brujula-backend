package co.edu.uniquindio.brujula.progress.application.service;

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
import java.util.List;
import java.util.UUID;

/**
 * Use case: read course attempts for the authenticated student.
 *
 * <p>Implements both {@link GetCourseAttemptQuery} and {@link ListMyCourseAttemptsQuery}. Enforces
 * BOLA ownership via subscription lookup.
 *
 * <p>Framework-free — wired by {@code ProgressConfiguration}. Satisfies: PRG-2.
 */
public class GetCourseAttemptService implements GetCourseAttemptQuery, ListMyCourseAttemptsQuery {

  private final LoadCourseAttemptPort loadCourseAttemptPort;
  private final LoadSubscriptionPort loadSubscriptionPort;

  public GetCourseAttemptService(
      LoadCourseAttemptPort loadCourseAttemptPort,
      LoadSubscriptionPort loadSubscriptionPort) {
    this.loadCourseAttemptPort = loadCourseAttemptPort;
    this.loadSubscriptionPort = loadSubscriptionPort;
  }

  @Override
  public CourseAttemptView getCourseAttempt(UUID attemptId, UUID actorId) {
    CourseAttempt attempt =
        loadCourseAttemptPort
            .loadById(attemptId)
            .orElseThrow(() -> new AttemptNotFoundException(attemptId));

    // BOLA: verify the subscription belongs to actorId
    validateOwnership(attempt.getSubscriptionId().value(), actorId, attemptId);

    return toView(attempt);
  }

  @Override
  public CourseAttemptPageView listMyCourseAttempts(ListCourseAttemptsQuery query) {
    // When subscriptionId is provided, verify ownership first
    if (query.subscriptionId() != null) {
      Subscription sub =
          loadSubscriptionPort
              .loadById(query.subscriptionId())
              .orElseThrow(() -> new SubscriptionNotFoundException(query.subscriptionId()));
      if (!sub.getUserId().equals(query.actorId())) {
        // BOLA: return empty page (or 404 — here we return empty to not reveal existence)
        throw new SubscriptionNotFoundException(query.subscriptionId());
      }
    }

    List<CourseAttempt> attempts = loadCourseAttemptPort.loadBySubscriptionIdAndCourseId(
        query.subscriptionId(), query.courseId(), query.page(), query.size());
    long total = loadCourseAttemptPort.countBySubscriptionIdAndCourseId(
        query.subscriptionId(), query.courseId());

    List<CourseAttemptView> views = attempts.stream().map(this::toView).toList();
    int totalPages = query.size() > 0 ? (int) Math.ceil((double) total / query.size()) : 0;

    return new CourseAttemptPageView(views, query.page(), query.size(), total, totalPages);
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  private void validateOwnership(UUID subscriptionId, UUID actorId, UUID attemptId) {
    Subscription sub = loadSubscriptionPort.loadById(subscriptionId).orElse(null);
    if (sub == null || !sub.getUserId().equals(actorId)) {
      throw new AttemptNotFoundException(attemptId);
    }
  }

  CourseAttemptView toView(CourseAttempt attempt) {
    BigDecimal gradeValue = attempt.getGrade() != null ? attempt.getGrade().value() : null;
    return new CourseAttemptView(
        attempt.getId().value(),
        attempt.getSubscriptionId().value(),
        attempt.getCourseId(),
        "N/A", // courseName resolved by WU4
        attempt.getPeriodId(),
        null,   // periodName resolved by WU4
        attempt.getJourneyCode(),
        attempt.getStatus().name(), // FIX-S1: plain String, no domain type in application DTO
        gradeValue,
        attempt.getRegisteredAt(),
        attempt.getVersion());
  }
}
