package co.edu.uniquindio.brujula.imports.application.port.in;

import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;
import java.util.UUID;

/**
 * Input port: retrieve a single import batch by id (IMP-004).
 *
 * <p>STAFF actors are restricted to batches belonging to their assigned programs (403 otherwise).
 * ADMIN actors may retrieve any batch.
 *
 * <p>Published in the imports module. No Spring/JPA imports (ARQ-003).
 */
public interface GetImportUseCase {

  /**
   * Get a single import summary by id.
   *
   * @param importId the batch UUID
   * @param actorEmail the caller's institutional email (for STAFF scoping)
   * @param actorRole "STAFF" or "ADMIN"
   * @return the ImportSummary
   * @throws co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotFoundException if the
   *     batch does not exist (→ 404)
   * @throws co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException if STAFF
   *     is not assigned to the batch's program (→ 403)
   */
  ImportSummary get(UUID importId, String actorEmail, String actorRole);
}
