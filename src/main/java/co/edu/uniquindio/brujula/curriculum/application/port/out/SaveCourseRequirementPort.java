package co.edu.uniquindio.brujula.curriculum.application.port.out;

import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: persist CourseRequirement aggregates (create, update, archive).
 *
 * <p>Named by need (ISP). No Spring/framework imports.
 *
 * <p>Contract: {@link #save} must flush after save so the {@code @Version} is incremented and
 * returned in the managed entity (optimistic-lock pattern, ADR-010).
 */
public interface SaveCourseRequirementPort {

  /**
   * Persist (insert or update) a CourseRequirement aggregate.
   *
   * @param requirement the requirement to persist
   * @return the managed, post-flush CourseRequirement with the updated version
   */
  CourseRequirement save(CourseRequirement requirement);

  /**
   * Load the current version of a CourseRequirement for write.
   *
   * @param id the requirement ID
   * @return the current requirement, or empty if not found
   */
  Optional<CourseRequirement> findCurrentById(CourseRequirementId id);

  /**
   * Load all requirements for a given plan (across all target courses in the plan). Used for cycle
   * detection against the real persisted graph.
   *
   * @param planId the study plan UUID
   * @return all requirements belonging to courses in the plan
   */
  List<CourseRequirement> findAllByPlanId(UUID planId);
}
