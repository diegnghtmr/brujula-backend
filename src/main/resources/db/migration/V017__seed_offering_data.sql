-- V017__seed_offering_data.sql
-- Forward-only migration. NEVER edit this file after it is committed.
-- Seeds one academic period (2026-2) with published course offerings.
--
-- Deliberate test fixtures (see OfferingPersistenceAdapterIT):
--   (a) SCHEDULE CLASH: Cálculo Diferencial DIURNA (opt 0011) and Programación III DIURNA (opt 0021)
--       share MONDAY 08:00-10:00 → simulator will detect SIMULATION_SCHEDULE_CLASH
--   (b) HARD PREREQ: Cálculo Diferencial (offering 0010) requires Matemáticas Generales
--       → student with no history will hit SIMULATION_PREREQUISITE_NOT_MET
--   (c) NEAR CREDIT LIMIT: Five ISC offerings totalling 16 cr (Programación III 3cr,
--       Estadística 3cr, Grafos 3cr, ED 3cr, Sistemas de Información 3cr + Cálculo 4cr = 19cr)
--       → adding all six exceeds 23 cr default limit
--   (d) ARCHIVED offering: offering 0070 with status=ARCHIVED → SIMULATION_ARCHIVED_OFFERING block
--   (e) PRELIMINARY offering: offering 0080 with status=PRELIMINARY → PRELIMINARY_OFFERING warning
--   (f) DIURNA and NOCTURNA journeys present.
--
-- UUID encoding: all offering IDs start with 00, all option IDs with 00, all session IDs with 00.
-- UUIDs must contain only hex chars [0-9a-f].
-- References: course UUIDs from V012 (Sistemas) and V013 (Lenguas).
-- Cross-schema references are opaque UUIDs — no FK.

-- ============================================================
-- Academic period 2026-2 (ACTIVE)
-- ============================================================
INSERT INTO offering.academic_period (id, name, start_date, end_date, status, version)
VALUES ('00000001-0000-4000-a000-000000000001',
        '2026-2',
        '2026-07-15',
        '2026-11-28',
        'ACTIVE',
        0);

-- ============================================================
-- ISC (Sistemas) offerings — program_id = 11111111-0000-4000-a000-000000000001
-- ============================================================

-- Cálculo Diferencial (4 cr) — FINAL — has hard prereq (Matemáticas Generales)
-- course_id = c0020000-0000-4000-a000-000000000001
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('00000001-0000-4001-a000-000000000001',
        'c0020000-0000-4000-a000-000000000001',
        'Cálculo Diferencial',
        '11111111-0000-4000-a000-000000000001',
        '00000001-0000-4000-a000-000000000001',
        'FINAL',
        'Requires Matematicas Generales (hard prereq fixture)',
        0);

-- DIURNA option: Mon/Wed/Fri 08:00-10:00 (6h/week)
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('00000001-0000-4002-a000-000000000001',
        '00000001-0000-4001-a000-000000000001',
        'Grupo A', 'DIURNA', 0);

INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
VALUES ('00000001-0000-4003-a000-000000000001', '00000001-0000-4002-a000-000000000001', 'MONDAY',    '08:00', '10:00'),
       ('00000001-0000-4003-a000-000000000002', '00000001-0000-4002-a000-000000000001', 'WEDNESDAY', '08:00', '10:00'),
       ('00000001-0000-4003-a000-000000000003', '00000001-0000-4002-a000-000000000001', 'FRIDAY',    '08:00', '10:00');

-- NOCTURNA option: Tue/Thu 18:00-21:00 (6h/week)
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('00000001-0000-4002-a000-000000000002',
        '00000001-0000-4001-a000-000000000001',
        'Grupo B', 'NOCTURNA', 0);

INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
VALUES ('00000001-0000-4003-a000-000000000004', '00000001-0000-4002-a000-000000000002', 'TUESDAY',  '18:00', '21:00'),
       ('00000001-0000-4003-a000-000000000005', '00000001-0000-4002-a000-000000000002', 'THURSDAY', '18:00', '21:00');

-- -------------------------------------------------------
-- Programación III (3 cr) — FINAL
-- course_id = c0040000-0000-4000-a000-000000000004
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('00000002-0000-4001-a000-000000000001',
        'c0040000-0000-4000-a000-000000000004',
        'Programación III',
        '11111111-0000-4000-a000-000000000001',
        '00000001-0000-4000-a000-000000000001',
        'FINAL',
        NULL,
        0);

-- DIURNA option: Mon 08:00-10:00, Wed 10:00-12:00 — MONDAY 08:00-10:00 CLASHES with Calculo DIURNA
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('00000002-0000-4002-a000-000000000001',
        '00000002-0000-4001-a000-000000000001',
        'Grupo A', 'DIURNA', 0);

INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
VALUES ('00000002-0000-4003-a000-000000000001', '00000002-0000-4002-a000-000000000001', 'MONDAY',    '08:00', '10:00'),
       ('00000002-0000-4003-a000-000000000002', '00000002-0000-4002-a000-000000000001', 'WEDNESDAY', '10:00', '12:00');

-- NOCTURNA option: Mon/Wed 18:00-19:30
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('00000002-0000-4002-a000-000000000002',
        '00000002-0000-4001-a000-000000000001',
        'Grupo B', 'NOCTURNA', 0);

INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
VALUES ('00000002-0000-4003-a000-000000000003', '00000002-0000-4002-a000-000000000002', 'MONDAY',    '18:00', '19:30'),
       ('00000002-0000-4003-a000-000000000004', '00000002-0000-4002-a000-000000000002', 'WEDNESDAY', '18:00', '19:30');

-- -------------------------------------------------------
-- Estadística y Probabilidad (3 cr) — FINAL
-- course_id = c0050000-0000-4000-a000-000000000001
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('00000003-0000-4001-a000-000000000001',
        'c0050000-0000-4000-a000-000000000001',
        'Estadística y Probabilidad',
        '11111111-0000-4000-a000-000000000001',
        '00000001-0000-4000-a000-000000000001',
        'FINAL',
        NULL,
        0);

INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('00000003-0000-4002-a000-000000000001',
        '00000003-0000-4001-a000-000000000001',
        'Grupo A', 'DIURNA', 0);

INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
VALUES ('00000003-0000-4003-a000-000000000001', '00000003-0000-4002-a000-000000000001', 'TUESDAY',  '10:00', '12:00'),
       ('00000003-0000-4003-a000-000000000002', '00000003-0000-4002-a000-000000000001', 'THURSDAY', '10:00', '12:00');

-- -------------------------------------------------------
-- Teoría de Grafos (3 cr) — FINAL
-- course_id = c0050000-0000-4000-a000-000000000002
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('00000004-0000-4001-a000-000000000001',
        'c0050000-0000-4000-a000-000000000002',
        'Teoría de Grafos',
        '11111111-0000-4000-a000-000000000001',
        '00000001-0000-4000-a000-000000000001',
        'FINAL',
        NULL,
        0);

INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('00000004-0000-4002-a000-000000000001',
        '00000004-0000-4001-a000-000000000001',
        'Grupo A', 'DIURNA', 0);

INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
VALUES ('00000004-0000-4003-a000-000000000001', '00000004-0000-4002-a000-000000000001', 'MONDAY',    '14:00', '16:00'),
       ('00000004-0000-4003-a000-000000000002', '00000004-0000-4002-a000-000000000001', 'WEDNESDAY', '14:00', '16:00');

-- -------------------------------------------------------
-- Estructuras de Datos (3 cr) — FINAL
-- course_id = c0050000-0000-4000-a000-000000000004
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('00000005-0000-4001-a000-000000000001',
        'c0050000-0000-4000-a000-000000000004',
        'Estructuras de Datos',
        '11111111-0000-4000-a000-000000000001',
        '00000001-0000-4000-a000-000000000001',
        'FINAL',
        NULL,
        0);

INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('00000005-0000-4002-a000-000000000001',
        '00000005-0000-4001-a000-000000000001',
        'Grupo A', 'NOCTURNA', 0);

INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
VALUES ('00000005-0000-4003-a000-000000000001', '00000005-0000-4002-a000-000000000001', 'TUESDAY',  '18:00', '19:30'),
       ('00000005-0000-4003-a000-000000000002', '00000005-0000-4002-a000-000000000001', 'THURSDAY', '18:00', '19:30');

-- -------------------------------------------------------
-- Sistemas de Información (3 cr) — FINAL
-- course_id = c0040000-0000-4000-a000-000000000005
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('00000006-0000-4001-a000-000000000001',
        'c0040000-0000-4000-a000-000000000005',
        'Sistemas de Información',
        '11111111-0000-4000-a000-000000000001',
        '00000001-0000-4000-a000-000000000001',
        'FINAL',
        NULL,
        0);

INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('00000006-0000-4002-a000-000000000001',
        '00000006-0000-4001-a000-000000000001',
        'Grupo A', 'DIURNA', 0);

INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
VALUES ('00000006-0000-4003-a000-000000000001', '00000006-0000-4002-a000-000000000001', 'FRIDAY', '10:00', '12:00');

-- -------------------------------------------------------
-- Programación II (3 cr) — ARCHIVED (fixture d: archived block)
-- course_id = c0030000-0000-4000-a000-000000000005
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('00000007-0000-4001-a000-000000000001',
        'c0030000-0000-4000-a000-000000000005',
        'Programación II',
        '11111111-0000-4000-a000-000000000001',
        '00000001-0000-4000-a000-000000000001',
        'ARCHIVED',
        'Archived — no longer offered this period',
        0);

INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('00000007-0000-4002-a000-000000000001',
        '00000007-0000-4001-a000-000000000001',
        'Grupo A', 'NOCTURNA', 0);

INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
VALUES ('00000007-0000-4003-a000-000000000001', '00000007-0000-4002-a000-000000000001', 'FRIDAY', '19:00', '22:00');

-- ============================================================
-- Lenguas offering — program_id = 11111111-0000-4000-b000-000000000001
-- ============================================================

-- Habilidades Comunicativas e Integradas en Francés III (4 cr) — PRELIMINARY (fixture e: warning)
-- course_id = a0050000-0000-4000-b000-000000000003  (Lenguas program, Sem V, V013)
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('00000008-0000-4001-b000-000000000001',
        'a0050000-0000-4000-b000-000000000003',
        'Habilidades Comunicativas e Integradas en Francés III',
        '11111111-0000-4000-b000-000000000001',
        '00000001-0000-4000-a000-000000000001',
        'PRELIMINARY',
        'Preliminary — schedule subject to change',
        0);

INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('00000008-0000-4002-b000-000000000001',
        '00000008-0000-4001-b000-000000000001',
        'Grupo A', 'NOCTURNA', 0);

INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
VALUES ('00000008-0000-4003-b000-000000000001', '00000008-0000-4002-b000-000000000001', 'WEDNESDAY', '18:00', '20:00');
