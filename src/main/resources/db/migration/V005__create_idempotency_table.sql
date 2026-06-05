-- V005: Create platform idempotency table
--
-- Module: sharedkernel (platform schema)
-- Schema: platform
-- Satisfies: Spec §Idempotent Create (ADR-009, TRD §13), Design §3 V005

CREATE TABLE IF NOT EXISTS platform.idempotency_record (
    idempotency_key UUID        NOT NULL,
    operation       TEXT        NOT NULL,
    request_hash    TEXT        NOT NULL,
    response_status INTEGER     NOT NULL,
    response_body   JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT idempotency_record_pk PRIMARY KEY (idempotency_key, operation)
);

COMMENT ON TABLE  platform.idempotency_record IS 'Stores idempotency key state for create operations (TRD §13). TTL-bounded by application-level cleanup.';
COMMENT ON COLUMN platform.idempotency_record.idempotency_key IS 'Client-supplied UUID header Idempotency-Key.';
COMMENT ON COLUMN platform.idempotency_record.operation IS 'Operation identifier, e.g. CREATE_STAFF. Differentiates keys across endpoints.';
COMMENT ON COLUMN platform.idempotency_record.request_hash IS 'SHA-256 hash of the request body. Conflict if same key+op maps to different hash.';
COMMENT ON COLUMN platform.idempotency_record.response_status IS 'HTTP status code of the original response.';
COMMENT ON COLUMN platform.idempotency_record.response_body IS 'JSON response body of the original response for replay.';
