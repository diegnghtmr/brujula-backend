package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Confidence level of an inferred or semi-official prerequisite edge.
 *
 * <p>Used by the simulator (future) to decide whether to block enrollment or merely advise. Stored
 * and exposed in Fase 2 but NOT enforced at runtime (ADR-F2-004).
 *
 * <p>Maps to the confidence column in {@code curriculum.course_requirement} and the OpenAPI {@code
 * confidence} enum extension on {@code CourseRequirement}.
 */
public enum Confidence {
  HIGH,
  MEDIUM,
  LOW
}
