package co.edu.uniquindio.brujula.progress.domain;

import java.util.List;
import java.util.Optional;

/**
 * Domain service: resolves the effective declared {@link AttemptStatus} for a course from a list of
 * multiple attempts.
 *
 * <p>Rule (ADR-F3-006): the effective status is the most favorable:
 *
 * <ol>
 *   <li>Any PASSED attempt ⇒ {@code Optional.of(PASSED)} (regardless of subsequent FAILED or
 *       CANCELLED).
 *   <li>Any FAILED attempt (and no PASSED) ⇒ {@code Optional.of(FAILED)}.
 *   <li>Only CANCELLED attempts ⇒ {@code Optional.of(CANCELLED)}.
 *   <li>No attempts ⇒ {@code Optional.empty()} — caller decides how to interpret "not attempted"
 *       (FIX-W2: removed the PENDING sentinel; PENDING is not in the contract enum).
 * </ol>
 *
 * <p>Pure function (no ports, no Spring). Unit-tested in {@code EffectiveAttemptResolverTest}.
 */
public class EffectiveAttemptResolver {

  /**
   * Resolve the effective status from a list of attempts for a single (subscriptionId, courseId).
   *
   * @param attempts list of attempts (may be empty or null)
   * @return the most favorable effective declared status, or empty if there are no attempts
   */
  public Optional<AttemptStatus> resolve(List<CourseAttempt> attempts) {
    if (attempts == null || attempts.isEmpty()) {
      return Optional.empty();
    }

    boolean hasPassed = false;
    boolean hasFailed = false;

    for (CourseAttempt attempt : attempts) {
      switch (attempt.getStatus()) {
        case PASSED -> hasPassed = true;
        case FAILED -> hasFailed = true;
        case CANCELLED -> {
          /* continue — least favorable */
        }
      }
    }

    if (hasPassed) return Optional.of(AttemptStatus.PASSED);
    if (hasFailed) return Optional.of(AttemptStatus.FAILED);
    return Optional.of(AttemptStatus.CANCELLED);
  }
}
