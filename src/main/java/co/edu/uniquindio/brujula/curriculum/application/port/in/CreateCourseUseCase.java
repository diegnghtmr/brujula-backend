package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseCommand;

/**
 * Input port: create a new Course (idempotent).
 *
 * <p>Published via {@code curriculum::api} NamedInterface so that the {@code imports} module
 * can depend on it for confirm-import (WU6). No framework imports.
 */
public interface CreateCourseUseCase {

  /**
   * Create a new Course.
   *
   * <p>Idempotency: if a record for {@code command.idempotencyKey()} already exists with the same
   * request hash, returns the previously stored view without creating a duplicate.
   * If the same key is reused with a different hash, throws
   * {@link co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException}.
   *
   * @param command the creation command
   * @return a view of the created (or replayed) course
   */
  CourseView createCourse(CreateCourseCommand command);
}
