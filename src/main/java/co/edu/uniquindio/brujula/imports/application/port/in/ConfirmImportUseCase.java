package co.edu.uniquindio.brujula.imports.application.port.in;

import co.edu.uniquindio.brujula.imports.application.dto.ConfirmImportCommand;
import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;

/**
 * Input port: confirm a VALIDATED ImportBatch, atomically replaying all stored rows through the
 * appropriate write use-cases.
 *
 * <p>The confirm operation is annotated {@code @Transactional} in its service implementation. All
 * row writes are performed inside a single physical transaction. Any failure on any row propagates
 * and rolls back ALL writes — no partial commits.
 *
 * <p>Published in the imports module application layer. No Spring/JPA imports.
 */
public interface ConfirmImportUseCase {

  /**
   * Confirm a validated ImportBatch.
   *
   * <p>Pipeline:
   *
   * <ol>
   *   <li>Idempotency check — replay if same key+hash; conflict if key reused with different hash
   *   <li>Load ImportBatch — not found → 404
   *   <li>Assignment guard — STAFF must be active and assigned; ADMIN bypasses
   *   <li>Optimistic-lock check — expectedVersion mismatch → 409
   *   <li>Domain guard — only VALIDATED batches are confirmable → 409 for INVALID/CONFIRMED
   *   <li>Atomic row replay (inside the {@code @Transactional} boundary): COURSES → {@code
   *       CreateCourseUseCase}; REQUIREMENTS → {@code CreateCourseRequirementUseCase}; OFFERINGS →
   *       not yet supported (throws UnsupportedOperationException)
   *   <li>Mark batch CONFIRMED, persist
   *   <li>Record audit entry IMPORT_CONFIRMED
   *   <li>Store idempotency record (inside the TX)
   *   <li>Return ImportSummary with status CONFIRMED
   * </ol>
   *
   * @param command the confirm command
   * @return ImportSummary with status CONFIRMED (or replayed from idempotency store)
   * @throws co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotFoundException if no
   *     batch with the given importId exists (→ 404)
   * @throws co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException if STAFF
   *     actor is not assigned to the batch's program (→ 403)
   * @throws co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotConfirmableException
   *     if the batch is INVALID or CONFIRMED (→ 409)
   * @throws
   *     co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException
   *     if the idempotency key was reused with a different body (→ 409)
   * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if the expectedVersion
   *     does not match the stored version (→ 409)
   */
  ImportSummary confirm(ConfirmImportCommand command);
}
