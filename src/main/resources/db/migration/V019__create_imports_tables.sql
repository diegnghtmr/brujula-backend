-- V019__create_imports_tables.sql
-- Forward-only migration. NEVER edit this file after it is committed.
-- Creates imports module tables. Schema 'imports' was created in V001__create_schemas.sql.
-- Design: ADR-F6-002 (JSONB via @JdbcTypeCode), ADR-F6-006 (no cross-schema FK, UUID refs).
-- INV-01: CONFIRMED batch cannot be re-confirmed (enforced at domain layer).
-- INV-02: INVALID batch cannot be confirmed (enforced at domain layer).

-- ---------------------------------------------------------------------------
-- imports.import_batch
-- ---------------------------------------------------------------------------
CREATE TABLE imports.import_batch (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    kind                VARCHAR(20) NOT NULL
                                    CONSTRAINT import_batch_kind_chk
                                    CHECK (kind IN ('COURSES', 'REQUIREMENTS', 'OFFERINGS')),
    program_id          UUID        NOT NULL,
    -- planId: required for COURSES/REQUIREMENTS; NULL for OFFERINGS
    plan_id             UUID,
    -- periodId: required for OFFERINGS; NULL for COURSES/REQUIREMENTS
    period_id           UUID,
    status              VARCHAR(20) NOT NULL
                                    CONSTRAINT import_batch_status_chk
                                    CHECK (status IN ('VALIDATED', 'INVALID', 'CONFIRMED')),
    total_rows          INT         NOT NULL,
    valid_rows          INT         NOT NULL,
    -- JSONB payload: serialized List<ImportRow> for confirm replay (ADR-F6-002)
    validated_rows_json JSONB       NOT NULL DEFAULT '[]',
    -- JSONB payload: serialized List<ImportIssue>
    issues_json         JSONB       NOT NULL DEFAULT '[]',
    -- Schema version guard: must match ImportBatch.CURRENT_SCHEMA_VERSION at confirm time (NFR-006)
    schema_version      INT         NOT NULL DEFAULT 1,
    -- Idempotency support (populated by UploadImportService / WU3)
    idempotency_key     UUID,
    idempotency_hash    VARCHAR(64),
    -- Optimistic locking (@Version — must match entity version field) (ADR-010)
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT import_batch_pk PRIMARY KEY (id),
    CONSTRAINT import_batch_idempotency_key_uq UNIQUE (idempotency_key),
    CONSTRAINT import_batch_valid_rows_chk
        CHECK (valid_rows >= 0 AND valid_rows <= total_rows),
    CONSTRAINT import_batch_total_rows_chk
        CHECK (total_rows >= 0)
);

-- Index for loading batches by program (access control scope)
CREATE INDEX idx_import_batch_program_status
    ON imports.import_batch (program_id, status);

-- Index for filtering by kind + status (list imports query)
CREATE INDEX idx_import_batch_kind_status
    ON imports.import_batch (kind, status);
