package co.edu.uniquindio.brujula.imports.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Covers uncovered branches in imports/domain/model classes.
 *
 * <p>ImportRow: negative index guard. ImportBatch.validated: null issues and null validatedRows use
 * safe defaults. ImportBatch.invalid: null issues throws. ImportBatch.reconstitute: null issues and
 * null rows use safe defaults.
 */
class ImportDomainModelTest {

  private static final UUID PROGRAM_ID = UUID.randomUUID();
  private static final UUID PLAN_ID = UUID.randomUUID();

  // -------------------------------------------------------------------------
  // ImportRow — negative index guard
  // -------------------------------------------------------------------------

  @Test
  void importRow_rejects_negative_index() {
    // The guard `if (index < 0)` in the compact constructor must throw
    assertThatThrownBy(() -> new ImportRow(-1, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("index");
  }

  @Test
  void importRow_accepts_zero_index() {
    ImportRow row = new ImportRow(0, Map.of("k", "v"));
    assertThat(row.index()).isEqualTo(0);
  }

  // -------------------------------------------------------------------------
  // ImportBatch.validated — null-safe fallback for issues and validatedRows
  // -------------------------------------------------------------------------

  @Test
  void validated_with_null_issues_uses_empty_list() {
    ImportBatch batch =
        ImportBatch.validated(
            ImportBatchId.generate(),
            ImportKind.COURSES,
            PROGRAM_ID,
            PLAN_ID,
            null,
            0,
            0,
            null, // null issues — must default to empty
            List.of(),
            Instant.now());

    assertThat(batch.getIssues()).isEmpty();
  }

  @Test
  void validated_with_null_validatedRows_uses_empty_list() {
    ImportBatch batch =
        ImportBatch.validated(
            ImportBatchId.generate(),
            ImportKind.COURSES,
            PROGRAM_ID,
            PLAN_ID,
            null,
            0,
            0,
            List.of(),
            null, // null validatedRows — must default to empty
            Instant.now());

    assertThat(batch.getValidatedRows()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // ImportBatch.invalid — null issues guard
  // -------------------------------------------------------------------------

  @Test
  void invalid_with_null_issues_throws_illegal_argument() {
    assertThatThrownBy(
            () ->
                ImportBatch.invalid(
                    ImportBatchId.generate(),
                    ImportKind.COURSES,
                    PROGRAM_ID,
                    PLAN_ID,
                    null,
                    0,
                    1,
                    null, // null issues — INVALID batch must have at least one
                    Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // -------------------------------------------------------------------------
  // ImportBatch.reconstitute — null-safe fallback
  // -------------------------------------------------------------------------

  @Test
  void reconstitute_with_null_issues_uses_empty_list() {
    ImportBatch batch =
        ImportBatch.reconstitute(
            ImportBatchId.generate(),
            ImportKind.COURSES,
            PROGRAM_ID,
            PLAN_ID,
            null,
            ImportStatus.CONFIRMED,
            3,
            3,
            null, // null issues — must default to empty
            List.of(),
            ImportBatch.CURRENT_SCHEMA_VERSION,
            2L,
            Instant.now());

    assertThat(batch.getIssues()).isEmpty();
    assertThat(batch.getStatus()).isEqualTo(ImportStatus.CONFIRMED);
  }

  @Test
  void reconstitute_with_null_validatedRows_uses_empty_list() {
    ImportBatch batch =
        ImportBatch.reconstitute(
            ImportBatchId.generate(),
            ImportKind.REQUIREMENTS,
            PROGRAM_ID,
            PLAN_ID,
            null,
            ImportStatus.VALIDATED,
            2,
            2,
            List.of(),
            null, // null validatedRows — must default to empty
            ImportBatch.CURRENT_SCHEMA_VERSION,
            0L,
            Instant.now());

    assertThat(batch.getValidatedRows()).isEmpty();
  }
}
