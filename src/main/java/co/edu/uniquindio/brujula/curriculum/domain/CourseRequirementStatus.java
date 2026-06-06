package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Lifecycle status of a {@link CourseRequirement}.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — the requirement is currently enforced.
 *   <li>{@link #ARCHIVED} — soft-deleted; no longer enforced (RN-005).
 * </ul>
 *
 * <p>No Spring/JPA imports (ARQ-002).
 */
public enum CourseRequirementStatus {
  ACTIVE,
  ARCHIVED
}
