package co.edu.uniquindio.brujula.progress.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * An unmet prerequisite for a course in the availability computation.
 *
 * <p>Used by {@link CourseAvailabilityService}. No Spring/JPA imports (ARQ-002).
 *
 * @param requiredCourseId the prerequisite course UUID
 * @param type the requirement type (e.g. COURSE_PASSED, SEQUENCE)
 * @param advisory true if this is a soft/advisory warning (inferred + MEDIUM/LOW confidence); false
 *     if this is a blocking requirement (HARD edge)
 */
public record UnmetRequirement(UUID requiredCourseId, String type, boolean advisory) {

  public UnmetRequirement {
    Objects.requireNonNull(requiredCourseId, "requiredCourseId must not be null");
    Objects.requireNonNull(type, "type must not be null");
  }
}
