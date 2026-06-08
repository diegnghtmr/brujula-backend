-- V016__create_offering_tables.sql
-- Forward-only migration. NEVER edit this file after it is committed.
-- Creates offering module tables. Schema 'offering' was created in V001__create_schemas.sql.
-- Design: ADR-F4-003. Cross-schema opaque UUIDs, NO cross-schema FK (ADR-F3-009).

-- ---------------------------------------------------------------------------
-- offering.academic_period
-- ---------------------------------------------------------------------------
CREATE TABLE offering.academic_period (
    id          UUID    NOT NULL DEFAULT gen_random_uuid(),
    name        TEXT    NOT NULL,
    start_date  DATE,
    end_date    DATE,
    status      TEXT    NOT NULL
                        CONSTRAINT academic_period_status_chk
                        CHECK (status IN ('UPCOMING', 'ACTIVE', 'CLOSED', 'ARCHIVED')),
    version     BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT academic_period_pk PRIMARY KEY (id),
    CONSTRAINT academic_period_name_uq UNIQUE (name)
);

-- Index for listing non-archived periods ordered by start_date
CREATE INDEX idx_academic_period_status
    ON offering.academic_period (status);

-- ---------------------------------------------------------------------------
-- offering.course_offering
-- ---------------------------------------------------------------------------
CREATE TABLE offering.course_offering (
    id          UUID    NOT NULL DEFAULT gen_random_uuid(),
    course_id   UUID    NOT NULL,   -- opaque ref to curriculum.course (no FK, ADR-F3-009)
    course_name TEXT    NOT NULL,   -- denormalized snapshot; avoids runtime curriculum coupling
    program_id  UUID    NOT NULL,   -- opaque ref to catalog.program (no FK)
    period_id   UUID    NOT NULL,   -- FK to academic_period (intra-schema)
    status      TEXT    NOT NULL
                        CONSTRAINT course_offering_status_chk
                        CHECK (status IN ('DRAFT', 'PRELIMINARY', 'FINAL', 'ARCHIVED')),
    notes       TEXT,
    version     BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT course_offering_pk PRIMARY KEY (id),

    CONSTRAINT offering_period_fk
        FOREIGN KEY (period_id)
        REFERENCES offering.academic_period (id)
        ON DELETE RESTRICT
);

-- Index for student catalogue queries (period + published status filter)
CREATE INDEX idx_offering_period_status
    ON offering.course_offering (period_id, status);

-- Index for program-based filtering within a period
CREATE INDEX idx_offering_program_period
    ON offering.course_offering (program_id, period_id);

-- ---------------------------------------------------------------------------
-- offering.schedule_option
-- ---------------------------------------------------------------------------
CREATE TABLE offering.schedule_option (
    id           UUID    NOT NULL DEFAULT gen_random_uuid(),
    offering_id  UUID    NOT NULL,
    label        TEXT    NOT NULL,
    journey_code TEXT    NOT NULL,
    version      BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT schedule_option_pk PRIMARY KEY (id),

    CONSTRAINT option_offering_fk
        FOREIGN KEY (offering_id)
        REFERENCES offering.course_offering (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_schedule_option_offering
    ON offering.schedule_option (offering_id);

-- ---------------------------------------------------------------------------
-- offering.schedule_session
-- ---------------------------------------------------------------------------
CREATE TABLE offering.schedule_session (
    id                 UUID    NOT NULL DEFAULT gen_random_uuid(),
    schedule_option_id UUID    NOT NULL,
    day                TEXT    NOT NULL
                               CONSTRAINT schedule_session_day_chk
                               CHECK (day IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY',
                                              'FRIDAY', 'SATURDAY', 'SUNDAY')),
    starts_at          TIME    NOT NULL,
    ends_at            TIME    NOT NULL,

    CONSTRAINT schedule_session_pk PRIMARY KEY (id),
    CONSTRAINT session_ends_after_starts_chk CHECK (ends_at > starts_at),

    CONSTRAINT session_option_fk
        FOREIGN KEY (schedule_option_id)
        REFERENCES offering.schedule_option (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_schedule_session_option
    ON offering.schedule_session (schedule_option_id);
