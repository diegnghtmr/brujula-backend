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

  /**
   * Bulk load all requirements whose target course ID is in the given set.
   *
   * <p>Used by the malla assembly service to load all requirement edges for a plan in a single
   * query (avoids N+1 per course).
   *
   * @param courseIds the set of course UUIDs whose requirements should be loaded
   * @return all requirements targeting any of the given course IDs
   */
  List<CourseRequirement> loadByTargetCourseIds(List<UUID> courseIds);
}
