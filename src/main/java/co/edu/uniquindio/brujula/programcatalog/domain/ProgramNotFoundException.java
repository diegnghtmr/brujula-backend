package co.edu.uniquindio.brujula.programcatalog.domain;

/**
 * Domain exception thrown when an academic program is not found by its ID.
 *
 * <p>Maps to HTTP 404 (Not Found) at the REST adapter layer (CAT-001, GetProgramQuery contract). No
 * Spring/JPA imports (ARQ-002).
 */
public class ProgramNotFoundException extends RuntimeException {

  public ProgramNotFoundException(ProgramId programId) {
    super("Academic program not found: " + programId.value());
  }
}
