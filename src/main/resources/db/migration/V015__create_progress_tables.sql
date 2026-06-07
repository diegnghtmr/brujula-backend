-- V015__create_progress_tables.sql
-- Forward-only migration. NEVER edit this file after it is committed.
-- Creates BOTH progress tables so WU2 (subscription) and WU3 (course attempt) share one migration.
-- Design: ADR-F3-010. Contract alignment: NO is_primary column (observation #1630).

-- Schema 'progress' was created in V001__create_schemas.sql

-- ---------------------------------------------------------------------------
-- progress.student_program_subscription
-- ---------------------------------------------------------------------------
CREATE TABLE progress.student_program_subscription (
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id                 UUID         NOT NULL,
    program_id              UUID         NOT NULL,
    plan_id                 UUID         NOT NULL,
    reference_journey_code  TEXT         NOT NULL,
    status                  TEXT         NOT NULL DEFAULT 'ACTIVE'
                                         CONSTRAINT subscription_status_chk
                                         CHECK (status IN ('ACTIVE', 'DELETED')),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                 BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT student_program_subscription_pk PRIMARY KEY (id)
);

-- Composite index for listing active subscriptions per student (SUB-2-LIST, cap guard)
CREATE INDEX idx_subscription_user_status
    ON progress.student_program_subscription (user_id, status);

-- ---------------------------------------------------------------------------
-- progress.course_attempt
-- ---------------------------------------------------------------------------
CREATE TABLE progress.course_attempt (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    subscription_id UUID         NOT NULL,
    course_id       UUID         NOT NULL,
    status          TEXT         NOT NULL,
    final_grade     NUMERIC(3,1),          -- nullable: CANCELLED has no grade
    period_id       UUID,                   -- opaque, nullable (ADR-F3-009): no FK
    journey_code    TEXT         NOT NULL,
    registered_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT course_attempt_pk PRIMARY KEY (id),

    -- Intra-schema FK: subscription must exist; ON DELETE RESTRICT (soft-delete, never hard-delete)
    CONSTRAINT attempt_subscription_fk
        FOREIGN KEY (subscription_id)
        REFERENCES progress.student_program_subscription (id)
        ON DELETE RESTRICT,

    -- Grade/status coherence:
    --   PASSED  requires grade in [3.0, 5.0]
    --   FAILED  requires grade in [0.0, 2.9]
    --   CANCELLED requires grade IS NULL
    CONSTRAINT attempt_grade_status_chk CHECK (
        (status = 'PASSED'    AND final_grade IS NOT NULL AND final_grade >= 3.0 AND final_grade <= 5.0)
     OR (status = 'FAILED'    AND final_grade IS NOT NULL AND final_grade >= 0.0 AND final_grade <  3.0)
     OR (status = 'CANCELLED' AND final_grade IS NULL)
    )
);

-- Composite index for bulk attempt loading and effective-status grouping (ADR-F3-010)
CREATE INDEX idx_attempt_sub_course_status
    ON progress.course_attempt (subscription_id, course_id, status);
