package co.edu.uniquindio.brujula.progress.domain;

import java.util.List;
import java.util.Objects;

/**
 * Computed availability result for a single course in the curriculum view.
 *
 * <p>Output of {@link CourseAvailabilityService}. No Spring/JPA imports (ARQ-002).
 *
 * @param status the computed {@link CourseProgressStatus} for the course
 * @param unmetRequirements list of unmet prerequisites (blocking or advisory). Non-null; empty when
 *     there are no unmet requirements
 */
public record CourseAvailability(CourseProgressStatus status, List<UnmetRequirement> unmetRequirements) {

  public CourseAvailability {
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(unmetRequirements, "unmetRequirements must not be null");
  }
}
