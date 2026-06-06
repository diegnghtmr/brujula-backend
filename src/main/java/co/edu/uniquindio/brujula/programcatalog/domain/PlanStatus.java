package co.edu.uniquindio.brujula.programcatalog.domain;

/**
 * Status of a {@link StudyPlan}.
 *
 * <ul>
 *   <li>DRAFT — under construction; hidden from student-role requests (CAT-002).
 *   <li>ACTIVE — published and visible to all authenticated users.
 *   <li>ARCHIVED — superseded; hidden from student-role requests (CAT-002).
 * </ul>
 *
 * <p>No Spring/JPA imports (ARQ-002).
 */
public enum PlanStatus {
  DRAFT,
  ACTIVE,
  ARCHIVED
}
