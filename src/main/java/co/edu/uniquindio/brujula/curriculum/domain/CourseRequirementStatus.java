package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Lifecycle status of a {@link CourseRequirement}.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — the requirement is currently enforced.
 *   <li>{@link #ARCHIVED} — soft-deleted; no longer enforced.
 * </ul>
 *
 * <p>No Spring/JPA imports.
 */
public enum CourseRequirementStatus {
  ACTIVE,
  ARCHIVED
}
