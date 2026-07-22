-- V028: Profundización as a pool of distinct interchangeable electives (PR1a)
--
-- Change: PROP-001-profundizacion-elective-pool. Source of truth: design §0 CORRECTION
-- (authoritative, 2026-07-22) + design §3/§5, refining the official source
-- "01 - Propuesta Profundizaciones o electivas 2026-2.pdf".
--
-- Product-owner ruling: Profundización I-IV are NOT four ordered slots — they are a
-- pool of >= 4 DISTINCT interchangeable electives. Each elective keeps ITS LINE's
-- entry prerequisite (interchangeability is across SLOTS only, not across lines).
-- The per-line gatekeeper is therefore RELOCATED from
-- curriculum.sequential_track.gatekeeper_course_id (V014) onto one COURSE_PASSED
-- course_requirement edge per elective — PrerequisiteEvaluator needs no code change,
-- only different data.
--
-- This migration:
--   1. Seeds 8 DEEPENING elective courses (design §3). No course_recommended_semester
--      rows are added for them (design D2) — the malla assembler only renders courses
--      with a semester row for the requested journey, so these 8 stay invisible to the
--      malla grid. The 4 existing slot courses (Profundización I-IV, V012) remain the
--      ACTIVE display anchors and are otherwise untouched by this migration.
--   2. Seeds 8 COURSE_PASSED edges: elective -> its line's gatekeeper course.
--   3. Seeds one CONFIGURABLE course_requirement on Trabajo de Grado: the pool gate
--      (required_course_id = NULL, required_credits = 4, reinterpreted as ">= 4 distinct
--      passed pool members").
--   4. ARCHIVEs (status='ARCHIVED', never deleted) the 4 now-superseded ordering edges
--      seeded at V012:733-746: the 2 slot->slot edges (Prof III <- Prof I, Prof IV <-
--      Prof II) and the 2 thesis-slot edges (Trabajo de Grado <- Prof III, Trabajo de
--      Grado <- Prof IV). Distinctness + the max-4/min-4 pool gate (step 3) replaces the
--      ordinal enforcement under full interchangeability + simultaneity.
--   5. NULLs curriculum.sequential_track.gatekeeper_course_id (V014) for the 3 ISC
--      DEEPENING line tracks — the logic is relocated (not removed) onto step 2's edges.
--
-- Idempotent: forward-only, safe to re-run (WHERE NOT EXISTS / stable-tuple guards).
-- Reversible: DELETE the 8 new courses (cascades their course_requirement rows via
-- course_requirement_target_fk ON DELETE CASCADE) + the CONFIGURABLE row; restore the
-- 4 archived rows to status='ACTIVE'; restore the 3 tracks' gatekeeper_course_id to the
-- V014 values (Ingeniería de Software I / Administración de Infraestructura de TI /
-- Bases de Datos I respectively).

-- ============================================================
-- 1. Eight DEEPENING elective courses (design §3, Sistemas Plan 21)
--    official_code minting: ISC-DEEP-<SLUG> (SLUG = accent-fold(name), strip non-alnum,
--    UPPERCASE — same convention as V012's ISC-S{sem}-{SLUG}, PROG segment reused as
--    DEEP to signal pool membership instead of a fixed semester).
-- ============================================================

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version)
SELECT 'c0dee000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
       'ISC-DEEP-DISENOCENTRADOENELUSUARIO', 'Diseño Centrado en el Usuario', 'DEEPENING', 3, 4, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM curriculum.course WHERE official_code = 'ISC-DEEP-DISENOCENTRADOENELUSUARIO');

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version)
SELECT 'c0dee000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
       'ISC-DEEP-DESARROLLODEAPLICACIONESPARADISPOSITIVOSMOVILES', 'Desarrollo de Aplicaciones para Dispositivos Móviles', 'DEEPENING', 3, 4, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM curriculum.course WHERE official_code = 'ISC-DEEP-DESARROLLODEAPLICACIONESPARADISPOSITIVOSMOVILES');

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version)
SELECT 'c0dee000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
       'ISC-DEEP-USABILIDADYEXPERIENCIADEUSUARIO', 'Usabilidad y Experiencia de Usuario', 'DEEPENING', 3, 4, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM curriculum.course WHERE official_code = 'ISC-DEEP-USABILIDADYEXPERIENCIADEUSUARIO');

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version)
SELECT 'c0dee000-0000-4000-a000-000000000004', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
       'ISC-DEEP-COMPUTACIONENLANUBE', 'Computación en la Nube', 'DEEPENING', 3, 4, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM curriculum.course WHERE official_code = 'ISC-DEEP-COMPUTACIONENLANUBE');

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version)
SELECT 'c0dee000-0000-4000-a000-000000000005', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
       'ISC-DEEP-ARQUITECTURAORIENTADAAMICROSERVICIOS', 'Arquitectura Orientada a Microservicios', 'DEEPENING', 3, 4, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM curriculum.course WHERE official_code = 'ISC-DEEP-ARQUITECTURAORIENTADAAMICROSERVICIOS');

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version)
SELECT 'c0dee000-0000-4000-a000-000000000006', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
       'ISC-DEEP-VISUALIZACIONDEDATOS', 'Visualización de Datos', 'DEEPENING', 3, 4, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM curriculum.course WHERE official_code = 'ISC-DEEP-VISUALIZACIONDEDATOS');

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version)
SELECT 'c0dee000-0000-4000-a000-000000000007', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
       'ISC-DEEP-VISIONARTIFICIAL', 'Visión Artificial', 'DEEPENING', 3, 4, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM curriculum.course WHERE official_code = 'ISC-DEEP-VISIONARTIFICIAL');

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version)
SELECT 'c0dee000-0000-4000-a000-000000000008', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
       'ISC-DEEP-INTRODUCCIONALBIGDATA', 'Introducción al Big Data', 'DEEPENING', 3, 4, 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM curriculum.course WHERE official_code = 'ISC-DEEP-INTRODUCCIONALBIGDATA');

-- ============================================================
-- 2. Per-elective COURSE_PASSED line-gatekeeper edges (design §0 CORRECTION table).
--    Electives keep their LINE's entry prerequisite; interchangeability is across
--    SLOTS only. Gatekeeper course looked up by official_code (V012/robust to id drift).
-- ============================================================

-- Línea Ingeniería del Software y HCI -> Ingeniería de Software I (ISC-S5-INGENIERIADESOFTWAREI)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version)
SELECT gen_random_uuid(), 'c0dee000-0000-4000-a000-000000000001'::uuid, 'COURSE_PASSED', gk.id, 'HIGH', false, 'ACTIVE', 0
FROM curriculum.course gk
WHERE gk.official_code = 'ISC-S5-INGENIERIADESOFTWAREI'
  AND NOT EXISTS (SELECT 1 FROM curriculum.course_requirement
                   WHERE target_course_id = 'c0dee000-0000-4000-a000-000000000001'::uuid
                     AND required_course_id = gk.id AND type = 'COURSE_PASSED');

INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version)
SELECT gen_random_uuid(), 'c0dee000-0000-4000-a000-000000000002'::uuid, 'COURSE_PASSED', gk.id, 'HIGH', false, 'ACTIVE', 0
FROM curriculum.course gk
WHERE gk.official_code = 'ISC-S5-INGENIERIADESOFTWAREI'
  AND NOT EXISTS (SELECT 1 FROM curriculum.course_requirement
                   WHERE target_course_id = 'c0dee000-0000-4000-a000-000000000002'::uuid
                     AND required_course_id = gk.id AND type = 'COURSE_PASSED');

INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version)
SELECT gen_random_uuid(), 'c0dee000-0000-4000-a000-000000000003'::uuid, 'COURSE_PASSED', gk.id, 'HIGH', false, 'ACTIVE', 0
FROM curriculum.course gk
WHERE gk.official_code = 'ISC-S5-INGENIERIADESOFTWAREI'
  AND NOT EXISTS (SELECT 1 FROM curriculum.course_requirement
                   WHERE target_course_id = 'c0dee000-0000-4000-a000-000000000003'::uuid
                     AND required_course_id = gk.id AND type = 'COURSE_PASSED');

-- Línea Infraestructura de T.I. -> Administración de Infraestructura de TI (ISC-S7-ADMINISTRACIONDEINFRAESTRUCTURADETI)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version)
SELECT gen_random_uuid(), 'c0dee000-0000-4000-a000-000000000004'::uuid, 'COURSE_PASSED', gk.id, 'HIGH', false, 'ACTIVE', 0
FROM curriculum.course gk
WHERE gk.official_code = 'ISC-S7-ADMINISTRACIONDEINFRAESTRUCTURADETI'
  AND NOT EXISTS (SELECT 1 FROM curriculum.course_requirement
                   WHERE target_course_id = 'c0dee000-0000-4000-a000-000000000004'::uuid
                     AND required_course_id = gk.id AND type = 'COURSE_PASSED');

INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version)
SELECT gen_random_uuid(), 'c0dee000-0000-4000-a000-000000000005'::uuid, 'COURSE_PASSED', gk.id, 'HIGH', false, 'ACTIVE', 0
FROM curriculum.course gk
WHERE gk.official_code = 'ISC-S7-ADMINISTRACIONDEINFRAESTRUCTURADETI'
  AND NOT EXISTS (SELECT 1 FROM curriculum.course_requirement
                   WHERE target_course_id = 'c0dee000-0000-4000-a000-000000000005'::uuid
                     AND required_course_id = gk.id AND type = 'COURSE_PASSED');

-- Línea Gestión de la Información y el Conocimiento -> Bases de Datos I (ISC-S6-BASESDEDATOSI)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version)
SELECT gen_random_uuid(), 'c0dee000-0000-4000-a000-000000000006'::uuid, 'COURSE_PASSED', gk.id, 'HIGH', false, 'ACTIVE', 0
FROM curriculum.course gk
WHERE gk.official_code = 'ISC-S6-BASESDEDATOSI'
  AND NOT EXISTS (SELECT 1 FROM curriculum.course_requirement
                   WHERE target_course_id = 'c0dee000-0000-4000-a000-000000000006'::uuid
                     AND required_course_id = gk.id AND type = 'COURSE_PASSED');

INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version)
SELECT gen_random_uuid(), 'c0dee000-0000-4000-a000-000000000007'::uuid, 'COURSE_PASSED', gk.id, 'HIGH', false, 'ACTIVE', 0
FROM curriculum.course gk
WHERE gk.official_code = 'ISC-S6-BASESDEDATOSI'
  AND NOT EXISTS (SELECT 1 FROM curriculum.course_requirement
                   WHERE target_course_id = 'c0dee000-0000-4000-a000-000000000007'::uuid
                     AND required_course_id = gk.id AND type = 'COURSE_PASSED');

INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version)
SELECT gen_random_uuid(), 'c0dee000-0000-4000-a000-000000000008'::uuid, 'COURSE_PASSED', gk.id, 'HIGH', false, 'ACTIVE', 0
FROM curriculum.course gk
WHERE gk.official_code = 'ISC-S6-BASESDEDATOSI'
  AND NOT EXISTS (SELECT 1 FROM curriculum.course_requirement
                   WHERE target_course_id = 'c0dee000-0000-4000-a000-000000000008'::uuid
                     AND required_course_id = gk.id AND type = 'COURSE_PASSED');

-- ============================================================
-- 3. CONFIGURABLE pool gate on Trabajo de Grado (design D1): >= 4 distinct PASSED
--    pool members. required_course_id is intentionally NULL (the pool has no single
--    prerequisite course); required_credits is reinterpreted as "distinct completions
--    required" per design §6.
-- ============================================================

INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, required_credits, status, version)
SELECT gen_random_uuid(), 'c0100000-0000-4000-a000-000000000001'::uuid, 'CONFIGURABLE', NULL, 4, 'ACTIVE', 0
WHERE NOT EXISTS (
    SELECT 1 FROM curriculum.course_requirement
     WHERE target_course_id = 'c0100000-0000-4000-a000-000000000001'::uuid
       AND type = 'CONFIGURABLE'
       AND status = 'ACTIVE');

-- ============================================================
-- 4. Archive the 4 superseded ordering edges (design D3 / §0 CORRECTION), seeded at
--    V012:733-746. Never deleted — ARCHIVED to preserve provenance/rollback trail.
-- ============================================================

-- Profundización III <- Profundización I (V012:732-734)
UPDATE curriculum.course_requirement
   SET status = 'ARCHIVED'
 WHERE target_course_id   = 'c0090000-0000-4000-a000-000000000004'::uuid
   AND required_course_id = 'c0080000-0000-4000-a000-000000000004'::uuid
   AND type               = 'COURSE_PASSED'
   AND status             = 'ACTIVE';

-- Profundización IV <- Profundización II (V012:736-738)
UPDATE curriculum.course_requirement
   SET status = 'ARCHIVED'
 WHERE target_course_id   = 'c0090000-0000-4000-a000-000000000005'::uuid
   AND required_course_id = 'c0080000-0000-4000-a000-000000000005'::uuid
   AND type               = 'COURSE_PASSED'
   AND status             = 'ACTIVE';

-- Trabajo de Grado <- Profundización III (V012:740-742)
UPDATE curriculum.course_requirement
   SET status = 'ARCHIVED'
 WHERE target_course_id   = 'c0100000-0000-4000-a000-000000000001'::uuid
   AND required_course_id = 'c0090000-0000-4000-a000-000000000004'::uuid
   AND type               = 'COURSE_PASSED'
   AND status             = 'ACTIVE';

-- Trabajo de Grado <- Profundización IV (V012:744-746)
UPDATE curriculum.course_requirement
   SET status = 'ARCHIVED'
 WHERE target_course_id   = 'c0100000-0000-4000-a000-000000000001'::uuid
   AND required_course_id = 'c0090000-0000-4000-a000-000000000005'::uuid
   AND type               = 'COURSE_PASSED'
   AND status             = 'ACTIVE';

-- ============================================================
-- 5. Relocate the 3 per-line gatekeepers off sequential_track (design §0 CORRECTION):
--    the logic now lives on the per-elective COURSE_PASSED edges seeded in step 2.
--    NULLing is naturally idempotent.
-- ============================================================

UPDATE curriculum.sequential_track
   SET gatekeeper_course_id = NULL
 WHERE id IN (
     'e0a20000-0000-4000-a000-000000000001', -- Línea Ingeniería del Software y HCI
     'e0a30000-0000-4000-a000-000000000001', -- Línea Infraestructura de T.I.
     'e0a40000-0000-4000-a000-000000000001'  -- Línea Gestión de la Información y el Conocimiento
 );
