package co.edu.uniquindio.brujula.programcatalog.application.port.in;

import co.edu.uniquindio.brujula.programcatalog.application.dto.ProgramView;
import java.util.UUID;

/**
 * Input port: retrieve a single academic program by ID.
 *
 * <p>Backs {@code GET /programs/{programId}} (CAT-001). Throws
 * {@link co.edu.uniquindio.brujula.programcatalog.domain.ProgramNotFoundException}
 * (mapped to HTTP 404) when the program does not exist.
 *
 * <p>Accepts plain {@link UUID} (not a domain VO) so REST adapters do not need to import domain
 * types (ARQ-001). No Spring/framework imports (ARQ-003).
 */
public interface GetProgramQuery {

  /**
   * Return the program with the given ID.
   *
   * @param programId the program UUID
   * @return the {@link ProgramView} DTO
   * @throws co.edu.uniquindio.brujula.programcatalog.domain.ProgramNotFoundException if not found
   */
  ProgramView getProgram(UUID programId);
}
