package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Domain exception thrown when a course is not found by its ID.
 *
 * <p>Maps to HTTP 404 (Not Found) at the REST adapter layer (CUR-001, GetCourseQuery contract). No
 * Spring/JPA imports (ARQ-002).
 */
public class CourseNotFoundException extends RuntimeException {

  public CourseNotFoundException(CourseId courseId) {
    super("Course not found: " + courseId.value());
  }
}
