package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Type of a course requirement (prerequisite edge).
 *
 * <p>No Spring/JPA imports.
 *
 * <ul>
 *   <li>{@link #COURSE_PASSED} — the student must have approved a specific course.
 *   <li>{@link #SEQUENCE} — the course must be taken in sequence (simultaneity may be allowed).
 *   <li>{@link #MINIMUM_CREDITS} — the student must have accumulated a minimum credit count.
 *   <li>{@link #CONFIGURABLE} — custom rule evaluated by the simulator (deferred logic).
 * </ul>
 */
public enum RequirementType {
  COURSE_PASSED,
  SEQUENCE,
  MINIMUM_CREDITS,
  CONFIGURABLE
}
