package co.edu.uniquindio.brujula.imports.domain.model;

/**
 * Discriminates the type of data contained in an ImportBatch.
 *
 * <p>Pure domain enum — zero framework annotations (ARQ-002).
 */
public enum ImportKind {

  /** Bulk import of curriculum courses. */
  COURSES,

  /** Bulk import of course requirements (prerequisites/co-requisites). */
  REQUIREMENTS,

  /** Bulk import of course offerings for an academic period. */
  OFFERINGS
}
