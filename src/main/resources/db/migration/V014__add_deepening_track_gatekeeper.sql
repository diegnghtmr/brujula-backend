-- V014: Add gatekeeper_course_id to sequential_track and seed per-line gatekeepers for ISC.
--
-- Context: The 3 ISC DEEPENING tracks (Ing. Software+HCI, Infra T.I., Gestión Información)
-- each require a different prerequisite course before a student can enrol in Profundización I/II.
-- The per-line gatekeeper cannot be modeled as a universal course_requirement edge on the shared
-- Profundización I/II slot courses (which would incorrectly force IS I on all 3 lines).
-- Solution: add a nullable gatekeeper_course_id FK on sequential_track (ADR-F2-003, CUR-005).
--
-- Source of truth for gatekeepers:
--   Ingeniería del Software y HCI  → Ingeniería de Software I         (PDF oficial 2026-2)
--   Infraestructura de T.I.         → Administración de Infraestructura de TI (PDF oficial 2026-2)
--   Gestión de la Información       → Bases de Datos I                 (PDF oficial 2026-2)
--
-- Satisfies: C1 + W3 from WU3-batch-3b verify report (#1611); ADR-F2-003; CUR-005.
-- Fase 2 scope: data modeling only — no runtime enforcement (non-goal per design).

-- ============================================================
-- 1. Add gatekeeper_course_id column (nullable, FK to curriculum.course)
-- ============================================================

ALTER TABLE curriculum.sequential_track
    ADD COLUMN IF NOT EXISTS gatekeeper_course_id UUID NULL
        CONSTRAINT sequential_track_gatekeeper_fk
            REFERENCES curriculum.course(id) ON DELETE RESTRICT;

COMMENT ON COLUMN curriculum.sequential_track.gatekeeper_course_id IS
    'Optional gatekeeper: course that must be passed before the student may enrol in level-1 of this track. '
    'NULL for PERSONAL_ELECTIVE tracks and any DEEPENING track without a defined prereq. '
    'For ISC DEEPENING lines this is the PDF-official line-specific prerequisite.';

-- ============================================================
-- 2. Seed per-line gatekeeper for the 3 ISC DEEPENING tracks
-- ============================================================

-- Línea Ingeniería del Software y HCI — gatekeeper: Ingeniería de Software I
-- course id: c0050000-0000-4000-a000-000000000005 (ISC-S5-INGENIERIADESOFTWAREI, V012)
UPDATE curriculum.sequential_track
SET    gatekeeper_course_id = 'c0050000-0000-4000-a000-000000000005'
WHERE  id = 'e0a20000-0000-4000-a000-000000000001';

-- Línea Infraestructura de T.I. — gatekeeper: Administración de Infraestructura de TI
-- course id: c0070000-0000-4000-a000-000000000006 (ISC-S7-ADMINISTRACIONDEINFRAESTRUCTURADETI, V012)
UPDATE curriculum.sequential_track
SET    gatekeeper_course_id = 'c0070000-0000-4000-a000-000000000006'
WHERE  id = 'e0a30000-0000-4000-a000-000000000001';

-- Línea Gestión de la Información y el Conocimiento — gatekeeper: Bases de Datos I
-- course id: c0060000-0000-4000-a000-000000000003 (ISC-S6-BASESDEDATOSI, V012)
UPDATE curriculum.sequential_track
SET    gatekeeper_course_id = 'c0060000-0000-4000-a000-000000000003'
WHERE  id = 'e0a40000-0000-4000-a000-000000000001';
