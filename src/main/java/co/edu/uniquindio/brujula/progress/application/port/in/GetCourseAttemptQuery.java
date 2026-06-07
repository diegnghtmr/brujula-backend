package co.edu.uniquindio.brujula.progress.application.port.in;

import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import java.util.UUID;

/**
 * Input port: get a single course attempt for the authenticated student (PRG-2).
 *
 * <p>Ownership (BOLA) enforced: returns 404 if the attempt belongs to another user.
 */
public interface GetCourseAttemptQuery {

  /**
   * Get a course attempt by ID, validating ownership.
   *
   * @param attemptId the attempt UUID
   * @param actorId the authenticated student's user ID
   * @return the attempt view
   */
  CourseAttemptView getCourseAttempt(UUID attemptId, UUID actorId);
}
