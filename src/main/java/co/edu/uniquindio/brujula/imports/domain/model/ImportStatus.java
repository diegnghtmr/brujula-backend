package co.edu.uniquindio.brujula.imports.domain.model;

/**
 * Lifecycle status of an ImportBatch.
 *
 * <p>Allowed transitions: VALIDATED → CONFIRMED. INVALID is terminal.
 *
 * <p>Pure domain enum — zero framework annotations (ARQ-002).
 */
public enum ImportStatus {

    /**
     * Batch has been parsed and all rows are structurally valid; at least one row passed
     * business-rule validation. The batch is eligible for confirmation (INV-01, INV-02).
     */
    VALIDATED,

    /**
     * Batch has been parsed but one or more rows have blocking errors. The batch is NOT
     * eligible for confirmation (INV-02).
     */
    INVALID,

    /**
     * Batch has been successfully confirmed — all valid rows have been committed to the
     * relevant module(s). Terminal state (INV-01).
     */
    CONFIRMED;

    /** Returns true if this batch is eligible for the confirm operation. */
    public boolean isConfirmable() {
        return this == VALIDATED;
    }
}
