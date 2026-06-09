package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.UpdateCourseRequirementCommand;

/**
 * Input port: update an existing CourseRequirement (optimistic lock, NOT idempotent).
 *
 * <p>Published via {@code curriculum::api} NamedInterface. No framework imports.
 */
public interface UpdateCourseRequirementUseCase {

  /**
   * Update a CourseRequirement's fields.
   *
   * @param command the update command
   * @return a view of the updated requirement
   * @throws org.springframework.dao.OptimisticLockingFailureException if the version is stale
   */
  CourseRequirementView updateCourseRequirement(UpdateCourseRequirementCommand command);
}
