package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.progress.application.port.in.api.GetStudentCourseHistoryQuery;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.EffectiveAttemptResolver;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service: read-only student course history query for cross-module consumers.
 *
 * <p>Reuses {@link EffectiveAttemptResolver} and the existing {@link LoadCourseAttemptPort} and
 * {@link LoadSubscriptionPort} — ZERO change to progress domain behavior (ADR-F4-002).
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Load all active subscriptions for the student.
 *   <li>Filter to the subscription matching the requested programId.
 *   <li>Load all course attempts for that subscription.
 *   <li>Group by courseId and resolve the effective status per course.
 * </ol>
 *
 * <p>Framework-free: no Spring or JPA dependencies (ARQ-002). Wired via {@code @Bean} in
 * {@link co.edu.uniquindio.brujula.progress.infrastructure.configuration.ProgressConfiguration}.
 */
public class GetStudentCourseHistoryService implements GetStudentCourseHistoryQuery {

  private static final String NONE_SENTINEL = "NONE";

  private final LoadSubscriptionPort loadSubscriptionPort;
  private final LoadCourseAttemptPort loadCourseAttemptPort;
  private final EffectiveAttemptResolver resolver;

  public GetStudentCourseHistoryService(
      LoadSubscriptionPort loadSubscriptionPort,
      LoadCourseAttemptPort loadCourseAttemptPort) {
    this.loadSubscriptionPort = loadSubscriptionPort;
    this.loadCourseAttemptPort = loadCourseAttemptPort;
    this.resolver = new EffectiveAttemptResolver();
  }

  @Override
  public Set<UUID> passedCourseIds(UUID ownerId, UUID programId) {
    Optional<Subscription> subscription = findSubscription(ownerId, programId);
    if (subscription.isEmpty()) {
      return Set.of();
    }

    List<CourseAttempt> attempts =
        loadCourseAttemptPort.loadBySubscriptionId(subscription.get().getId().value());

    return attempts.stream()
        .collect(Collectors.groupingBy(CourseAttempt::getCourseId))
        .entrySet()
        .stream()
        .filter(e -> resolver.resolve(e.getValue())
            .map(s -> s == AttemptStatus.PASSED)
            .orElse(false))
        .map(java.util.Map.Entry::getKey)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public String effectiveStatus(UUID ownerId, UUID programId, UUID courseId) {
    Optional<Subscription> subscription = findSubscription(ownerId, programId);
    if (subscription.isEmpty()) {
      return NONE_SENTINEL;
    }

    List<CourseAttempt> allAttempts =
        loadCourseAttemptPort.loadBySubscriptionId(subscription.get().getId().value());

    List<CourseAttempt> attemptsForCourse =
        allAttempts.stream()
            .filter(a -> courseId.equals(a.getCourseId()))
            .toList();

    return resolver
        .resolve(attemptsForCourse)
        .map(AttemptStatus::name)
        .orElse(NONE_SENTINEL);
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  private Optional<Subscription> findSubscription(UUID ownerId, UUID programId) {
    return loadSubscriptionPort
        .loadActiveByUserId(ownerId)
        .stream()
        .filter(s -> programId.equals(s.getProgramId()))
        .findFirst();
  }
}
