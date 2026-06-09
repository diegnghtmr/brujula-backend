package co.edu.uniquindio.brujula.imports.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotConfirmableException;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;
import co.edu.uniquindio.brujula.imports.domain.model.Severity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TDD T1.1 — Domain invariant tests for ImportBatch aggregate.
 *
 * <p>Tests are written RED first (no implementation exists). Covers INV-01, INV-02, INV-04,
 * NFR-006, IMP-001, IMP-002.
 */
class ImportBatchTest {

    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();

    // ---- INV-01, INV-02: confirm guards ----

    /**
     * INV-01: A CONFIRMED batch cannot be re-confirmed.
     */
    @Test
    void confirmed_batch_cannot_be_reconfirmed() {
        ImportBatch batch = validatedBatch(5, 5, List.of());
        batch.markConfirmed();

        assertThatThrownBy(batch::markConfirmed)
                .isInstanceOf(ImportBatchNotConfirmableException.class)
                .hasMessageContaining("CONFIRMED");
    }

    /**
     * INV-02: An INVALID batch cannot be confirmed.
     */
    @Test
    void invalid_batch_cannot_be_confirmed() {
        ImportBatch batch = invalidBatch(List.of(
                ImportIssue.error(0, null, "EMPTY_FILE", "No data rows")));

        assertThatThrownBy(batch::markConfirmed)
                .isInstanceOf(ImportBatchNotConfirmableException.class)
                .hasMessageContaining("INVALID");
    }

    /**
     * INV-04: Validated rows carry no published status — status field is ImportStatus, not
     * any offering/course status.
     */
    @Test
    void validated_rows_are_always_draft() {
        ImportBatch batch = validatedBatch(3, 3, List.of());
        // Status is ImportStatus (VALIDATED), not a course/offering publication status
        assertThat(batch.getStatus()).isEqualTo(ImportStatus.VALIDATED);
        assertThat(batch.getStatus()).isNotNull();
    }

    /**
     * NFR-006: schemaVersion is set on construction and is positive.
     */
    @Test
    void schema_version_is_set_on_construction() {
        ImportBatch batch = validatedBatch(3, 3, List.of());
        assertThat(batch.getSchemaVersion()).isGreaterThan(0);
    }

    /**
     * validRows must never exceed totalRows.
     */
    @Test
    void valid_rows_never_exceeds_total_rows() {
        List<ImportRow> rows = List.of(new ImportRow(0, Map.of("x", "y")));
        assertThatThrownBy(
                () -> ImportBatch.validated(
                        ImportBatchId.generate(),
                        ImportKind.COURSES,
                        PROGRAM_ID,
                        PLAN_ID,
                        null,
                        11, // validRows
                        10, // totalRows
                        List.of(),
                        rows,
                        Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("validRows");
    }

    /**
     * A VALIDATED batch transitions to CONFIRMED successfully.
     */
    @Test
    void validated_batch_can_be_confirmed() {
        ImportBatch batch = validatedBatch(5, 5, List.of());
        assertThat(batch.getStatus()).isEqualTo(ImportStatus.VALIDATED);

        batch.markConfirmed();

        assertThat(batch.getStatus()).isEqualTo(ImportStatus.CONFIRMED);
    }

    // ---- helpers ----

    private static ImportBatch validatedBatch(int validRows, int totalRows, List<ImportIssue> issues) {
        List<ImportRow> rows = List.of();
        return ImportBatch.validated(
                ImportBatchId.generate(),
                ImportKind.COURSES,
                PROGRAM_ID,
                PLAN_ID,
                null,
                validRows,
                totalRows,
                issues,
                rows,
                Instant.now());
    }

    private static ImportBatch invalidBatch(List<ImportIssue> issues) {
        return ImportBatch.invalid(
                ImportBatchId.generate(),
                ImportKind.COURSES,
                PROGRAM_ID,
                PLAN_ID,
                null,
                0,
                5,
                issues,
                Instant.now());
    }
}
