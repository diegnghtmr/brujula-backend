package co.edu.uniquindio.brujula.progress.application.port.in;

import java.util.UUID;

/**
 * Input port: delete a course attempt for the authenticated student (PRG-4).
 *
 * <p>Hard-deletes the attempt row but records an audit event (COURSE_ATTEMPT_DELETED).
 */
public interface DeleteCourseAttemptUseCase {

  /**
   * Delete a course attempt.
   *
   * @param attemptId the attempt UUID
   * @param actorId the authenticated student's user ID (BOLA validation)
   */
  void deleteCourseAttempt(UUID attemptId, UUID actorId);
}
