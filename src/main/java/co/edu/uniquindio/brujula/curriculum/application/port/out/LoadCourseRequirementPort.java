package co.edu.uniquindio.brujula.curriculum.application.port.out;

import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import java.util.List;
import java.util.UUID;

/**
 * Output port: load course requirements from persistence.
 *
 * <p>Named by need (ISP) per hexagonal architecture conventions. No Spring/framework imports.
 */
public interface LoadCourseRequirementPort {

  /**
   * Load all requirements where the target course matches the given course ID.
   *
   * @param targetCourseId the UUID of the course whose requirements are requested
   * @return the list of requirements (all statuses; filtering done in service if needed)
   */
  List<CourseRequirement> loadByTargetCourseId(UUID targetCourseId);
}
