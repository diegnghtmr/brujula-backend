-- V011: Create curriculum module tables (curriculum schema)
--
-- Module: curriculum
-- Schema: curriculum
-- Satisfies: Curriculum module spec
--
-- Tables:
--   curriculum.course                         — course aggregate (per study plan)
--   curriculum.course_recommended_semester    — join table: course × journey × semester
--   curriculum.course_requirement             — prerequisite/requirement edges
--   curriculum.sequential_track               — ordered electiva/profundización track
--   curriculum.sequential_track_level         — one level per track (ordered)
--   curriculum.sequential_track_level_option  — FK-safe child table for option course IDs
--   curriculum.degree_work_modality           — degree work modality per plan

-- -------------------------------------------------------------------------
-- course
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS curriculum.course (
    id                      UUID    NOT NULL DEFAULT gen_random_uuid(),
    plan_id                 UUID    NOT NULL,
    program_id              UUID    NOT NULL,
    official_code           TEXT,
    name                    TEXT    NOT NULL,
    type                    TEXT    NOT NULL,
    credits                 INT     NOT NULL DEFAULT 0,
    curricular_weekly_hours INT     NOT NULL DEFAULT 0,
    status                  TEXT    NOT NULL DEFAULT 'ACTIVE',
    version                 BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT course_pk              PRIMARY KEY (id),
    CONSTRAINT course_type_chk        CHECK (type IN (
                                        'REQUIRED', 'PERSONAL_ELECTIVE', 'DEEPENING',
                                        'DEGREE_WORK', 'INSTITUTIONAL', 'OPTIONAL')),
    CONSTRAINT course_status_chk      CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT course_credits_chk     CHECK (credits >= 0),
    CONSTRAINT course_hours_chk       CHECK (curricular_weekly_hours >= 0),
    CONSTRAINT course_official_code_uq UNIQUE (program_id, official_code)
);

COMMENT ON TABLE  curriculum.course IS 'Curricular course (space académico) belonging to a study plan.';
COMMENT ON COLUMN curriculum.course.plan_id IS 'FK reference to catalog.study_plan (no cross-schema FK).';
COMMENT ON COLUMN curriculum.course.program_id IS 'FK reference to catalog.academic_program (no cross-schema FK).';
COMMENT ON COLUMN curriculum.course.official_code IS 'Optional minted internal code (format PROG-Ssem-SLUG). Unique per program when set.';
COMMENT ON COLUMN curriculum.course.type IS 'Course type: REQUIRED | PERSONAL_ELECTIVE | DEEPENING | DEGREE_WORK | INSTITUTIONAL | OPTIONAL.';
COMMENT ON COLUMN curriculum.course.status IS 'Course lifecycle: ACTIVE | INACTIVE | ARCHIVED.';
COMMENT ON COLUMN curriculum.course.version IS 'Optimistic locking version counter.';

-- Malla assembly index: drives the primary malla group-by query (plan + status filter)
CREATE INDEX IF NOT EXISTS course_plan_status_idx
    ON curriculum.course (plan_id, status);

-- -------------------------------------------------------------------------
-- course_recommended_semester
-- -------------------------------------------------------------------------
-- PK is (course_id, journey_code): one row per course per journey defines
-- the recommended semester for that journey (diurna vs nocturna differ).
CREATE TABLE IF NOT EXISTS curriculum.course_recommended_semester (
    course_id    UUID NOT NULL,
    journey_code TEXT NOT NULL,
    semester     INT  NOT NULL,

    CONSTRAINT course_recommended_semester_pk         PRIMARY KEY (course_id, journey_code),
    CONSTRAINT course_recommended_semester_course_fk  FOREIGN KEY (course_id)
        REFERENCES curriculum.course (id) ON DELETE CASCADE,
    CONSTRAINT course_recommended_semester_chk        CHECK (semester >= 1)
);

COMMENT ON TABLE  curriculum.course_recommended_semester IS 'Per-journey recommended semester for a course. Drives malla assembly.';
COMMENT ON COLUMN curriculum.course_recommended_semester.journey_code IS 'Journey code reference (e.g. DIURNA, NOCTURNA). Not a FK — journey is in catalog schema.';
COMMENT ON COLUMN curriculum.course_recommended_semester.semester IS 'Recommended semester number (>= 1) for this course in the given journey.';

-- Malla assembly index: supports GROUP BY journey_code, semester
CREATE INDEX IF NOT EXISTS course_recommended_semester_journey_sem_idx
    ON curriculum.course_recommended_semester (journey_code, semester);

-- -------------------------------------------------------------------------
-- course_requirement
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS curriculum.course_requirement (
    id                   UUID    NOT NULL DEFAULT gen_random_uuid(),
    target_course_id     UUID    NOT NULL,
    type                 TEXT    NOT NULL,
    required_course_id   UUID,
    required_credits     INT,
    simultaneity_allowed BOOLEAN,
    confidence           TEXT,
    inferred             BOOLEAN,
    status               TEXT    NOT NULL DEFAULT 'ACTIVE',
    version              BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT course_requirement_pk              PRIMARY KEY (id),
    CONSTRAINT course_requirement_target_fk       FOREIGN KEY (target_course_id)
        REFERENCES curriculum.course (id) ON DELETE CASCADE,
    CONSTRAINT course_requirement_required_fk     FOREIGN KEY (required_course_id)
        REFERENCES curriculum.course (id) ON DELETE RESTRICT,
    CONSTRAINT course_requirement_type_chk        CHECK (type IN (
                                                    'COURSE_PASSED', 'SEQUENCE',
                                                    'MINIMUM_CREDITS', 'CONFIGURABLE')),
    CONSTRAINT course_requirement_type_coherence_chk CHECK (
                                                    type NOT IN ('COURSE_PASSED', 'SEQUENCE')
                                                    OR required_course_id IS NOT NULL),
    CONSTRAINT course_requirement_confidence_chk  CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW') OR confidence IS NULL),
    CONSTRAINT course_requirement_status_chk      CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT course_requirement_credits_chk     CHECK (required_credits IS NULL OR required_credits >= 0)
);

COMMENT ON TABLE  curriculum.course_requirement IS 'Prerequisite/requirement edge in the curricular graph. COURSE_PASSED/SEQUENCE edges form a DAG.';
COMMENT ON COLUMN curriculum.course_requirement.target_course_id IS 'Course that has this requirement (the one the student wants to take).';
COMMENT ON COLUMN curriculum.course_requirement.type IS 'Requirement type: COURSE_PASSED | SEQUENCE | MINIMUM_CREDITS | CONFIGURABLE.';
COMMENT ON COLUMN curriculum.course_requirement.required_course_id IS 'The prerequisite course (for COURSE_PASSED/SEQUENCE types). Nullable.';
COMMENT ON COLUMN curriculum.course_requirement.confidence IS 'Inference confidence for this edge: HIGH | MEDIUM | LOW. Null for official edges.';
COMMENT ON COLUMN curriculum.course_requirement.inferred IS 'True for inferred (non-official) edges from best-effort analysis.';
COMMENT ON COLUMN curriculum.course_requirement.version IS 'Optimistic locking version counter.';

-- Prerequisite graph lookup index: retrieve all requirements for a given target course
CREATE INDEX IF NOT EXISTS course_requirement_target_idx
    ON curriculum.course_requirement (target_course_id);

-- -------------------------------------------------------------------------
-- sequential_track
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS curriculum.sequential_track (
    id                              UUID    NOT NULL DEFAULT gen_random_uuid(),
    plan_id                         UUID    NOT NULL,
    kind                            TEXT    NOT NULL,
    label                           TEXT    NOT NULL,
    consecutive_simultaneity_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    version                         BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT sequential_track_pk      PRIMARY KEY (id),
    CONSTRAINT sequential_track_kind_chk CHECK (kind IN ('PERSONAL_ELECTIVE', 'DEEPENING'))
);

COMMENT ON TABLE  curriculum.sequential_track IS 'Ordered electiva/profundización sequential track for a study plan.';
COMMENT ON COLUMN curriculum.sequential_track.plan_id IS 'FK reference to catalog.study_plan (no cross-schema FK).';
COMMENT ON COLUMN curriculum.sequential_track.kind IS 'Track kind: PERSONAL_ELECTIVE | DEEPENING.';
COMMENT ON COLUMN curriculum.sequential_track.consecutive_simultaneity_allowed IS 'Whether consecutive levels can be taken simultaneously (configurable per plan).';
COMMENT ON COLUMN curriculum.sequential_track.version IS 'Optimistic locking version counter.';

-- -------------------------------------------------------------------------
-- sequential_track_level
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS curriculum.sequential_track_level (
    id        UUID NOT NULL DEFAULT gen_random_uuid(),
    track_id  UUID NOT NULL,
    level     INT  NOT NULL,

    CONSTRAINT sequential_track_level_pk          PRIMARY KEY (id),
    CONSTRAINT sequential_track_level_track_fk    FOREIGN KEY (track_id)
        REFERENCES curriculum.sequential_track (id) ON DELETE CASCADE,
    CONSTRAINT sequential_track_level_uq          UNIQUE (track_id, level),
    CONSTRAINT sequential_track_level_level_chk   CHECK (level >= 1)
);

COMMENT ON TABLE  curriculum.sequential_track_level IS 'One ordered level within a sequential track (e.g. Electiva I = 1, Electiva II = 2).';
COMMENT ON COLUMN curriculum.sequential_track_level.level IS 'Level sequence number (>= 1). Must be unique within a track.';

-- -------------------------------------------------------------------------
-- sequential_track_level_option
-- -------------------------------------------------------------------------
-- FK-safe child table for option course IDs (one row per course-level pair).
CREATE TABLE IF NOT EXISTS curriculum.sequential_track_level_option (
    level_id  UUID NOT NULL,
    course_id UUID NOT NULL,

    CONSTRAINT sequential_track_level_option_pk       PRIMARY KEY (level_id, course_id),
    CONSTRAINT sequential_track_level_option_level_fk FOREIGN KEY (level_id)
        REFERENCES curriculum.sequential_track_level (id) ON DELETE CASCADE
);

COMMENT ON TABLE  curriculum.sequential_track_level_option IS 'Many-to-many: real course options selectable at a given sequential track level.';
COMMENT ON COLUMN curriculum.sequential_track_level_option.level_id IS 'FK to curriculum.sequential_track_level.';
COMMENT ON COLUMN curriculum.sequential_track_level_option.course_id IS 'FK reference to curriculum.course (no FK enforced — course may be in same schema, but cascade rules simplified).';

-- -------------------------------------------------------------------------
-- degree_work_modality
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS curriculum.degree_work_modality (
    id                      UUID    NOT NULL DEFAULT gen_random_uuid(),
    plan_id                 UUID    NOT NULL,
    name                    TEXT    NOT NULL,
    credits                 INT,
    curricular_weekly_hours INT,
    status                  TEXT    NOT NULL DEFAULT 'ACTIVE',
    version                 BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT degree_work_modality_pk         PRIMARY KEY (id),
    CONSTRAINT degree_work_modality_status_chk CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT degree_work_modality_credits_chk CHECK (credits IS NULL OR credits >= 0),
    CONSTRAINT degree_work_modality_hours_chk   CHECK (curricular_weekly_hours IS NULL OR curricular_weekly_hours >= 0)
);

COMMENT ON TABLE  curriculum.degree_work_modality IS 'Degree work modality option for a study plan (e.g. aplicación, investigación, pasantía).';
COMMENT ON COLUMN curriculum.degree_work_modality.plan_id IS 'FK reference to catalog.study_plan (no cross-schema FK).';
COMMENT ON COLUMN curriculum.degree_work_modality.status IS 'Modality lifecycle: ACTIVE | ARCHIVED.';
COMMENT ON COLUMN curriculum.degree_work_modality.version IS 'Optimistic locking version counter.';
