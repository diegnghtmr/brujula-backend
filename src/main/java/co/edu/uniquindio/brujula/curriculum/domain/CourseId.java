package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identity value object for the {@link Course} aggregate.
 *
 * <p>Wraps a {@link UUID} to prevent accidental mixing of IDs at the type level. Immutable
 * (record). No Spring/JPA imports.
 */
public record CourseId(UUID value) {

  public CourseId {
    Objects.requireNonNull(value, "CourseId value must not be null");
  }

  /** Factory: generate a new random CourseId. */
  public static CourseId generate() {
    return new CourseId(UUID.randomUUID());
  }
}
