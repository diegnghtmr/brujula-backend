package co.edu.uniquindio.brujula.imports.application.port.in;

import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;
import co.edu.uniquindio.brujula.imports.application.dto.UploadImportCommand;

/**
 * Input port: upload and validate an import file.
 *
 * <p>Orchestrates: idempotency check → file type/size guard → assignment guard → parse → validate →
 * persist ImportBatch (VALIDATED or INVALID) → audit → idempotency save → return ImportSummary.
 *
 * <p>Published in the imports module api. No Spring/JPA imports.
 */
public interface UploadImportUseCase {

  /**
   * Process an import upload request.
   *
   * @param command the upload command carrying file bytes, actor identity, and target program refs
   * @return the import summary (from idempotency replay or freshly created)
   * @throws co.edu.uniquindio.brujula.imports.domain.exception.UnsupportedFileTypeException if the
   *     file content-type or extension is not supported (→ 415)
   * @throws co.edu.uniquindio.brujula.imports.domain.exception.FileSizeExceededException if the
   *     file exceeds the configured maximum size (→ 413)
   * @throws co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException if STAFF
   *     is not assigned to the target program or account is inactive (→ 403)
   * @throws co.edu.uniquindio.brujula.imports.domain.exception.MissingImportParameterException if
   *     planId/periodId is absent for the requested kind (→ 400)
   * @throws
   *     co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException
   *     if the idempotency key was already used with a different request body (→ 409)
   */
  ImportSummary upload(UploadImportCommand command);
}
