package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import java.util.UUID;

/**
 * Input port: retrieve a single course by its ID.
 *
 * <p>Backs {@code GET /courses/{courseId}} (CUR-001).
 *
 * <p>No Spring/framework imports (ARQ-003 — all port.in interfaces must be interfaces).
 * Accepts plain {@link UUID} (not a domain VO) so REST adapters do not need to import domain types
 * (ARQ-001).
 */
public interface GetCourseQuery {

  /**
   * Return the course view for the given course ID.
   *
   * @param courseId the course UUID
   * @return the {@link CourseView} DTO
   * @throws co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException if not found
   */
  CourseView getCourse(UUID courseId);
}
