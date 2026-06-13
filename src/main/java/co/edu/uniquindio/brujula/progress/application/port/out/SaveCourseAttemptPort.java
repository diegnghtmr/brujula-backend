package co.edu.uniquindio.brujula.progress.application.port.out;

import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;

/**
 * Output port: write-side operations for {@link CourseAttempt}.
 *
 * <p>Application layer depends on this interface, not on concrete persistence adapters.
 */
public interface SaveCourseAttemptPort {

  /**
   * Persist a course attempt (insert or update).
   *
   * <p>Returns the managed domain object with the post-flush version (mirrors {@code
   * SaveSubscriptionPort.save} pattern).
   *
   * @param attempt the attempt to persist
   * @return the saved attempt with the DB-incremented version
   */
  CourseAttempt save(CourseAttempt attempt);

  /**
   * Hard-delete a course attempt by the internal entity. In practice, the domain records a deletion
   * audit event before calling this.
   *
   * @param attempt the attempt to delete
   */
  void delete(CourseAttempt attempt);
}
