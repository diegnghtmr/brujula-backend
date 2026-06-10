-- synthetic.sql — Deterministic, parametrizable synthetic dataset for k6 load tests.
--
-- QAL-PERF-003: Creates N programs × M courses/plans/offerings for reproducible load data.
-- Parameters (customize via psql \set or sed substitution before running):
--   N_PROGRAMS  — number of synthetic academic programs  (default: 50)
--   M_COURSES   — courses per study plan                 (default: 30)
--   M_OFFERINGS — offerings (schedule options) per course (default: 2)
--
-- Idempotent: all INSERTs use ON CONFLICT DO NOTHING.
-- Run via: psql -U brujula -d planner_db -f perf/seed/synthetic.sql
--          OR: psql $DATABASE_URL -f perf/seed/synthetic.sql
--
-- Schema mapping (from V001 + V010 + V011 + V016 + V019):
--   catalog.academic_program    — programs
--   catalog.study_plan          — one plan per program
--   catalog.journey             — reuses existing DIURNA journey
--   catalog.program_journey     — program → journey association
--   curriculum.course           — courses per plan
--   offering.academic_period    — one shared synthetic period
--   offering.course_offering    — offerings per course
--   offering.schedule_option    — one option per offering
--   offering.schedule_session   — one session per option
--   imports.import_batch        — one VALIDATED batch per program (for list-imports.js)
--
-- UUIDs use a deterministic scheme:
--   programs:       'a0000000-0000-4000-NNNN-000000000001' (NNNN = zero-padded program index)
--   study plans:    'b0000000-0000-4000-NNNN-000000000001'
--   courses:        'c0000000-PPPP-4000-MMMM-000000000001' (PPPP=program, MMMM=course)
--   offerings:      'd0000000-PPPP-4000-MMMM-000000000001'
--   schedule opts:  'e0000000-PPPP-4000-MMMM-000000000001'
--   import batches: 'f0000000-0000-4000-NNNN-000000000001'
--   schedule sess:  'f1000000-PPPP-4000-MMMM-000000000001'
--
-- NOTE: This seed does NOT depend on V012/V013 ISC/Lenguas seed data.
--       It is self-contained and targets only synthetic UUIDs in the 'a0000000...' range.

-- ---------------------------------------------------------------------------
-- 0. Shared synthetic academic period
-- ---------------------------------------------------------------------------
INSERT INTO offering.academic_period (id, name, start_date, end_date, status, version)
VALUES (
    '99990000-0000-4000-a000-000000000001',
    'Synthetic Period 2026-I',
    '2026-01-15',
    '2026-06-15',
    'ACTIVE',
    0
)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 1. Programs: a0000000-0000-4000-{i:04x}-000000000001  (i = 1..N_PROGRAMS)
--    Using generate_series for N_PROGRAMS = 50
-- ---------------------------------------------------------------------------
INSERT INTO catalog.academic_program (id, name, faculty, status, version)
SELECT
    ('a0000000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
    'Synthetic Program ' || i,
    'Synthetic Faculty',
    'ACTIVE',
    0
FROM generate_series(1, 50) AS i
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Program-journey associations (reuse existing DIURNA journey)
-- ---------------------------------------------------------------------------
INSERT INTO catalog.program_journey (program_id, journey_id)
SELECT
    ('a0000000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
    j.id
FROM generate_series(1, 50) AS i
CROSS JOIN catalog.journey j
WHERE j.code = 'DIURNA'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. Study plans: b0000000-0000-4000-{i:04x}-000000000001
-- ---------------------------------------------------------------------------
INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, reference_year, version)
SELECT
    ('b0000000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
    ('a0000000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
    'Synthetic Plan ' || i,
    'ACTIVE',
    160,
    2024,
    0
FROM generate_series(1, 50) AS i
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Courses: c0000000-{p:04x}-4000-{m:04x}-000000000001
--    30 courses per plan (M_COURSES = 30)
-- ---------------------------------------------------------------------------
INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version)
SELECT
    ('c0000000-' || LPAD(TO_HEX(p), 4, '0') || '-4000-' || LPAD(TO_HEX(m), 4, '0') || '-000000000001')::UUID,
    ('b0000000-0000-4000-' || LPAD(TO_HEX(p), 4, '0') || '-000000000001')::UUID,
    ('a0000000-0000-4000-' || LPAD(TO_HEX(p), 4, '0') || '-000000000001')::UUID,
    'SYN-P' || p || '-C' || m,
    'Synthetic Course P' || p || ' C' || m,
    'REQUIRED',
    3,
    4,
    'ACTIVE',
    0
FROM generate_series(1, 50) AS p
CROSS JOIN generate_series(1, 30) AS m
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. Course offerings: d0000000-{p:04x}-4000-{m:04x}-000000000001
--    2 offerings per course (M_OFFERINGS = 2) using the synthetic period
-- ---------------------------------------------------------------------------
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, version)
SELECT
    ('d0000000-' || LPAD(TO_HEX(p), 4, '0') || '-4000-' || LPAD(TO_HEX(m), 4, '0') || '-000000000001')::UUID,
    ('c0000000-' || LPAD(TO_HEX(p), 4, '0') || '-4000-' || LPAD(TO_HEX(m), 4, '0') || '-000000000001')::UUID,
    'Synthetic Course P' || p || ' C' || m,
    ('a0000000-0000-4000-' || LPAD(TO_HEX(p), 4, '0') || '-000000000001')::UUID,
    '99990000-0000-4000-a000-000000000001',
    'PRELIMINARY',
    0
FROM generate_series(1, 50) AS p
CROSS JOIN generate_series(1, 30) AS m
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. Schedule options: e0000000-{p:04x}-4000-{m:04x}-000000000001
-- ---------------------------------------------------------------------------
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
SELECT
    ('e0000000-' || LPAD(TO_HEX(p), 4, '0') || '-4000-' || LPAD(TO_HEX(m), 4, '0') || '-000000000001')::UUID,
    ('d0000000-' || LPAD(TO_HEX(p), 4, '0') || '-4000-' || LPAD(TO_HEX(m), 4, '0') || '-000000000001')::UUID,
    'Grupo ' || m,
    'DIURNA',
    0
FROM generate_series(1, 50) AS p
CROSS JOIN generate_series(1, 30) AS m
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 7. Schedule sessions: one Monday session per option
--    f1000000-{p:04x}-4000-{m:04x}-000000000001
--    Deterministic UUID derived from (p, m) so ON CONFLICT (id) DO NOTHING deduplicates re-runs.
--    NOTE: all UUID characters must be hex (0-9, a-f). Prefix 'f1' used to distinguish sessions.
-- ---------------------------------------------------------------------------
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
SELECT
    ('f1000000-' || LPAD(TO_HEX(p), 4, '0') || '-4000-' || LPAD(TO_HEX(m), 4, '0') || '-000000000001')::UUID,
    ('e0000000-' || LPAD(TO_HEX(p), 4, '0') || '-4000-' || LPAD(TO_HEX(m), 4, '0') || '-000000000001')::UUID,
    'MONDAY',
    '08:00:00',
    '10:00:00'
FROM generate_series(1, 50) AS p
CROSS JOIN generate_series(1, 30) AS m
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 8. Import batches: one VALIDATED batch per program (for list-imports.js)
--    f0000000-0000-4000-{i:04x}-000000000001
-- ---------------------------------------------------------------------------
INSERT INTO imports.import_batch (
    id, kind, program_id, plan_id, period_id, status,
    total_rows, valid_rows, validated_rows_json, issues_json,
    schema_version, version, created_at
)
SELECT
    ('f0000000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
    'COURSES',
    ('a0000000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
    ('b0000000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
    NULL,
    'VALIDATED',
    30,
    30,
    '[]'::jsonb,
    '[]'::jsonb,
    1,
    0,
    NOW()
FROM generate_series(1, 50) AS i
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 9. Staff account + program assignments (for list-imports.js)
--
-- administration.staff_account is NOT auto-provisioned by OIDC login; it is
-- managed by ADMIN users via the administration module.  We must seed it here
-- so the k6 harness is fully reproducible from a fresh docker compose up + seed
-- with no manual steps.
--
-- UUID scheme (consistent with existing pattern above):
--   staff account:      19000000-0000-4000-0001-000000000001
--   staff assignments:  19100000-0000-4000-{i:04x}-000000000001  (i = 1..10)
--
-- The 10 program UUIDs match synthetic programs seeded in block 1 above
-- (a0000000-0000-4000-{i:04x}-000000000001, i = 1..10).
--
-- No FK from staff_program_assignment.program_id to catalog.academic_program
-- (cross-schema FK intentionally omitted in V003; see migration comment).
-- ---------------------------------------------------------------------------

-- 9a. Staff account for staff@uniquindio.edu.co
INSERT INTO administration.staff_account (id, email, full_name, status, version, created_at, updated_at)
VALUES (
    '19000000-0000-4000-0001-000000000001',
    'staff@uniquindio.edu.co',
    'Staff Test',
    'ACTIVE',
    0,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;

-- 9b. Assign the staff account to synthetic programs 1..10
--     (10 assignments are sufficient for list-imports.js to return non-empty results)
--     staff_id resolved via sub-select so this block is safe whether the account was
--     seeded by 9a above or by a prior manual run that produced a different UUID.
INSERT INTO administration.staff_program_assignment (id, staff_id, program_id, active, assigned_at, version)
SELECT
    ('19100000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
    (SELECT id FROM administration.staff_account WHERE email = 'staff@uniquindio.edu.co'),
    ('a0000000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
    true,
    NOW(),
    0
FROM generate_series(1, 10) AS i
ON CONFLICT (staff_id, program_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Summary
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    RAISE NOTICE 'Synthetic seed complete:';
    RAISE NOTICE '  % programs', (SELECT COUNT(*) FROM catalog.academic_program WHERE name LIKE 'Synthetic Program%');
    RAISE NOTICE '  % study plans', (SELECT COUNT(*) FROM catalog.study_plan WHERE name LIKE 'Synthetic Plan%');
    RAISE NOTICE '  % courses', (SELECT COUNT(*) FROM curriculum.course WHERE name LIKE 'Synthetic Course%');
    RAISE NOTICE '  % offerings', (SELECT COUNT(*) FROM offering.course_offering WHERE course_name LIKE 'Synthetic Course%');
    RAISE NOTICE '  % import batches (50 total / 10 visible to the seeded staff user)', (SELECT COUNT(*) FROM imports.import_batch WHERE status = 'VALIDATED');
    RAISE NOTICE '  % staff assignments', (SELECT COUNT(*) FROM administration.staff_program_assignment WHERE staff_id = (SELECT id FROM administration.staff_account WHERE email = 'staff@uniquindio.edu.co'));
END $$;
