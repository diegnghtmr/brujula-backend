package co.edu.uniquindio.brujula.progress.application.port.in;

import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import co.edu.uniquindio.brujula.progress.application.dto.UpdateCourseAttemptCommand;

/**
 * Input port: update an existing course attempt (PRG-3).
 *
 * <p>Ownership (BOLA) validated. Grade-status coherence enforced. Optimistic lock via version.
 */
public interface UpdateCourseAttemptUseCase {

  /**
   * Update an existing course attempt.
   *
   * @param command the update command including version for optimistic locking
   * @return the updated attempt view
   */
  CourseAttemptView updateCourseAttempt(UpdateCourseAttemptCommand command);
}
