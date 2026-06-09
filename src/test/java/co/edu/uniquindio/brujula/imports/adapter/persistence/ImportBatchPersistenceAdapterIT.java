package co.edu.uniquindio.brujula.imports.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.SaveImportBatchPort;
import co.edu.uniquindio.brujula.imports.domain.exception.SchemaVersionMismatchException;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * TDD T1.2 — Persistence round-trip integration tests for ImportBatch.
 *
 * <p>Uses the shared Testcontainers PostgreSQL via {@link AbstractPostgresIntegrationTest}.
 * Tests verify JSONB round-trip, optimistic lock version, and schema version guard.
 *
 * <p>Covers: IMP-001, IMP-003, IMP-004, IMP-005 (persistence side), NFR-006.
 */
@SpringBootTest
@Transactional
class ImportBatchPersistenceAdapterIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private SaveImportBatchPort saveImportBatchPort;

    @Autowired
    private LoadImportBatchPort loadImportBatchPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private static final UUID PROGRAM_ID = UUID.fromString("00000001-0000-0000-0000-000000000001");
    private static final UUID PLAN_ID = UUID.fromString("00000001-0000-0000-0000-000000000002");

    // -------------------------------------------------------------------------
    // T1.2-1: save and load VALIDATED batch — all fields round-trip
    // -------------------------------------------------------------------------

    @Test
    void save_and_load_validated_batch_round_trips_all_fields() {
        List<ImportRow> rows = List.of(
                new ImportRow(0, Map.of("course_name", "Cálculo Diferencial", "credits", "4")),
                new ImportRow(1, Map.of("course_name", "Álgebra Lineal", "credits", "3")));
        List<ImportIssue> issues = List.of(
                ImportIssue.warning(1, "official_code", "COURSE_ALREADY_EXISTS", "Already exists"));

        ImportBatch batch = ImportBatch.validated(
                ImportBatchId.generate(),
                ImportKind.COURSES,
                PROGRAM_ID,
                PLAN_ID,
                null,
                2, 2,
                issues, rows,
                Instant.now());

        ImportBatch saved = saveImportBatchPort.save(batch);
        Optional<ImportBatch> loaded = loadImportBatchPort.findById(saved.getId());

        assertThat(loaded).isPresent();
        ImportBatch result = loaded.get();

        assertThat(result.getId()).isEqualTo(batch.getId());
        assertThat(result.getKind()).isEqualTo(ImportKind.COURSES);
        assertThat(result.getProgramId()).isEqualTo(PROGRAM_ID);
        assertThat(result.getPlanId()).isEqualTo(PLAN_ID);
        assertThat(result.getPeriodId()).isNull();
        assertThat(result.getStatus()).isEqualTo(ImportStatus.VALIDATED);
        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getValidRows()).isEqualTo(2);
        assertThat(result.getSchemaVersion()).isEqualTo(ImportBatch.CURRENT_SCHEMA_VERSION);

        // JSONB round-trip: issues
        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).code()).isEqualTo("COURSE_ALREADY_EXISTS");

        // JSONB round-trip: validatedRows
        assertThat(result.getValidatedRows()).hasSize(2);
        assertThat(result.getValidatedRows().get(0).cells()).containsEntry("course_name", "Cálculo Diferencial");
    }

    // -------------------------------------------------------------------------
    // T1.2-2: optimistic lock version increments on update
    // -------------------------------------------------------------------------

    @Test
    void optimistic_lock_version_increments_on_update() {
        ImportBatch batch = validatedBatch();
        ImportBatch saved = saveImportBatchPort.save(batch);

        assertThat(saved.getVersion()).isEqualTo(0L);

        // Simulate a status update by marking confirmed and saving again
        saved.markConfirmed();
        ImportBatch updated = saveImportBatchPort.save(saved);

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    // -------------------------------------------------------------------------
    // T1.2-3: load raises schema version mismatch when version differs
    // -------------------------------------------------------------------------

    @Test
    void load_raises_schema_version_mismatch_when_version_differs() {
        ImportBatch batch = validatedBatch();
        ImportBatch saved = saveImportBatchPort.save(batch);

        // Manually corrupt the schema_version in DB to simulate a mismatch
        jdbcTemplate.update(
                "UPDATE imports.import_batch SET schema_version = 999 WHERE id = ?",
                saved.getId().value());

        // Evict the entity from the first-level cache so the next load hits the DB
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> loadImportBatchPort.findById(saved.getId()))
                .isInstanceOf(SchemaVersionMismatchException.class)
                .hasMessageContaining("SCHEMA_VERSION_MISMATCH");
    }

    // -------------------------------------------------------------------------
    // T1.2-4: load returns empty for unknown id
    // -------------------------------------------------------------------------

    @Test
    void load_returns_empty_for_unknown_id() {
        Optional<ImportBatch> result = loadImportBatchPort.findById(ImportBatchId.generate());
        assertThat(result).isEmpty();
    }

    // ---- helpers ----

    private ImportBatch validatedBatch() {
        return ImportBatch.validated(
                ImportBatchId.generate(),
                ImportKind.COURSES,
                PROGRAM_ID,
                PLAN_ID,
                null,
                3, 3,
                List.of(),
                List.of(new ImportRow(0, Map.of("course_name", "Test Course"))),
                Instant.now());
    }
}
