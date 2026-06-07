package co.edu.uniquindio.brujula.progress.application.port.in;

import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import co.edu.uniquindio.brujula.progress.application.dto.CreateCourseAttemptCommand;

/**
 * Input port: record a new course attempt for the authenticated student (PRG-1).
 *
 * <p>Idempotent via {@code IdempotencyStorePort}. Ownership (BOLA) validated via subscriptionId.
 */
public interface CreateCourseAttemptUseCase {

  /**
   * Record a course attempt.
   *
   * @param command the create command
   * @return the created attempt view
   */
  CourseAttemptView createCourseAttempt(CreateCourseAttemptCommand command);
}
