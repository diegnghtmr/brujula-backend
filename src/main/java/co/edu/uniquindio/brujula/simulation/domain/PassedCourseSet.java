package co.edu.uniquindio.brujula.simulation.domain;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable set of course UUIDs the student has effectively passed.
 *
 * <p>Populated from {@code ProgressReadPort.passedCourseIds()} and injected into validation rules
 * as a pure value (no port calls inside domain).
 *
 * @param courseIds the set of passed course UUIDs (defensive copy; never null)
 */
public record PassedCourseSet(Set<UUID> courseIds) {

  public PassedCourseSet {
    Objects.requireNonNull(courseIds, "courseIds required");
    courseIds = Set.copyOf(courseIds);
  }

  /** Returns true when the student has passed the given course. */
  public boolean hasPassed(UUID courseId) {
    return courseIds.contains(courseId);
  }

  /** Convenience factory for an empty set. */
  public static PassedCourseSet empty() {
    return new PassedCourseSet(Set.of());
  }
}
