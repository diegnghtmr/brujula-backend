package co.edu.uniquindio.brujula.progress.application.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application-layer DTO: a single course in the curriculum view with its computed availability.
 *
 * <p>Status is passed as a plain String (the enum name, e.g. "AVAILABLE", "BLOCKED") to avoid
 * domain type leakage into the application DTO layer.
 *
 * @param courseId the course UUID
 * @param officialCode nullable course code
 * @param name course name
 * @param credits credits as a double
 * @param type course type string (REQUIRED, PERSONAL_ELECTIVE, etc.)
 * @param progressStatus computed status name (PENDING/PASSED/FAILED/CANCELLED/AVAILABLE/BLOCKED)
 * @param unmetRequirements descriptions of unmet prerequisites (blocking or advisory); may be empty
 * @param offPlan true if this course is in the subscription's attempt list but NOT in the plan
 */
public record CurriculumCourseData(
    UUID courseId,
    String officialCode,
    String name,
    double credits,
    String type,
    String progressStatus,
    List<String> unmetRequirements,
    boolean offPlan) {

  public CurriculumCourseData {
    Objects.requireNonNull(courseId, "courseId must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(progressStatus, "progressStatus must not be null");
    Objects.requireNonNull(unmetRequirements, "unmetRequirements must not be null");
  }
}
