package co.edu.uniquindio.brujula.imports.application.port.out;

import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: load ImportBatch aggregates from the persistence store.
 *
 * <p>Provides read operations for:
 *
 * <ul>
 *   <li>GET by id (IMP-004, IMP-005 — confirm needs VALIDATED batch)
 *   <li>LIST by program (IMP-003 — STAFF access-scoped list)
 * </ul>
 */
public interface LoadImportBatchPort {

  /**
   * Find an ImportBatch by its id.
   *
   * <p>Raises {@link
   * co.edu.uniquindio.brujula.imports.domain.exception.SchemaVersionMismatchException} if the
   * stored {@code schema_version} does not match {@link
   * co.edu.uniquindio.brujula.imports.domain.model.ImportBatch#CURRENT_SCHEMA_VERSION}.
   *
   * @param id the batch id
   * @return the batch if present; empty otherwise
   */
  Optional<ImportBatch> findById(ImportBatchId id);

  /**
   * List ImportBatches associated with a given program, optionally filtered by kind/status.
   *
   * <p>Used by the list endpoint (IMP-003) with STAFF access scoping.
   *
   * @param programId the program to scope results to
   * @param kind optional kind filter; null means all kinds
   * @param status optional status filter; null means all statuses
   * @return list of matching batches (unordered; ordering applied at service layer)
   */
  List<ImportBatch> findByProgram(UUID programId, ImportKind kind, ImportStatus status);

  /**
   * List ImportBatches for a set of programs in a single batch query (QAL-PERF-001).
   *
   * <p>Replaces the per-program {@link #findByProgram(UUID, ImportKind, ImportStatus)} loop in
   * {@code ListImportsService.loadBatchesForStaff} with a single IN-clause query, reducing N
   * programs from N round-trips to exactly 1.
   *
   * @param programIds the program UUIDs to include; must not be null or empty
   * @param kind optional kind filter; null means all kinds
   * @param status optional status filter; null means all statuses
   * @return list of matching batches across all given programs (unordered)
   */
  List<ImportBatch> findByProgramIds(List<UUID> programIds, ImportKind kind, ImportStatus status);

  /**
   * List ALL ImportBatches, optionally filtered by kind/status.
   *
   * <p>Used by the list endpoint (IMP-003) for ADMIN actors who see all programs.
   *
   * @param kind optional kind filter; null means all kinds
   * @param status optional status filter; null means all statuses
   * @return list of all matching batches
   */
  List<ImportBatch> findAll(ImportKind kind, ImportStatus status);
}
