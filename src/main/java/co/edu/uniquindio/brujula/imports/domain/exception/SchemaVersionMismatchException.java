package co.edu.uniquindio.brujula.imports.domain.exception;

import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;

/**
 * Thrown when an ImportBatch is loaded from persistence and its stored {@code schema_version}
 * does not match the current application's expected version (NFR-006).
 *
 * <p>This guards against attempting to confirm a batch whose JSONB payload was written under
 * an older (incompatible) schema, which would cause silent data corruption.
 *
 * <p>Error code: {@code SCHEMA_VERSION_MISMATCH}
 *
 * <p>Pure domain exception — zero framework annotations (ARQ-002).
 */
public class SchemaVersionMismatchException extends RuntimeException {

    public static final String ERROR_CODE = "SCHEMA_VERSION_MISMATCH";

    private final ImportBatchId batchId;
    private final int storedVersion;
    private final int expectedVersion;

    public SchemaVersionMismatchException(
            ImportBatchId batchId, int storedVersion, int expectedVersion) {
        super("[%s] ImportBatch %s: stored schema_version=%d but application expects %d"
                .formatted(ERROR_CODE, batchId.value(), storedVersion, expectedVersion));
        this.batchId = batchId;
        this.storedVersion = storedVersion;
        this.expectedVersion = expectedVersion;
    }

    public ImportBatchId getBatchId() { return batchId; }

    public int getStoredVersion() { return storedVersion; }

    public int getExpectedVersion() { return expectedVersion; }
}
