package co.edu.uniquindio.brujula.progress.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object: identity of a {@link CourseAttempt} aggregate.
 *
 * <p>No Spring/JPA imports.
 */
public record AttemptId(UUID value) {

  public AttemptId {
    Objects.requireNonNull(value, "AttemptId value must not be null");
  }

  public static AttemptId generate() {
    return new AttemptId(UUID.randomUUID());
  }

  public static AttemptId of(UUID value) {
    return new AttemptId(value);
  }
}
