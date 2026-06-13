-- V021: Promote two ISC malla prerequisite edges from SOFT to HARD
--
-- Product decision (2026-06-10): The following two Ingeniería de Sistemas y
-- Computación (ISC) prerequisite edges were seeded in V012 as SOFT
-- (confidence='MEDIUM', inferred=true). Per academic review they are confirmed
-- official malla edges and must block the dependent course when unmet.
-- They are promoted to HARD (confidence='HIGH', inferred=false).
--
-- Edges promoted:
--   1. Matemáticas Discretas ← Álgebra Lineal
--      target_course_id  = c0030000-0000-4000-a000-000000000003
--      required_course_id = c0020000-0000-4000-a000-000000000002
--   2. Formulación y Evaluación de Proyectos ← Ingeniería Económica
--      target_course_id  = c0080000-0000-4000-a000-000000000001
--      required_course_id = c0070000-0000-4000-a000-000000000001
--
-- Idempotent: re-running this migration (outside Flyway, e.g., in dev) leaves
-- the data consistent. Rows are identified by the stable
-- (target_course_id, required_course_id, type) tuple — NOT by the random id.
--
-- Reversible: to revert, execute:
--   UPDATE curriculum.course_requirement
--      SET confidence = 'MEDIUM', inferred = true
--    WHERE (target_course_id, required_course_id, type) IN (
--      ('c0030000-0000-4000-a000-000000000003'::uuid,
--       'c0020000-0000-4000-a000-000000000002'::uuid,
--       'COURSE_PASSED'),
--      ('c0080000-0000-4000-a000-000000000001'::uuid,
--       'c0070000-0000-4000-a000-000000000001'::uuid,
--       'COURSE_PASSED')
--    );
--
-- No schema change. No domain/code change. Data-only.

-- Edge 1: Matemáticas Discretas ← Álgebra Lineal
UPDATE curriculum.course_requirement
   SET confidence = 'HIGH',
       inferred   = false
 WHERE target_course_id   = 'c0030000-0000-4000-a000-000000000003'::uuid
   AND required_course_id = 'c0020000-0000-4000-a000-000000000002'::uuid
   AND type               = 'COURSE_PASSED';

-- Edge 2: Formulación y Evaluación de Proyectos ← Ingeniería Económica
UPDATE curriculum.course_requirement
   SET confidence = 'HIGH',
       inferred   = false
 WHERE target_course_id   = 'c0080000-0000-4000-a000-000000000001'::uuid
   AND required_course_id = 'c0070000-0000-4000-a000-000000000001'::uuid
   AND type               = 'COURSE_PASSED';
