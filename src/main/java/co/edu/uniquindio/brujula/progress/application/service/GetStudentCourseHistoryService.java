package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.progress.application.port.in.api.GetStudentCourseHistoryQuery;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.EffectiveAttemptResolver;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service: read-only student course history query for cross-module consumers.
 *
 * <p>Reuses {@link EffectiveAttemptResolver} and the existing {@link LoadCourseAttemptPort} and
 * {@link LoadSubscriptionPort} — ZERO change to progress domain behavior.
 *
 * <p>Academic history is SHARED across journeys: attempts are aggregated across ALL active
 * subscriptions of the same (student, program), so a course passed in any journey (DIURNA/NOCTURNA)
 * counts for the program. A grade belongs to the program, not the journey.
 *
 * <p>Framework-free: no Spring or JPA dependencies. Wired via {@code @Bean} in {@link
 * co.edu.uniquindio.brujula.progress.infrastructure.configuration.ProgressConfiguration}.
 */
public class GetStudentCourseHistoryService implements GetStudentCourseHistoryQuery {

  private static final String NONE_SENTINEL = "NONE";

  private final LoadSubscriptionPort loadSubscriptionPort;
  private final LoadCourseAttemptPort loadCourseAttemptPort;
  private final EffectiveAttemptResolver resolver;

  public GetStudentCourseHistoryService(
      LoadSubscriptionPort loadSubscriptionPort, LoadCourseAttemptPort loadCourseAttemptPort) {
    this.loadSubscriptionPort = loadSubscriptionPort;
    this.loadCourseAttemptPort = loadCourseAttemptPort;
    this.resolver = new EffectiveAttemptResolver();
  }

  @Override
  public Set<UUID> passedCourseIds(UUID ownerId, UUID programId) {
    return attemptsForProgram(ownerId, programId).stream()
        .collect(Collectors.groupingBy(CourseAttempt::getCourseId))
        .entrySet()
        .stream()
        .filter(
            e -> resolver.resolve(e.getValue()).map(s -> s == AttemptStatus.PASSED).orElse(false))
        .map(java.util.Map.Entry::getKey)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public String effectiveStatus(UUID ownerId, UUID programId, UUID courseId) {
    List<CourseAttempt> attemptsForCourse =
        attemptsForProgram(ownerId, programId).stream()
            .filter(a -> courseId.equals(a.getCourseId()))
            .toList();

    return resolver.resolve(attemptsForCourse).map(AttemptStatus::name).orElse(NONE_SENTINEL);
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /**
   * Aggregates attempts across ALL active subscriptions of (owner, program), so academic history is
   * shared across journeys (e.g. DIURNA + NOCTURNA) of the same program.
   */
  private List<CourseAttempt> attemptsForProgram(UUID ownerId, UUID programId) {
    return loadSubscriptionPort.loadActiveByUserId(ownerId).stream()
        .filter(s -> programId.equals(s.getProgramId()))
        .flatMap(s -> loadCourseAttemptPort.loadBySubscriptionId(s.getId().value()).stream())
        .toList();
  }
}
