package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identity value object for the {@link CourseRequirement} aggregate.
 *
 * <p>Immutable (record). No Spring/JPA imports.
 */
public record CourseRequirementId(UUID value) {

  public CourseRequirementId {
    Objects.requireNonNull(value, "CourseRequirementId value must not be null");
  }

  /** Factory: generate a new random CourseRequirementId. */
  public static CourseRequirementId generate() {
    return new CourseRequirementId(UUID.randomUUID());
  }
}
