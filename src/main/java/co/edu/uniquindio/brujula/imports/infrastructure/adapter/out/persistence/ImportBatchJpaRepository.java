package co.edu.uniquindio.brujula.imports.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for {@link ImportBatchJpaEntity}.
 *
 * <p>Package-private per ARQ-006b. Only {@link ImportBatchPersistenceAdapter} uses this repository
 * directly.
 */
interface ImportBatchJpaRepository extends JpaRepository<ImportBatchJpaEntity, UUID> {

  /**
   * List batches for a given program, with optional kind and status filters.
   *
   * <p>Null filter values are treated as "all" via JPQL conditional IS NULL check.
   */
  @Query(
      """
            SELECT b FROM ImportBatchJpaEntity b
            WHERE b.programId = :programId
              AND (:kind IS NULL OR b.kind = :kind)
              AND (:status IS NULL OR b.status = :status)
            ORDER BY b.createdAt DESC
            """)
  List<ImportBatchJpaEntity> findByProgram(
      @Param("programId") UUID programId,
      @Param("kind") String kind,
      @Param("status") String status);

  /** List all batches, with optional kind and status filters (for ADMIN access). */
  @Query(
      """
            SELECT b FROM ImportBatchJpaEntity b
            WHERE (:kind IS NULL OR b.kind = :kind)
              AND (:status IS NULL OR b.status = :status)
            ORDER BY b.createdAt DESC
            """)
  List<ImportBatchJpaEntity> findAllByFilters(
      @Param("kind") String kind, @Param("status") String status);

  /**
   * List batches for a set of programs in a single IN-clause query (QAL-PERF-001).
   *
   * <p>Replaces the O(N) per-program loop in {@code ListImportsService.loadBatchesForStaff} with
   * one prepared statement regardless of the number of programs.
   */
  @Query(
      """
            SELECT b FROM ImportBatchJpaEntity b
            WHERE b.programId IN :programIds
              AND (:kind IS NULL OR b.kind = :kind)
              AND (:status IS NULL OR b.status = :status)
            ORDER BY b.createdAt DESC
            """)
  List<ImportBatchJpaEntity> findByProgramIds(
      @Param("programIds") List<UUID> programIds,
      @Param("kind") String kind,
      @Param("status") String status);
}
