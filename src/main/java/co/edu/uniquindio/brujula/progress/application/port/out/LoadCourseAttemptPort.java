package co.edu.uniquindio.brujula.progress.application.port.out;

import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: read-side operations for {@link CourseAttempt}.
 *
 * <p>Application layer depends on this interface, not on concrete persistence adapters.
 */
public interface LoadCourseAttemptPort {

  /**
   * Load a course attempt by its ID.
   *
   * @param attemptId the attempt UUID
   * @return the attempt if found, empty otherwise
   */
  Optional<CourseAttempt> loadById(UUID attemptId);

  /**
   * Load all course attempts for a given subscription (for listing and effective-status
   * resolution).
   *
   * @param subscriptionId the owning subscription UUID
   * @return list of attempts (may be empty)
   */
  List<CourseAttempt> loadBySubscriptionId(UUID subscriptionId);

  /**
   * Load course attempts for a given subscription, optionally filtered by courseId.
   *
   * @param subscriptionId the owning subscription UUID
   * @param courseId optional course filter (null = all courses)
   * @param page 0-based page number
   * @param size page size
   * @return list of matching attempts
   */
  List<CourseAttempt> loadBySubscriptionIdAndCourseId(
      UUID subscriptionId, UUID courseId, int page, int size);

  /**
   * Count the total number of attempts for a given subscription and optional courseId filter.
   *
   * @param subscriptionId the owning subscription UUID
   * @param courseId optional course filter (null = all courses)
   * @return total count
   */
  long countBySubscriptionIdAndCourseId(UUID subscriptionId, UUID courseId);

  /**
   * Load all attempts across ALL active subscriptions of a (user, program) pair, so the academic
   * history is shared across journeys (e.g. DIURNA + NOCTURNA) of the same program.
   *
   * @param userId the owning student UUID
   * @param programId the program UUID
   * @return list of attempts across all the student's active subscriptions for that program
   */
  List<CourseAttempt> loadByUserIdAndProgramId(UUID userId, UUID programId);

  /**
   * Paged variant of {@link #loadByUserIdAndProgramId}, optionally filtered by courseId, ordered by
   * most recent attempt first.
   *
   * @param userId the owning student UUID
   * @param programId the program UUID
   * @param courseId optional course filter (null = all courses)
   * @param page 0-based page number
   * @param size page size
   * @return list of matching attempts across the student's active subscriptions for that program
   */
  List<CourseAttempt> loadByUserIdAndProgramIdAndCourseId(
      UUID userId, UUID programId, UUID courseId, int page, int size);

  /**
   * Count attempts across all active subscriptions of a (user, program), optional courseId filter.
   *
   * @param userId the owning student UUID
   * @param programId the program UUID
   * @param courseId optional course filter (null = all courses)
   * @return total count across the student's active subscriptions for that program
   */
  long countByUserIdAndProgramIdAndCourseId(UUID userId, UUID programId, UUID courseId);
}
