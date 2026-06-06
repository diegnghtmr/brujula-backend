package co.edu.uniquindio.brujula.curriculum.domain;

/**
 * Lifecycle status of a {@link Course} aggregate.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — visible and enrollable.
 *   <li>{@link #INACTIVE} — temporarily disabled; hidden from student views.
 *   <li>{@link #ARCHIVED} — soft-deleted per RN-005; hidden from all non-admin views.
 * </ul>
 *
 * <p>No Spring/JPA imports (ARQ-002).
 */
public enum CourseStatus {
  ACTIVE,
  INACTIVE,
  ARCHIVED
}
