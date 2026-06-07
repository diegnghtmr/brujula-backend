package co.edu.uniquindio.brujula.progress.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable view of a course within a plan, as consumed by domain services.
 *
 * <p>Input to {@link CourseAvailabilityService} and {@link ProgressSummaryCalculator}. Produced by
 * the application layer from the curriculum published port (ADR-F3-005). No Spring/JPA imports
 * (ARQ-002).
 *
 * @param courseId the course UUID
 * @param name the course name
 * @param credits number of credits (may be fractional)
 * @param type the course type string (REQUIRED, PERSONAL_ELECTIVE, etc.)
 * @param recommendedSemester recommended semester (1-based)
 */
public record PlanCourse(
    UUID courseId,
    String name,
    double credits,
    String type,
    int recommendedSemester) {

  public PlanCourse {
    Objects.requireNonNull(courseId, "courseId must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(type, "type must not be null");
  }
}
