package co.edu.uniquindio.brujula.imports.domain.model;

import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotConfirmableException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root: represents a single bulk import request and its outcome.
 *
 * <p>An ImportBatch is created through one of the static factory methods ({@link #validated} or
 * {@link #invalid}) and transitions from VALIDATED → CONFIRMED via {@link #markConfirmed()}.
 * INVALID batches are terminal — they cannot be confirmed (INV-02).
 *
 * <p>The {@code validatedRows} payload stores the parsed row data as a list of {@link ImportRow}
 * records so that the confirm operation can replay them without re-parsing the original file.
 * JSONB serialization is handled in the persistence adapter layer, not here.
 *
 * <p>Invariants enforced:
 * <ul>
 *   <li>INV-01 — A CONFIRMED batch cannot be re-confirmed.
 *   <li>INV-02 — An INVALID batch cannot be confirmed.
 *   <li>validRows ≤ totalRows (structural invariant on construction).
 * </ul>
 *
 * <p>Pure domain class — zero framework annotations (ARQ-002).
 */
public class ImportBatch {

    /**
     * Current schema version of the JSONB payload ({@code validatedRows}).
     *
     * <p>Increment this constant whenever the {@link ImportRow} / {@link ImportIssue} structures
     * change in a breaking way. The persistence adapter compares this value against the
     * {@code schema_version} column at load time and rejects mismatches (NFR-006).
     */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final ImportBatchId id;
    private final ImportKind kind;
    private final UUID programId;
    private final UUID planId;
    private final UUID periodId;
    private ImportStatus status;
    private final int totalRows;
    private final int validRows;
    private final List<ImportIssue> issues;
    private final List<ImportRow> validatedRows;
    private final int schemaVersion;
    private final long version;
    private final Instant createdAt;

    private ImportBatch(
            ImportBatchId id,
            ImportKind kind,
            UUID programId,
            UUID planId,
            UUID periodId,
            ImportStatus status,
            int totalRows,
            int validRows,
            List<ImportIssue> issues,
            List<ImportRow> validatedRows,
            int schemaVersion,
            long version,
            Instant createdAt) {
        this.id = id;
        this.kind = kind;
        this.programId = programId;
        this.planId = planId;
        this.periodId = periodId;
        this.status = status;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.issues = Collections.unmodifiableList(issues);
        this.validatedRows = Collections.unmodifiableList(validatedRows);
        this.schemaVersion = schemaVersion;
        this.version = version;
        this.createdAt = createdAt;
    }

    /**
     * Factory: creates a VALIDATED batch. The batch has at least one valid row and is eligible
     * for confirmation.
     *
     * @throws IllegalArgumentException if validRows > totalRows
     */
    public static ImportBatch validated(
            ImportBatchId id,
            ImportKind kind,
            UUID programId,
            UUID planId,
            UUID periodId,
            int validRows,
            int totalRows,
            List<ImportIssue> issues,
            List<ImportRow> validatedRows,
            Instant createdAt) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (validRows > totalRows) {
            throw new IllegalArgumentException(
                    "validRows (%d) must not exceed totalRows (%d)".formatted(validRows, totalRows));
        }
        List<ImportIssue> safeIssues = issues != null ? issues : List.of();
        List<ImportRow> safeRows = validatedRows != null ? validatedRows : List.of();
        return new ImportBatch(
                id, kind, programId, planId, periodId,
                ImportStatus.VALIDATED,
                totalRows, validRows,
                safeIssues, safeRows,
                CURRENT_SCHEMA_VERSION, 0L, createdAt);
    }

    /**
     * Factory: creates an INVALID batch. At least one blocking error exists; the batch is not
     * confirmable.
     *
     * @throws IllegalArgumentException if issues is null or empty
     */
    public static ImportBatch invalid(
            ImportBatchId id,
            ImportKind kind,
            UUID programId,
            UUID planId,
            UUID periodId,
            int validRows,
            int totalRows,
            List<ImportIssue> issues,
            Instant createdAt) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (issues == null || issues.isEmpty()) {
            throw new IllegalArgumentException("An INVALID batch must have at least one issue");
        }
        return new ImportBatch(
                id, kind, programId, planId, periodId,
                ImportStatus.INVALID,
                totalRows, validRows,
                issues, List.of(),
                CURRENT_SCHEMA_VERSION, 0L, createdAt);
    }

    /**
     * Reconstitution factory: rebuilds an ImportBatch from persistence without applying
     * business logic. Used exclusively by the persistence mapper.
     */
    public static ImportBatch reconstitute(
            ImportBatchId id,
            ImportKind kind,
            UUID programId,
            UUID planId,
            UUID periodId,
            ImportStatus status,
            int totalRows,
            int validRows,
            List<ImportIssue> issues,
            List<ImportRow> validatedRows,
            int schemaVersion,
            long version,
            Instant createdAt) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        List<ImportIssue> safeIssues = issues != null ? issues : List.of();
        List<ImportRow> safeRows = validatedRows != null ? validatedRows : List.of();
        return new ImportBatch(
                id, kind, programId, planId, periodId,
                status,
                totalRows, validRows,
                safeIssues, safeRows,
                schemaVersion, version, createdAt);
    }

    /**
     * Transitions this batch from VALIDATED to CONFIRMED.
     *
     * <p>Enforces:
     * <ul>
     *   <li>INV-01 — throws if already CONFIRMED
     *   <li>INV-02 — throws if INVALID
     * </ul>
     *
     * @throws ImportBatchNotConfirmableException if the batch is not in VALIDATED status
     */
    public void markConfirmed() {
        if (!status.isConfirmable()) {
            throw new ImportBatchNotConfirmableException(id, status);
        }
        this.status = ImportStatus.CONFIRMED;
    }

    // ---- Accessors ----

    public ImportBatchId getId() { return id; }

    public ImportKind getKind() { return kind; }

    public UUID getProgramId() { return programId; }

    public UUID getPlanId() { return planId; }

    public UUID getPeriodId() { return periodId; }

    public ImportStatus getStatus() { return status; }

    public int getTotalRows() { return totalRows; }

    public int getValidRows() { return validRows; }

    public List<ImportIssue> getIssues() { return issues; }

    public List<ImportRow> getValidatedRows() { return validatedRows; }

    public int getSchemaVersion() { return schemaVersion; }

    public long getVersion() { return version; }

    public Instant getCreatedAt() { return createdAt; }
}
