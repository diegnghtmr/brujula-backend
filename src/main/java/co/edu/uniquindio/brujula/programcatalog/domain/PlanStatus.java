package co.edu.uniquindio.brujula.programcatalog.domain;

/**
 * Status of a {@link StudyPlan}.
 *
 * <ul>
 *   <li>DRAFT — under construction; hidden from student-role requests.
 *   <li>ACTIVE — published and visible to all authenticated users.
 *   <li>ARCHIVED — superseded; hidden from student-role requests.
 * </ul>
 *
 * <p>No Spring/JPA imports.
 */
public enum PlanStatus {
  DRAFT,
  ACTIVE,
  ARCHIVED
}
