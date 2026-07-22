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
 * @param filledElectiveName for a numbered display-slot course belonging to a pool requirement
 *     (PROP-001 design D5, e.g. "Profundización I"), the name of the specific pool elective the
 *     student passed for that slot; {@code null} when the slot has no filled elective (or this
 *     course is not a pool display slot at all)
 */
public record CurriculumCourseData(
    UUID courseId,
    String officialCode,
    String name,
    double credits,
    String type,
    String progressStatus,
    List<String> unmetRequirements,
    boolean offPlan,
    String filledElectiveName) {

  public CurriculumCourseData {
    Objects.requireNonNull(courseId, "courseId must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(progressStatus, "progressStatus must not be null");
    Objects.requireNonNull(unmetRequirements, "unmetRequirements must not be null");
  }

  /**
   * Convenience constructor preserving the original 8-arg shape (no {@code filledElectiveName}).
   * Backward compatible with all pre-PROP-001 call sites.
   */
  public CurriculumCourseData(
      UUID courseId,
      String officialCode,
      String name,
      double credits,
      String type,
      String progressStatus,
      List<String> unmetRequirements,
      boolean offPlan) {
    this(
        courseId,
        officialCode,
        name,
        credits,
        type,
        progressStatus,
        unmetRequirements,
        offPlan,
        null);
  }
}
