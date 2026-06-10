package co.edu.uniquindio.brujula.imports.domain.model;

import java.util.Objects;

/**
 * Value object: a single validation issue discovered during import parsing or validation.
 *
 * <p>Row 0 represents a global (file-level) issue. A null {@code column} means the issue is not
 * tied to a specific column. {@link Severity#ERROR} issues are blocking (the row is excluded from
 * {@code validRows}); {@link Severity#WARNING} issues are non-blocking.
 *
 * <p>Pure domain record — zero framework annotations (ARQ-002).
 */
public record ImportIssue(int row, String column, String code, String message, Severity severity) {

  public ImportIssue {
    Objects.requireNonNull(code, "code must not be null");
    Objects.requireNonNull(message, "message must not be null");
    Objects.requireNonNull(severity, "severity must not be null");
    if (row < 0) {
      throw new IllegalArgumentException("row must be >= 0 (0 = global)");
    }
  }

  /** Factory: creates a blocking (ERROR) issue. */
  public static ImportIssue error(int row, String column, String code, String message) {
    return new ImportIssue(row, column, code, message, Severity.ERROR);
  }

  /** Factory: creates a non-blocking (WARNING) issue. */
  public static ImportIssue warning(int row, String column, String code, String message) {
    return new ImportIssue(row, column, code, message, Severity.WARNING);
  }

  /** Returns true if this issue blocks the affected row from being counted as valid. */
  public boolean isBlocking() {
    return severity == Severity.ERROR;
  }
}
