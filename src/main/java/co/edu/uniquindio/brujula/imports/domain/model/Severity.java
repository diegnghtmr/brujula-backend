package co.edu.uniquindio.brujula.imports.domain.model;

/**
 * Severity level for an ImportIssue.
 *
 * <p>ERROR issues are blocking — the affected row does not count toward {@code validRows}. WARNING
 * issues are non-blocking — the row still counts toward {@code validRows}.
 *
 * <p>Pure domain enum — zero framework annotations.
 */
public enum Severity {

  /** Blocking issue: the row is excluded from the validated set and cannot be committed. */
  ERROR,

  /** Non-blocking issue: the row is still valid and can be committed, but has a caveat. */
  WARNING
}
