-- V010: Create programcatalog module tables (catalog schema)
--
-- Module: programcatalog
-- Schema: catalog
-- Satisfies: CAT-001, CAT-002, CAT-003, Design §5 V010
--
-- Tables:
--   catalog.academic_program  — academic programs (aggregate with optimistic lock)
--   catalog.journey           — journeys/jornadas (DIURNA, NOCTURNA, etc.)
--   catalog.study_plan        — study plans per program (aggregate with optimistic lock)
--   catalog.program_journey   — M:N association between programs and journeys

-- -------------------------------------------------------------------------
-- academic_program
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalog.academic_program (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL,
    faculty     TEXT        NOT NULL,
    status      TEXT        NOT NULL DEFAULT 'ACTIVE',
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT academic_program_pk         PRIMARY KEY (id),
    CONSTRAINT academic_program_status_chk CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

COMMENT ON TABLE  catalog.academic_program IS 'Academic programs offered by the university (e.g. Ingeniería de Sistemas y Computación).';
COMMENT ON COLUMN catalog.academic_program.name IS 'Official program name.';
COMMENT ON COLUMN catalog.academic_program.faculty IS 'Faculty that owns the program.';
COMMENT ON COLUMN catalog.academic_program.status IS 'Program lifecycle status: ACTIVE | INACTIVE | ARCHIVED.';
COMMENT ON COLUMN catalog.academic_program.version IS 'Optimistic locking version counter (DB-006/DT-014).';

-- -------------------------------------------------------------------------
-- journey
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalog.journey (
    id      UUID    NOT NULL DEFAULT gen_random_uuid(),
    code    TEXT    NOT NULL,
    name    TEXT    NOT NULL,
    active  BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT journey_pk      PRIMARY KEY (id),
    CONSTRAINT journey_code_uq UNIQUE (code)
);

COMMENT ON TABLE  catalog.journey IS 'Journey/jornada options available for study plans (e.g. DIURNA, NOCTURNA).';
COMMENT ON COLUMN catalog.journey.code IS 'Short identifier code — unique across all journeys (e.g. DIURNA, NOCTURNA).';
COMMENT ON COLUMN catalog.journey.active IS 'Whether this journey is currently accepting students.';

-- -------------------------------------------------------------------------
-- study_plan
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalog.study_plan (
    id              UUID    NOT NULL DEFAULT gen_random_uuid(),
    program_id      UUID    NOT NULL,
    name            TEXT    NOT NULL,
    status          TEXT    NOT NULL DEFAULT 'DRAFT',
    total_credits   INT     NOT NULL,
    reference_year  INT,
    version         BIGINT  NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT study_plan_pk            PRIMARY KEY (id),
    CONSTRAINT study_plan_program_fk    FOREIGN KEY (program_id)
        REFERENCES catalog.academic_program (id) ON DELETE RESTRICT,
    CONSTRAINT study_plan_status_chk    CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT study_plan_credits_chk   CHECK (total_credits > 0)
);

COMMENT ON TABLE  catalog.study_plan IS 'Versioned study plan belonging to an academic program.';
COMMENT ON COLUMN catalog.study_plan.program_id IS 'FK to catalog.academic_program.';
COMMENT ON COLUMN catalog.study_plan.status IS 'Plan lifecycle: DRAFT | ACTIVE | ARCHIVED.';
COMMENT ON COLUMN catalog.study_plan.total_credits IS 'Total credits required to complete this plan (must be > 0).';
COMMENT ON COLUMN catalog.study_plan.reference_year IS 'Reference year for the plan version (e.g. 2016).';
COMMENT ON COLUMN catalog.study_plan.version IS 'Optimistic locking version counter (DB-006/DT-014).';

-- -------------------------------------------------------------------------
-- program_journey (M:N association)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS catalog.program_journey (
    program_id  UUID NOT NULL,
    journey_id  UUID NOT NULL,

    CONSTRAINT program_journey_pk         PRIMARY KEY (program_id, journey_id),
    CONSTRAINT program_journey_program_fk FOREIGN KEY (program_id)
        REFERENCES catalog.academic_program (id) ON DELETE CASCADE,
    CONSTRAINT program_journey_journey_fk FOREIGN KEY (journey_id)
        REFERENCES catalog.journey (id) ON DELETE CASCADE
);

COMMENT ON TABLE  catalog.program_journey IS 'Many-to-many association between academic programs and journeys.';
COMMENT ON COLUMN catalog.program_journey.program_id IS 'FK to catalog.academic_program.';
COMMENT ON COLUMN catalog.program_journey.journey_id IS 'FK to catalog.journey.';
