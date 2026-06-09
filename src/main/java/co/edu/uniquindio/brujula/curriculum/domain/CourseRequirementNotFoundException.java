package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Domain exception thrown when a CourseRequirement is not found by its ID.
 *
 * <p>Maps to HTTP 404 (Not Found) at the REST adapter layer (CUR-003).
 * No Spring/JPA imports (ARQ-002).
 */
public class CourseRequirementNotFoundException extends RuntimeException {

  public CourseRequirementNotFoundException(CourseRequirementId requirementId) {
    super("CourseRequirement not found: " + requirementId.value());
  }
}
