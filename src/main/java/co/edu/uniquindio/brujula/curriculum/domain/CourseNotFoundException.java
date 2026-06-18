package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Domain exception thrown when a course is not found by its ID.
 *
 * <p>Maps to HTTP 404 (Not Found) at the REST adapter layer. No Spring/JPA imports.
 */
public class CourseNotFoundException extends RuntimeException {

  public CourseNotFoundException(CourseId courseId) {
    super("Course not found: " + courseId.value());
  }
}
