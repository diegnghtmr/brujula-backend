package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;

/**
 * Input port: retrieve a single course by its typed ID.
 *
 * <p>Backs {@code GET /courses/{courseId}} (CUR-001).
 *
 * <p>No Spring/framework imports (ARQ-003 — all port.in interfaces must be interfaces).
 */
public interface GetCourseQuery {

  /**
   * Return the course view for the given course ID.
   *
   * @param courseId the typed course UUID
   * @return the {@link CourseView} DTO
   * @throws co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException if not found
   */
  CourseView getCourse(CourseId courseId);
}
