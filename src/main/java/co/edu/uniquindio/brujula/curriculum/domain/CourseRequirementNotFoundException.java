package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Domain exception thrown when a CourseRequirement is not found by its ID.
 *
 * <p>Maps to HTTP 404 (Not Found) at the REST adapter layer. No Spring/JPA imports.
 */
public class CourseRequirementNotFoundException extends RuntimeException {

  public CourseRequirementNotFoundException(CourseRequirementId requirementId) {
    super("CourseRequirement not found: " + requirementId.value());
  }
}
