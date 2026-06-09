package co.edu.uniquindio.brujula.imports.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.SaveImportBatchPort;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * QAL-PERF-001 — Query-count regression test for {@link LoadImportBatchPort}.
 *
 * <p>TDD RED: Asserts that {@code findByProgramIds} issues exactly <strong>1</strong> SQL
 * prepared statement for N=10 programs, replacing the former O(N) per-program loop.
 *
 * <p>Uses Hibernate {@link Statistics} to count prepared statements; no mock counters.
 * Requires a real Testcontainers PostgreSQL via {@link AbstractPostgresIntegrationTest}.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class ImportBatchQueryCountTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private SaveImportBatchPort saveImportBatchPort;

    @Autowired
    private LoadImportBatchPort loadImportBatchPort;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SessionFactory sessionFactory;

    private Statistics stats;

    /** 10 distinct program IDs used for this test. */
    private static final int N = 10;
    private final List<UUID> programIds = new ArrayList<>();

    @BeforeEach
    void seedData() {
        stats = sessionFactory.getStatistics();

        // Seed one VALIDATED import batch per program
        for (int i = 0; i < N; i++) {
            UUID programId = UUID.randomUUID();
            programIds.add(programId);
            ImportBatch batch = ImportBatch.validated(
                    ImportBatchId.generate(),
                    ImportKind.COURSES,
                    programId,
                    UUID.randomUUID(),
                    null,
                    1, 1,
                    List.of(),
                    List.of(),
                    Instant.now());
            saveImportBatchPort.save(batch);
        }
        // Flush so all rows are visible to the subsequent read
        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void disableStats() {
        stats.setStatisticsEnabled(false);
    }

    /**
     * QAL-PERF-001-S1: staff with 10 assigned programs → exactly 1 SQL query.
     *
     * <p>Enables Hibernate statistics, resets the counter, calls the batch method, and asserts
     * that exactly 1 prepared statement was executed regardless of N.
     */
    @Test
    void findByProgramIds_withTenPrograms_issuesExactlyOneSqlQuery() {
        // Enable and clear statistics just before the measured call
        stats.setStatisticsEnabled(true);
        stats.clear();

        // Act — the batch method under test (this will FAIL until findByProgramIds is implemented)
        List<ImportBatch> result = loadImportBatchPort.findByProgramIds(
                programIds, null, null);

        // Assert — exactly 1 prepared statement issued
        long actualCount = stats.getPrepareStatementCount();
        assertThat(actualCount)
                .as("Expected exactly 1 SQL statement for %d programs, but got %d", N, actualCount)
                .isEqualTo(1);

        // Functional correctness: all 10 batches returned
        assertThat(result).hasSize(N);
    }
}
