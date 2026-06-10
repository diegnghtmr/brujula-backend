package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseRequirementCommand;

/**
 * Input port: create a new CourseRequirement (idempotent, with cycle check).
 *
 * <p>Published via {@code curriculum::api} NamedInterface so that the {@code imports} module can
 * call it for REQUIREMENTS confirm (WU6). No framework imports.
 */
public interface CreateCourseRequirementUseCase {

  /**
   * Create a new CourseRequirement.
   *
   * <p>Validates prerequisite graph acyclicity against the real persisted graph before persisting.
   * If adding this requirement would introduce a cycle, throws {@link
   * co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteCycleException}.
   *
   * <p>Idempotency: same key + same hash → returns stored view; same key + different hash → {@link
   * co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException}.
   *
   * @param command the creation command
   * @return a view of the created (or replayed) requirement
   */
  CourseRequirementView createCourseRequirement(CreateCourseRequirementCommand command);
}
