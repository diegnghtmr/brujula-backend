package co.edu.uniquindio.brujula.imports.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

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
    @Query("""
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

    /**
     * List all batches, with optional kind and status filters (for ADMIN access).
     */
    @Query("""
            SELECT b FROM ImportBatchJpaEntity b
            WHERE (:kind IS NULL OR b.kind = :kind)
              AND (:status IS NULL OR b.status = :status)
            ORDER BY b.createdAt DESC
            """)
    List<ImportBatchJpaEntity> findAllByFilters(
            @Param("kind") String kind,
            @Param("status") String status);
}
