package co.edu.uniquindio.brujula.programcatalog.domain;

/**
 * Status of an {@link AcademicProgram}.
 *
 * <p>ACTIVE: visible and queryable. INACTIVE: temporarily disabled. ARCHIVED: soft-deleted.
 * No Spring/JPA imports (ARQ-002).
 */
public enum ProgramStatus {
  ACTIVE,
  INACTIVE,
  ARCHIVED
}
