package co.edu.uniquindio.brujula.imports.domain;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.Severity;

import org.junit.jupiter.api.Test;

/**
 * TDD T1.1 — ImportIssue value object tests.
 *
 * <p>Covers IMP-002: row=0 is global, column is nullable, severity distinction.
 */
class ImportIssueTest {

    /**
     * IMP-002: A row=0 issue is a global-level issue. It is accepted without error.
     */
    @Test
    void issue_with_row_zero_is_global() {
        ImportIssue issue = ImportIssue.error(0, null, "EMPTY_FILE", "No data rows found");

        assertThat(issue.row()).isEqualTo(0);
        assertThat(issue.column()).isNull();
        assertThat(issue.code()).isEqualTo("EMPTY_FILE");
        assertThat(issue.severity()).isEqualTo(Severity.ERROR);
    }

    /**
     * IMP-002: Severity.ERROR issues are blocking; Severity.WARNING issues are non-blocking.
     */
    @Test
    void blocking_issue_contributes_to_errors() {
        ImportIssue errorIssue = ImportIssue.error(1, "course_name", "COURSE_NAME_MISSING", "Name is blank");
        ImportIssue warnIssue = ImportIssue.warning(2, "official_code", "COURSE_ALREADY_EXISTS", "Already in plan");

        assertThat(errorIssue.severity()).isEqualTo(Severity.ERROR);
        assertThat(errorIssue.isBlocking()).isTrue();

        assertThat(warnIssue.severity()).isEqualTo(Severity.WARNING);
        assertThat(warnIssue.isBlocking()).isFalse();
    }

    /**
     * An issue with a data row (row > 0) has a valid row reference.
     */
    @Test
    void issue_with_positive_row_references_data_row() {
        ImportIssue issue = ImportIssue.error(3, "credits", "INVALID_CREDITS", "Credits must be positive");

        assertThat(issue.row()).isEqualTo(3);
        assertThat(issue.column()).isEqualTo("credits");
    }
}
