package co.edu.uniquindio.brujula.curriculum.application.port.out;

import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import java.util.List;
import java.util.Optional;
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
}
