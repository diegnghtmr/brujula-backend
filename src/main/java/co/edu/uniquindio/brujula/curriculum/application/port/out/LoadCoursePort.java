package co.edu.uniquindio.brujula.curriculum.application.port.out;

import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Output port: load courses from persistence.
 *
 * <p>Named by need (ISP) per hexagonal architecture conventions. No Spring/framework imports.
 */
public interface LoadCoursePort {

  /**
   * Load all courses belonging to a study plan.
   *
   * @param planId the study plan UUID
   * @return all courses (all statuses — filtering happens in the service)
   */
  List<Course> loadByPlanId(UUID planId);

  /**
   * Load a single course by its typed ID.
   *
   * @param courseId the course typed UUID
   * @return the course if found, empty otherwise
   */
  Optional<Course> loadById(CourseId courseId);

  /**
   * Load multiple courses by their UUIDs in a single batch query (QAL-PERF-002).
   *
   * <p>Course IDs not found in persistence are absent from the returned list.
   * Order is not guaranteed.
   *
   * @param courseIds the set of course UUIDs to load; must not be null
   * @return list of found courses (may be smaller than {@code courseIds.size()})
   */
  List<Course> loadByIds(Set<UUID> courseIds);
}
