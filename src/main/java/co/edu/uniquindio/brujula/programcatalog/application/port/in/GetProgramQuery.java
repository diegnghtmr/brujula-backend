package co.edu.uniquindio.brujula.programcatalog.application.port.in;

import co.edu.uniquindio.brujula.programcatalog.application.dto.ProgramView;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramId;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramNotFoundException;

/**
 * Input port: retrieve a single academic program by ID.
 *
 * <p>Backs {@code GET /programs/{programId}} (CAT-001). Throws {@link ProgramNotFoundException}
 * (mapped to HTTP 404) when the program does not exist.
 *
 * <p>No Spring/framework imports (ARQ-003).
 */
public interface GetProgramQuery {

  /**
   * Return the program with the given ID.
   *
   * @param programId the program typed UUID
   * @return the {@link ProgramView} DTO
   * @throws ProgramNotFoundException if no program with that ID exists
   */
  ProgramView getProgram(ProgramId programId);
}
