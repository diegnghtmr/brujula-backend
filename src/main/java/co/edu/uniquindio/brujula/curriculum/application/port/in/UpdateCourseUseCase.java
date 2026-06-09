package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.dto.UpdateCourseCommand;

/**
 * Input port: update an existing Course (optimistic lock, NOT idempotent).
 *
 * <p>Published via {@code curriculum::api} NamedInterface. No framework imports.
 */
public interface UpdateCourseUseCase {

  /**
   * Update course fields.
   *
   * @param command the update command (contains expectedVersion for optimistic lock)
   * @return a view of the updated course with the new version
   * @throws org.springframework.dao.OptimisticLockingFailureException if the version is stale
   * @throws co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException if course not found
   */
  CourseView updateCourse(UpdateCourseCommand command);
}
