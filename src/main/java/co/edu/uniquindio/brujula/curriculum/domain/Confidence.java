package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Confidence level of an inferred or semi-official prerequisite edge.
 *
 * <p>Used by the simulator (future) to decide whether to block enrollment or merely advise. Stored
 * Stored and exposed but NOT enforced at runtime.
 *
 * <p>Maps to the confidence column in {@code curriculum.course_requirement} and the OpenAPI {@code
 * confidence} enum extension on {@code CourseRequirement}.
 */
public enum Confidence {
  HIGH,
  MEDIUM,
  LOW
}
