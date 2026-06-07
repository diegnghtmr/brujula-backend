package co.edu.uniquindio.brujula.progress.application.port.out;

import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: read-side operations for {@link CourseAttempt}.
 *
 * <p>ARQ-003: application layer depends on this interface, not on concrete persistence adapters.
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
}
