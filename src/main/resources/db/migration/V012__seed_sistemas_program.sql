-- V012: Seed Ingeniería de Sistemas y Computación program (ISC)
--
-- !! PROVISIONAL / NON-OFFICIAL !!
-- Source: Plan 21, Acuerdo C.A. 075/2017, 163 cr, 57 courses, 10 sem DIURNA.
-- NOCTURNA 11-semester distribution is INFERRED (topological re-binning from
-- institutional planilla — human-approved 2026-06-06 per #1610, treated as inferred).
-- Prerequisite edges: ALTA-confidence edges (malla/user-confirmed) → inferred=false;
-- MEDIA-confidence edges → inferred=true.
--
-- Satisfies: CAT-002/003, CUR-001/002/003/004/005/006/007, Design §5 V012
-- official_code minting: {PROG}-S{diurna_sem}-{SLUG}
--   PROG=ISC, SLUG = accent-fold(name) → strip non-alnum → UPPERCASE
--
-- Single transaction (Flyway default for PG DML — no DDL here).

-- ============================================================
-- 1. Academic program
-- ============================================================
INSERT INTO catalog.academic_program (id, name, faculty, status, version)
VALUES (
    '11111111-0000-4000-a000-000000000001',
    'Ingeniería de Sistemas y Computación',
    'Facultad de Ingeniería',
    'ACTIVE',
    0
);

-- ============================================================
-- 2. Journeys (shared catalog rows — insert only if not present)
-- ============================================================
INSERT INTO catalog.journey (id, code, name, active)
VALUES ('22222222-0000-4000-a000-000000000001', 'DIURNA',  'Jornada Diurna',   true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO catalog.journey (id, code, name, active)
VALUES ('22222222-0000-4000-a000-000000000002', 'NOCTURNA', 'Jornada Nocturna', true)
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 3. Program–journey associations
-- ============================================================
INSERT INTO catalog.program_journey (program_id, journey_id)
SELECT '11111111-0000-4000-a000-000000000001', id
FROM   catalog.journey
WHERE  code = 'DIURNA';

INSERT INTO catalog.program_journey (program_id, journey_id)
SELECT '11111111-0000-4000-a000-000000000001', id
FROM   catalog.journey
WHERE  code = 'NOCTURNA';

-- ============================================================
-- 4. Study plan
-- ============================================================
INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, reference_year, version)
VALUES (
    '33333333-0000-4000-a000-000000000001',
    '11111111-0000-4000-a000-000000000001',
    'Plan 21 — Reforma Curricular 2016 (Acuerdo 075/2017)',
    'ACTIVE',
    163,
    2016,
    0
);

-- ============================================================
-- 5. Courses (57 total)
--    FK: plan_id = '33333333-0000-4000-a000-000000000001'
--    FK: program_id = '11111111-0000-4000-a000-000000000001'
--    Types: REQUIRED / PERSONAL_ELECTIVE / DEEPENING / DEGREE_WORK
--    official_code: ISC-S{diurna_sem}-{SLUG}
-- ============================================================

-- --- Semester I (5 courses, 12 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0010000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S1-MATEMATICASGENERALES', 'Matemáticas Generales', 'REQUIRED', 2, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0010000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S1-INTRODUCCIONALAINGENIERIADESISTEMASYCOMPUTACION', 'Introducción a la Ingeniería de Sistemas y Computación', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0010000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S1-UNIQUINDIANIDAD', 'Uniquindianidad', 'INSTITUTIONAL', 2, 2, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0010000-0000-4000-a000-000000000004', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S1-LOGICADEPROGRAMACION', 'Lógica de Programación', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0010000-0000-4000-a000-000000000005', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S1-LECTURAYESCRITURAENINGENIERIA', 'Lectura y Escritura en Ingeniería', 'REQUIRED', 2, 4, 'ACTIVE', 0);

-- --- Semester II (6 courses, 18 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0020000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S2-CALCULODIFERENCIAL', 'Cálculo Diferencial', 'REQUIRED', 4, 6, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0020000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S2-ALGEBRALINEAL', 'Álgebra Lineal', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0020000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S2-PROGRAMACIONI', 'Programación I', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0020000-0000-4000-a000-000000000004', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S2-PENSAMIENTOSISTEMICO', 'Pensamiento Sistémico', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0020000-0000-4000-a000-000000000005', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S2-FUNDAMENTOSDEELECTRICIDADELECTRONICATELECOMUNICACIONES', 'Fundamentos de Electricidad, Electrónica, Telecomunicaciones', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0020000-0000-4000-a000-000000000006', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S2-ELECTIVADEFORMACIONPERSONALI', 'Electiva de Formación Personal I', 'PERSONAL_ELECTIVE', 2, 4, 'ACTIVE', 0);

-- --- Semester III (5 courses, 17 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0030000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S3-CALCULOINTEGRAL', 'Cálculo Integral', 'REQUIRED', 4, 5, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0030000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S3-FISICAGENERAL', 'Física General', 'REQUIRED', 4, 6, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0030000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S3-MATEMATICASDISCRETAS', 'Matemáticas Discretas', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0030000-0000-4000-a000-000000000004', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S3-TEORIAYDISENIOORGANIZACIONAL', 'Teoría y Diseño Organizacional', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0030000-0000-4000-a000-000000000005', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S3-PROGRAMACIONII', 'Programación II', 'REQUIRED', 3, 4, 'ACTIVE', 0);

-- --- Semester IV (6 courses, 18 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0040000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S4-CALCULOMULTIVARIADOYVECTORIAL', 'Cálculo Multivariado y Vectorial', 'REQUIRED', 4, 5, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0040000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S4-ECUACIONESDIFERENCIALES', 'Ecuaciones Diferenciales', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0040000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S4-SEGUNDALENGUAI', 'Segunda Lengua I', 'REQUIRED', 2, 2, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0040000-0000-4000-a000-000000000004', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S4-PROGRAMACIONIII', 'Programación III', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0040000-0000-4000-a000-000000000005', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S4-SISTEMASDEINFORMACION', 'Sistemas de Información', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0040000-0000-4000-a000-000000000006', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S4-FUNDAMENTOSDEINFRAESTRUCTURACOMPUTACIONAL', 'Fundamentos de Infraestructura Computacional', 'REQUIRED', 3, 4, 'ACTIVE', 0);

-- --- Semester V (6 courses, 18 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0050000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S5-ESTADISTICAYPROBABILIDAD', 'Estadística y Probabilidad', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0050000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S5-TEORIADEGRAFOS', 'Teoría de Grafos', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0050000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S5-SEGUNDALENGUAII', 'Segunda Lengua II', 'REQUIRED', 2, 2, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0050000-0000-4000-a000-000000000004', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S5-ESTRUCTURASDEDATOS', 'Estructuras de Datos', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0050000-0000-4000-a000-000000000005', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S5-INGENIERIADESOFTWAREI', 'Ingeniería de Software I', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0050000-0000-4000-a000-000000000006', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S5-INFRAESTRUCTURACOMPUTACIONAL', 'Infraestructura Computacional', 'REQUIRED', 4, 6, 'ACTIVE', 0);

-- --- Semester VI (6 courses, 19 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0060000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S6-DIRECCIONESTRATEGICADETI', 'Dirección Estratégica de TI', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0060000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S6-ANALISISNUMERICO', 'Análisis Numérico', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0060000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S6-BASESDEDATOSI', 'Bases de Datos I', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0060000-0000-4000-a000-000000000004', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S6-PROGRAMACIONAVANZADA', 'Programación Avanzada', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0060000-0000-4000-a000-000000000005', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S6-INGENIERIADESOFTWAREII', 'Ingeniería de Software II', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0060000-0000-4000-a000-000000000006', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S6-INFRAESTRUCTURADECOMUNICACIONES', 'Infraestructura de Comunicaciones', 'REQUIRED', 4, 6, 'ACTIVE', 0);

-- --- Semester VII (6 courses, 18 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0070000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S7-INGENIERIAECONOMICA', 'Ingeniería Económica', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0070000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S7-FUNDAMENTOSDEHCI', 'Fundamentos de HCI', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0070000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S7-SEMINARIODEINVESTIGACION', 'Seminario de Investigación', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0070000-0000-4000-a000-000000000004', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S7-BASESDEDATOSII', 'Bases de Datos II', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0070000-0000-4000-a000-000000000005', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S7-INGENIERIADESOFTWAREIII', 'Ingeniería de Software III', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0070000-0000-4000-a000-000000000006', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S7-ADMINISTRACIONDEINFRAESTRUCTURADETI', 'Administración de Infraestructura de TI', 'REQUIRED', 3, 4, 'ACTIVE', 0);

-- --- Semester VIII (6 courses, 16 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0080000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S8-FORMULACIONYEVALUACIONDEPROYECTOS', 'Formulación y Evaluación de Proyectos', 'REQUIRED', 2, 2, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0080000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S8-TEORIADELENGUAJESFORMALES', 'Teoría de Lenguajes Formales', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0080000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S8-ANALISISDEALGORITMOS', 'Análisis de Algoritmos', 'REQUIRED', 3, 4, 'ACTIVE', 0);

-- Profundización slots I & II (DEEPENING elective placeholders — sem VIII)
INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0080000-0000-4000-a000-000000000004', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S8-PROFUNDIZACIONI', 'Profundización I', 'DEEPENING', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0080000-0000-4000-a000-000000000005', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S8-PROFUNDIZACIONII', 'Profundización II', 'DEEPENING', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0080000-0000-4000-a000-000000000006', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S8-SEMINARIODEINGENIERIA', 'Seminario de Ingeniería', 'REQUIRED', 2, 4, 'ACTIVE', 0);

-- --- Semester IX (6 courses, 15 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0090000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S9-ADMINISTRACIONDEPROYECTOS', 'Administración de Proyectos', 'REQUIRED', 2, 2, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0090000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S9-INTELIGENCIAARTIFICIAL', 'Inteligencia Artificial', 'REQUIRED', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0090000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S9-LEGISLACIONLABORALYPROPIEDADINTELECTUAL', 'Legislación Laboral y Propiedad Intelectual', 'REQUIRED', 2, 2, 'ACTIVE', 0);

-- Profundización slots III & IV (DEEPENING elective placeholders — sem IX)
INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0090000-0000-4000-a000-000000000004', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S9-PROFUNDIZACIONIII', 'Profundización III', 'DEEPENING', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0090000-0000-4000-a000-000000000005', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S9-PROFUNDIZACIONIV', 'Profundización IV', 'DEEPENING', 3, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0090000-0000-4000-a000-000000000006', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S9-ADMINISTRACION', 'Administración', 'REQUIRED', 2, 4, 'ACTIVE', 0);

-- --- Semester X (5 courses, 12 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0100000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S10-TRABAJODEGRADO', 'Trabajo de Grado', 'DEGREE_WORK', 6, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0100000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S10-ELECTIVADEFORMACIONPERSONALII', 'Electiva de Formación Personal II', 'PERSONAL_ELECTIVE', 2, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0100000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S10-ELECTIVADEFORMACIONPERSONALIII', 'Electiva de Formación Personal III', 'PERSONAL_ELECTIVE', 2, 4, 'ACTIVE', 0);

-- Cátedra Multidisciplinar (0 credits — graduation requirement, not a semester prereq)
INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0100000-0000-4000-a000-000000000004', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S10-CATEDRAMULTIDISCIPLINAR', 'Cátedra Multidisciplinar', 'INSTITUTIONAL', 0, 4, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('c0100000-0000-4000-a000-000000000005', '33333333-0000-4000-a000-000000000001', '11111111-0000-4000-a000-000000000001',
 'ISC-S10-ETICAPROFESIONAL', 'Ética Profesional', 'REQUIRED', 2, 4, 'ACTIVE', 0);

-- ============================================================
-- 6. Course recommended semesters
--    DIURNA: official semesters from Plan 21 malla
--    NOCTURNA: approved inferred distribution (11 sem, #1607/#1610)
-- ============================================================

-- Semester I courses (DIURNA=1, NOCTURNA=1)
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0010000-0000-4000-a000-000000000001', 'DIURNA', 1),
('c0010000-0000-4000-a000-000000000002', 'DIURNA', 1),
('c0010000-0000-4000-a000-000000000003', 'DIURNA', 1),
('c0010000-0000-4000-a000-000000000004', 'DIURNA', 1),
('c0010000-0000-4000-a000-000000000005', 'DIURNA', 1);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0010000-0000-4000-a000-000000000001', 'NOCTURNA', 1),
('c0010000-0000-4000-a000-000000000002', 'NOCTURNA', 1),
('c0010000-0000-4000-a000-000000000003', 'NOCTURNA', 1),
('c0010000-0000-4000-a000-000000000004', 'NOCTURNA', 1),
('c0010000-0000-4000-a000-000000000005', 'NOCTURNA', 1);

-- Semester II courses
-- DIURNA: Cálculo Dif, Álgebra Lineal, Prog I, Pensamiento Sistémico, Fund. Electricidad, EFP I
-- NOCTURNA: same
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0020000-0000-4000-a000-000000000001', 'DIURNA', 2),
('c0020000-0000-4000-a000-000000000002', 'DIURNA', 2),
('c0020000-0000-4000-a000-000000000003', 'DIURNA', 2),
('c0020000-0000-4000-a000-000000000004', 'DIURNA', 2),
('c0020000-0000-4000-a000-000000000005', 'DIURNA', 2),
('c0020000-0000-4000-a000-000000000006', 'DIURNA', 2);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0020000-0000-4000-a000-000000000001', 'NOCTURNA', 2),
('c0020000-0000-4000-a000-000000000002', 'NOCTURNA', 2),
('c0020000-0000-4000-a000-000000000003', 'NOCTURNA', 2),
('c0020000-0000-4000-a000-000000000004', 'NOCTURNA', 2),
('c0020000-0000-4000-a000-000000000005', 'NOCTURNA', 2),
-- EFP I flexible from sem 2 nocturna (institutional planilla)
('c0020000-0000-4000-a000-000000000006', 'NOCTURNA', 2);

-- Semester III courses
-- DIURNA=3, NOCTURNA=3
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0030000-0000-4000-a000-000000000001', 'DIURNA', 3),
('c0030000-0000-4000-a000-000000000002', 'DIURNA', 3),
('c0030000-0000-4000-a000-000000000003', 'DIURNA', 3),
('c0030000-0000-4000-a000-000000000004', 'DIURNA', 3),
('c0030000-0000-4000-a000-000000000005', 'DIURNA', 3);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0030000-0000-4000-a000-000000000001', 'NOCTURNA', 3),
('c0030000-0000-4000-a000-000000000002', 'NOCTURNA', 3),
('c0030000-0000-4000-a000-000000000003', 'NOCTURNA', 3),
('c0030000-0000-4000-a000-000000000004', 'NOCTURNA', 3),
('c0030000-0000-4000-a000-000000000005', 'NOCTURNA', 3);

-- Semester IV courses
-- DIURNA=4, NOCTURNA=4
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0040000-0000-4000-a000-000000000001', 'DIURNA', 4),
('c0040000-0000-4000-a000-000000000002', 'DIURNA', 4),
('c0040000-0000-4000-a000-000000000003', 'DIURNA', 4),
('c0040000-0000-4000-a000-000000000004', 'DIURNA', 4),
('c0040000-0000-4000-a000-000000000005', 'DIURNA', 4),
('c0040000-0000-4000-a000-000000000006', 'DIURNA', 4);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0040000-0000-4000-a000-000000000001', 'NOCTURNA', 4),
('c0040000-0000-4000-a000-000000000002', 'NOCTURNA', 4),
('c0040000-0000-4000-a000-000000000003', 'NOCTURNA', 4),
('c0040000-0000-4000-a000-000000000004', 'NOCTURNA', 4),
('c0040000-0000-4000-a000-000000000005', 'NOCTURNA', 4),
('c0040000-0000-4000-a000-000000000006', 'NOCTURNA', 4);

-- Semester V courses
-- DIURNA=5
-- NOCTURNA: Estadística=5, T.Grafos=5, Seg.Lengua II=5, Estructuras=5
--           Ing.Software I=6 (moved per #1607), Infra.Computacional=6 (moved per #1607)
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0050000-0000-4000-a000-000000000001', 'DIURNA', 5),
('c0050000-0000-4000-a000-000000000002', 'DIURNA', 5),
('c0050000-0000-4000-a000-000000000003', 'DIURNA', 5),
('c0050000-0000-4000-a000-000000000004', 'DIURNA', 5),
('c0050000-0000-4000-a000-000000000005', 'DIURNA', 5),
('c0050000-0000-4000-a000-000000000006', 'DIURNA', 5);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0050000-0000-4000-a000-000000000001', 'NOCTURNA', 5),
('c0050000-0000-4000-a000-000000000002', 'NOCTURNA', 5),
('c0050000-0000-4000-a000-000000000003', 'NOCTURNA', 5),
('c0050000-0000-4000-a000-000000000004', 'NOCTURNA', 5),
-- Ing.Software I moved from diurna V to nocturna VI (#1607 inferred)
('c0050000-0000-4000-a000-000000000005', 'NOCTURNA', 6),
-- Infra.Computacional moved from diurna V to nocturna VI (#1607 inferred)
('c0050000-0000-4000-a000-000000000006', 'NOCTURNA', 6);

-- Semester VI courses
-- DIURNA=6
-- NOCTURNA: Direc.Estratégica=6, Análisis Num=6, BD I=6, Prog.Avanzada=7, IS II=7, Infra.Com=7
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0060000-0000-4000-a000-000000000001', 'DIURNA', 6),
('c0060000-0000-4000-a000-000000000002', 'DIURNA', 6),
('c0060000-0000-4000-a000-000000000003', 'DIURNA', 6),
('c0060000-0000-4000-a000-000000000004', 'DIURNA', 6),
('c0060000-0000-4000-a000-000000000005', 'DIURNA', 6),
('c0060000-0000-4000-a000-000000000006', 'DIURNA', 6);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
-- Direc.Estratégica: prereq = Estadística(N5) → N6 OK
('c0060000-0000-4000-a000-000000000001', 'NOCTURNA', 6),
-- Análisis Numérico: prereq = Ecuaciones Dif(N4) → N6 OK
('c0060000-0000-4000-a000-000000000002', 'NOCTURNA', 6),
-- BD I: prereq = Estadística(N5) + Estructuras(N5) → N6 OK
('c0060000-0000-4000-a000-000000000003', 'NOCTURNA', 6),
-- Programación Avanzada: prereq = Estructuras(N5) → N7 OK
('c0060000-0000-4000-a000-000000000004', 'NOCTURNA', 7),
-- IS II: prereq = IS I (N6) → N7 OK
('c0060000-0000-4000-a000-000000000005', 'NOCTURNA', 7),
-- Infra.Comunicaciones: prereq = Infra.Computacional(N6) → N7 OK
('c0060000-0000-4000-a000-000000000006', 'NOCTURNA', 7);

-- Semester VII courses
-- DIURNA=7
-- NOCTURNA: Ing.Económica=8, Fund.HCI=8, Sem.Inv=8, BD II=7, IS III=8, Admin.Infra=8
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0070000-0000-4000-a000-000000000001', 'DIURNA', 7),
('c0070000-0000-4000-a000-000000000002', 'DIURNA', 7),
('c0070000-0000-4000-a000-000000000003', 'DIURNA', 7),
('c0070000-0000-4000-a000-000000000004', 'DIURNA', 7),
('c0070000-0000-4000-a000-000000000005', 'DIURNA', 7),
('c0070000-0000-4000-a000-000000000006', 'DIURNA', 7);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
-- Ing.Económica: prereq = Direc.Estratégica(N6) → N8 OK
('c0070000-0000-4000-a000-000000000001', 'NOCTURNA', 8),
-- Fund.HCI: prereq = IS I(N6) → N8 OK
('c0070000-0000-4000-a000-000000000002', 'NOCTURNA', 8),
-- Sem.Investigación: no prereq → N8
('c0070000-0000-4000-a000-000000000003', 'NOCTURNA', 8),
-- BD II: prereq = BD I(N6) + Prog.Avanzada(N7) → N7 OK
('c0070000-0000-4000-a000-000000000004', 'NOCTURNA', 7),
-- IS III: prereq = IS II(N7) → N8 OK
('c0070000-0000-4000-a000-000000000005', 'NOCTURNA', 8),
-- Admin.Infraestructura TI: prereq = Infra.Comunicaciones(N7) → N8 OK
('c0070000-0000-4000-a000-000000000006', 'NOCTURNA', 8);

-- Semester VIII courses
-- DIURNA=8
-- NOCTURNA: Form.Evaluación=9, TLF=7, Análisis.Alg=9, Prof.I=9, Prof.II=9, Sem.Ingeniería=10
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0080000-0000-4000-a000-000000000001', 'DIURNA', 8),
('c0080000-0000-4000-a000-000000000002', 'DIURNA', 8),
('c0080000-0000-4000-a000-000000000003', 'DIURNA', 8),
('c0080000-0000-4000-a000-000000000004', 'DIURNA', 8),
('c0080000-0000-4000-a000-000000000005', 'DIURNA', 8),
('c0080000-0000-4000-a000-000000000006', 'DIURNA', 8);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
-- Formulación y Evaluación: prereq = Ing.Económica(N8) → N9 OK
('c0080000-0000-4000-a000-000000000001', 'NOCTURNA', 9),
-- TLF: prereq = Seminario.Investigación(N8) → N7? WAIT - Sem.Inv is in N8, TLF must be > N8.
-- But TLF's prereq from prereqs.md = Teoría de Grafos (N5 diurna, N5 nocturna).
-- However looking at the approved nocturna distribution in #1607: TLF→sem 7.
-- WAIT: I need to re-check. The oficial prereq doc says TLF ← Teoría de Grafos (ALTA, malla).
-- The plan doc says TLF ← Seminario de Investigación (in the official JSON).
-- We use the prerrequisitos-inferidos.md as AUTHORITATIVE: TLF ← Teoría de Grafos (ALTA, malla, confirmed by user).
-- Teoría de Grafos is NOCTURNA=5, TLF in approved dist is NOCTURNA=7. 7>5 ✓ no forward dependency.
('c0080000-0000-4000-a000-000000000002', 'NOCTURNA', 7),
-- Análisis de Algoritmos: prereq = Estructuras de Datos(N5) → N9 OK
('c0080000-0000-4000-a000-000000000003', 'NOCTURNA', 9),
-- Prof I (DEEPENING): line prereq varies → N9 (gatekeeper IS I at N6, Admin Infra TI at N8, BD I at N6)
('c0080000-0000-4000-a000-000000000004', 'NOCTURNA', 9),
-- Prof II: same as Prof I → N9
('c0080000-0000-4000-a000-000000000005', 'NOCTURNA', 9),
-- Sem.Ingeniería: no prereq → N10
('c0080000-0000-4000-a000-000000000006', 'NOCTURNA', 10);

-- Semester IX courses
-- DIURNA=9
-- NOCTURNA: Admin.Proyectos=10, IA=10, Legislación=10, Prof.III=10, Prof.IV=10, Admin=9
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0090000-0000-4000-a000-000000000001', 'DIURNA', 9),
('c0090000-0000-4000-a000-000000000002', 'DIURNA', 9),
('c0090000-0000-4000-a000-000000000003', 'DIURNA', 9),
('c0090000-0000-4000-a000-000000000004', 'DIURNA', 9),
('c0090000-0000-4000-a000-000000000005', 'DIURNA', 9),
('c0090000-0000-4000-a000-000000000006', 'DIURNA', 9);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
-- Admin.Proyectos: prereq = Formulación y Evaluación(N9) → N10 OK
('c0090000-0000-4000-a000-000000000001', 'NOCTURNA', 10),
-- IA: prereq = Admin.Proyectos(N10) + Análisis.Alg(N9) → N10 OK (both prereqs ≤ N10)
-- Wait: Admin.Proyectos is at N10 and IA requires Admin.Proyectos → IA must be > N10.
-- IA prereq from prerrequisitos.md: "Administración de Proyectos; Análisis de Algoritmos"
-- Both in N9 and N10 respectively... Admin.Proyectos(N10), Análisis.Alg(N9).
-- But IA is currently set to N10 in #1607 table. Admin.Proyectos is also N10.
-- That creates a forward dependency! IA(N10) requires Admin.Proyectos(N10) — same semester is OK
-- (semester ordering: required_sem <= target_sem). So N10 <= N10 is fine. ✓
-- But wait: let me re-read the spec. From #1607: IA is N10, Admin.Proyectos is N10.
-- The validation is: required_course.semester <= target_course.semester.
-- Admin.Proyectos(N10) <= IA(N10) → 10 <= 10 → OK. ✓
-- Análisis.Algoritmos(N9) <= IA(N10) → 9 <= 10 → OK. ✓
('c0090000-0000-4000-a000-000000000002', 'NOCTURNA', 10),
-- Legislación: no prereq → N10
('c0090000-0000-4000-a000-000000000003', 'NOCTURNA', 10),
-- Prof.III: prereq = Prof.I(N9) + line gatekeeper → N10 OK
('c0090000-0000-4000-a000-000000000004', 'NOCTURNA', 10),
-- Prof.IV: prereq = Prof.II(N9) + line gatekeeper → N10 OK
('c0090000-0000-4000-a000-000000000005', 'NOCTURNA', 10),
-- Admin: no prereq → N9 (#1607)
('c0090000-0000-4000-a000-000000000006', 'NOCTURNA', 9);

-- Semester X courses
-- DIURNA=10
-- NOCTURNA: Trabajo de Grado=11, EFP II & III = flexible (2, per #1607), Cátedra=graduation_req (10), Ética=11
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('c0100000-0000-4000-a000-000000000001', 'DIURNA', 10),
('c0100000-0000-4000-a000-000000000002', 'DIURNA', 10),
('c0100000-0000-4000-a000-000000000003', 'DIURNA', 10),
('c0100000-0000-4000-a000-000000000004', 'DIURNA', 10),
('c0100000-0000-4000-a000-000000000005', 'DIURNA', 10);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
-- Trabajo de Grado: prereq = Prof.III(N10) + Prof.IV(N10) → N11 OK
('c0100000-0000-4000-a000-000000000001', 'NOCTURNA', 11),
-- EFP II & III: flexible from sem 2 in nocturna (placed at 2 per #1607)
('c0100000-0000-4000-a000-000000000002', 'NOCTURNA', 2),
('c0100000-0000-4000-a000-000000000003', 'NOCTURNA', 2),
-- Cátedra Multidisciplinar: graduation req, placed at N10
('c0100000-0000-4000-a000-000000000004', 'NOCTURNA', 10),
-- Ética Profesional: no prereq → N11
('c0100000-0000-4000-a000-000000000005', 'NOCTURNA', 11);

-- ============================================================
-- 7. Course requirements (prerequisite edges)
--    Sources: prerrequisitos-inferidos.md
--    ALTA + confirmed by user → confidence=HIGH, inferred=false
--    MEDIA → confidence=MEDIUM, inferred=true
-- ============================================================

-- Cálculo Diferencial ← Matemáticas Generales (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0020000-0000-4000-a000-000000000001', 'COURSE_PASSED', 'c0010000-0000-4000-a000-000000000001', 'HIGH', false, 'ACTIVE', 0);

-- Álgebra Lineal ← Matemáticas Generales (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0020000-0000-4000-a000-000000000002', 'COURSE_PASSED', 'c0010000-0000-4000-a000-000000000001', 'HIGH', false, 'ACTIVE', 0);

-- Programación I ← Lógica de Programación (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0020000-0000-4000-a000-000000000003', 'COURSE_PASSED', 'c0010000-0000-4000-a000-000000000004', 'HIGH', false, 'ACTIVE', 0);

-- Cálculo Integral ← Cálculo Diferencial (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0030000-0000-4000-a000-000000000001', 'COURSE_PASSED', 'c0020000-0000-4000-a000-000000000001', 'HIGH', false, 'ACTIVE', 0);

-- Física General ← Cálculo Diferencial (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0030000-0000-4000-a000-000000000002', 'COURSE_PASSED', 'c0020000-0000-4000-a000-000000000001', 'HIGH', false, 'ACTIVE', 0);

-- Matemáticas Discretas ← Álgebra Lineal (MEDIA/malla → inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0030000-0000-4000-a000-000000000003', 'COURSE_PASSED', 'c0020000-0000-4000-a000-000000000002', 'MEDIUM', true, 'ACTIVE', 0);

-- Teoría y Diseño Organizacional ← Pensamiento Sistémico (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0030000-0000-4000-a000-000000000004', 'COURSE_PASSED', 'c0020000-0000-4000-a000-000000000004', 'HIGH', false, 'ACTIVE', 0);

-- Programación II ← Programación I (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0030000-0000-4000-a000-000000000005', 'COURSE_PASSED', 'c0020000-0000-4000-a000-000000000003', 'HIGH', false, 'ACTIVE', 0);

-- Cálculo Multivariado ← Cálculo Integral (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0040000-0000-4000-a000-000000000001', 'COURSE_PASSED', 'c0030000-0000-4000-a000-000000000001', 'HIGH', false, 'ACTIVE', 0);

-- Ecuaciones Diferenciales ← Cálculo Integral (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0040000-0000-4000-a000-000000000002', 'COURSE_PASSED', 'c0030000-0000-4000-a000-000000000001', 'HIGH', false, 'ACTIVE', 0);

-- Programación III ← Programación II (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0040000-0000-4000-a000-000000000004', 'COURSE_PASSED', 'c0030000-0000-4000-a000-000000000005', 'HIGH', false, 'ACTIVE', 0);

-- Sistemas de Información ← Teoría y Diseño Organizacional (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0040000-0000-4000-a000-000000000005', 'COURSE_PASSED', 'c0030000-0000-4000-a000-000000000004', 'HIGH', false, 'ACTIVE', 0);

-- Fundamentos Infraestructura Computacional ← Fund.Electricidad,Electrónica,Telecom (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0040000-0000-4000-a000-000000000006', 'COURSE_PASSED', 'c0020000-0000-4000-a000-000000000005', 'HIGH', false, 'ACTIVE', 0);

-- Estadística y Probabilidad ← Cálculo Integral (ALTA/usuario confirmed)
-- Note: user confirmed ← Cálculo Integral (not Cálculo Multivariado per old error)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0050000-0000-4000-a000-000000000001', 'COURSE_PASSED', 'c0030000-0000-4000-a000-000000000001', 'HIGH', false, 'ACTIVE', 0);

-- Teoría de Grafos ← Matemáticas Discretas (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0050000-0000-4000-a000-000000000002', 'COURSE_PASSED', 'c0030000-0000-4000-a000-000000000003', 'HIGH', false, 'ACTIVE', 0);

-- Segunda Lengua II ← Segunda Lengua I (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0050000-0000-4000-a000-000000000003', 'COURSE_PASSED', 'c0040000-0000-4000-a000-000000000003', 'HIGH', false, 'ACTIVE', 0);

-- Estructuras de Datos ← Programación III (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0050000-0000-4000-a000-000000000004', 'COURSE_PASSED', 'c0040000-0000-4000-a000-000000000004', 'HIGH', false, 'ACTIVE', 0);

-- Ingeniería de Software I ← Programación III (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0050000-0000-4000-a000-000000000005', 'COURSE_PASSED', 'c0040000-0000-4000-a000-000000000004', 'HIGH', false, 'ACTIVE', 0);

-- Ingeniería de Software I ← Sistemas de Información (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0050000-0000-4000-a000-000000000005', 'COURSE_PASSED', 'c0040000-0000-4000-a000-000000000005', 'HIGH', false, 'ACTIVE', 0);

-- Infraestructura Computacional ← Fundamentos Infraestructura Computacional (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0050000-0000-4000-a000-000000000006', 'COURSE_PASSED', 'c0040000-0000-4000-a000-000000000006', 'HIGH', false, 'ACTIVE', 0);

-- Dirección Estratégica de TI ← Sistemas de Información (ALTA/usuario confirmed)
-- requires: Sistemas de Información
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0060000-0000-4000-a000-000000000001', 'COURSE_PASSED', 'c0040000-0000-4000-a000-000000000005', 'HIGH', false, 'ACTIVE', 0);

-- Análisis Numérico ← Ecuaciones Diferenciales (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0060000-0000-4000-a000-000000000002', 'COURSE_PASSED', 'c0040000-0000-4000-a000-000000000002', 'HIGH', false, 'ACTIVE', 0);

-- Bases de Datos I ← Estadística y Probabilidad (ALTA/usuario confirmed)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0060000-0000-4000-a000-000000000003', 'COURSE_PASSED', 'c0050000-0000-4000-a000-000000000001', 'HIGH', false, 'ACTIVE', 0);

-- Bases de Datos I ← Estructuras de Datos (ALTA/usuario confirmed)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0060000-0000-4000-a000-000000000003', 'COURSE_PASSED', 'c0050000-0000-4000-a000-000000000004', 'HIGH', false, 'ACTIVE', 0);

-- Programación Avanzada ← Estructuras de Datos (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0060000-0000-4000-a000-000000000004', 'COURSE_PASSED', 'c0050000-0000-4000-a000-000000000004', 'HIGH', false, 'ACTIVE', 0);

-- Ingeniería de Software II ← Ingeniería de Software I (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0060000-0000-4000-a000-000000000005', 'COURSE_PASSED', 'c0050000-0000-4000-a000-000000000005', 'HIGH', false, 'ACTIVE', 0);

-- Infraestructura de Comunicaciones ← Infraestructura Computacional (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0060000-0000-4000-a000-000000000006', 'COURSE_PASSED', 'c0050000-0000-4000-a000-000000000006', 'HIGH', false, 'ACTIVE', 0);

-- Ingeniería Económica ← Estadística y Probabilidad (ALTA/usuario confirmed)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0070000-0000-4000-a000-000000000001', 'COURSE_PASSED', 'c0050000-0000-4000-a000-000000000001', 'HIGH', false, 'ACTIVE', 0);

-- Fundamentos de HCI ← Ingeniería de Software I (ALTA/usuario confirmed)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0070000-0000-4000-a000-000000000002', 'COURSE_PASSED', 'c0050000-0000-4000-a000-000000000005', 'HIGH', false, 'ACTIVE', 0);

-- Bases de Datos II ← Bases de Datos I (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0070000-0000-4000-a000-000000000004', 'COURSE_PASSED', 'c0060000-0000-4000-a000-000000000003', 'HIGH', false, 'ACTIVE', 0);

-- Bases de Datos II ← Programación Avanzada (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0070000-0000-4000-a000-000000000004', 'COURSE_PASSED', 'c0060000-0000-4000-a000-000000000004', 'HIGH', false, 'ACTIVE', 0);

-- Ingeniería de Software III ← Ingeniería de Software II (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0070000-0000-4000-a000-000000000005', 'COURSE_PASSED', 'c0060000-0000-4000-a000-000000000005', 'HIGH', false, 'ACTIVE', 0);

-- Administración Infraestructura TI ← Infraestructura de Comunicaciones (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0070000-0000-4000-a000-000000000006', 'COURSE_PASSED', 'c0060000-0000-4000-a000-000000000006', 'HIGH', false, 'ACTIVE', 0);

-- Formulación y Evaluación de Proyectos ← Ingeniería Económica (MEDIA/malla → inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0080000-0000-4000-a000-000000000001', 'COURSE_PASSED', 'c0070000-0000-4000-a000-000000000001', 'MEDIUM', true, 'ACTIVE', 0);

-- Teoría de Lenguajes Formales ← Teoría de Grafos (ALTA/usuario confirmed)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0080000-0000-4000-a000-000000000002', 'COURSE_PASSED', 'c0050000-0000-4000-a000-000000000002', 'HIGH', false, 'ACTIVE', 0);

-- Análisis de Algoritmos ← Estructuras de Datos (ALTA/usuario confirmed)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0080000-0000-4000-a000-000000000003', 'COURSE_PASSED', 'c0050000-0000-4000-a000-000000000004', 'HIGH', false, 'ACTIVE', 0);

-- Profundización I and Profundización II: the per-line gatekeeper prereq is modeled at the
-- sequential_track level via gatekeeper_course_id (added by V014). No universal course_requirement
-- edge is seeded here to avoid wrongly forcing IS I on Infra TI / Gestión Info students.
-- The slot-to-slot sequence edges (Prof III ← Prof I, Prof IV ← Prof II) are line-independent
-- and are kept below (as seeded originally).

-- Administración de Proyectos ← Formulación y Evaluación de Proyectos (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0090000-0000-4000-a000-000000000001', 'COURSE_PASSED', 'c0080000-0000-4000-a000-000000000001', 'HIGH', false, 'ACTIVE', 0);

-- Inteligencia Artificial ← Administración de Proyectos (ALTA/malla)
-- Note: plan JSON says "Administración de Proyectos; Análisis de Algoritmos" as prereqs.
-- Using Análisis de Algoritmos as the key technical prereq per the malla.
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0090000-0000-4000-a000-000000000002', 'COURSE_PASSED', 'c0090000-0000-4000-a000-000000000001', 'HIGH', false, 'ACTIVE', 0);

-- Inteligencia Artificial ← Análisis de Algoritmos (ALTA/malla)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0090000-0000-4000-a000-000000000002', 'COURSE_PASSED', 'c0080000-0000-4000-a000-000000000003', 'HIGH', false, 'ACTIVE', 0);

-- Profundización III ← Profundización I (ALTA/PDF oficial + usuario)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0090000-0000-4000-a000-000000000004', 'COURSE_PASSED', 'c0080000-0000-4000-a000-000000000004', 'HIGH', false, 'ACTIVE', 0);

-- Profundización IV ← Profundización II (ALTA/PDF oficial + usuario)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0090000-0000-4000-a000-000000000005', 'COURSE_PASSED', 'c0080000-0000-4000-a000-000000000005', 'HIGH', false, 'ACTIVE', 0);

-- Trabajo de Grado ← Profundización III (ALTA/usuario)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0100000-0000-4000-a000-000000000001', 'COURSE_PASSED', 'c0090000-0000-4000-a000-000000000004', 'HIGH', false, 'ACTIVE', 0);

-- Trabajo de Grado ← Profundización IV (ALTA/usuario)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'c0100000-0000-4000-a000-000000000001', 'COURSE_PASSED', 'c0090000-0000-4000-a000-000000000005', 'HIGH', false, 'ACTIVE', 0);

-- ============================================================
-- 8. Sequential tracks
-- ============================================================

-- --- PERSONAL_ELECTIVE track: Electivas de Formación Personal ---
INSERT INTO curriculum.sequential_track (id, plan_id, kind, label, consecutive_simultaneity_allowed, version) VALUES
('e0a10000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001',
 'PERSONAL_ELECTIVE', 'Electivas de Formación Personal', false, 0);

INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b10000-0000-4000-a000-000000000001', 'e0a10000-0000-4000-a000-000000000001', 1);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b10000-0000-4000-a000-000000000002', 'e0a10000-0000-4000-a000-000000000001', 2);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b10000-0000-4000-a000-000000000003', 'e0a10000-0000-4000-a000-000000000001', 3);

INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b10000-0000-4000-a000-000000000001', 'c0020000-0000-4000-a000-000000000006');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b10000-0000-4000-a000-000000000002', 'c0100000-0000-4000-a000-000000000002');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b10000-0000-4000-a000-000000000003', 'c0100000-0000-4000-a000-000000000003');

-- --- DEEPENING track: Línea Ingeniería del Software y HCI ---
-- Gatekeeper: Ingeniería de Software I
INSERT INTO curriculum.sequential_track (id, plan_id, kind, label, consecutive_simultaneity_allowed, version) VALUES
('e0a20000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001',
 'DEEPENING', 'Línea de Profundización: Ingeniería del Software y HCI', false, 0);

INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b20000-0000-4000-a000-000000000001', 'e0a20000-0000-4000-a000-000000000001', 1);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b20000-0000-4000-a000-000000000002', 'e0a20000-0000-4000-a000-000000000001', 2);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b20000-0000-4000-a000-000000000003', 'e0a20000-0000-4000-a000-000000000001', 3);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b20000-0000-4000-a000-000000000004', 'e0a20000-0000-4000-a000-000000000001', 4);

-- Levels 1 and 2 = Profundización I and II (the plan slots)
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b20000-0000-4000-a000-000000000001', 'c0080000-0000-4000-a000-000000000004');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b20000-0000-4000-a000-000000000002', 'c0080000-0000-4000-a000-000000000005');
-- Levels 3 and 4 = Profundización III and IV (the plan slots)
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b20000-0000-4000-a000-000000000003', 'c0090000-0000-4000-a000-000000000004');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b20000-0000-4000-a000-000000000004', 'c0090000-0000-4000-a000-000000000005');

-- --- DEEPENING track: Línea Infraestructura de T.I. ---
-- Gatekeeper: Administración de Infraestructura de TI
INSERT INTO curriculum.sequential_track (id, plan_id, kind, label, consecutive_simultaneity_allowed, version) VALUES
('e0a30000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001',
 'DEEPENING', 'Línea de Profundización: Infraestructura de T.I.', false, 0);

INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b30000-0000-4000-a000-000000000001', 'e0a30000-0000-4000-a000-000000000001', 1);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b30000-0000-4000-a000-000000000002', 'e0a30000-0000-4000-a000-000000000001', 2);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b30000-0000-4000-a000-000000000003', 'e0a30000-0000-4000-a000-000000000001', 3);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b30000-0000-4000-a000-000000000004', 'e0a30000-0000-4000-a000-000000000001', 4);

INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b30000-0000-4000-a000-000000000001', 'c0080000-0000-4000-a000-000000000004');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b30000-0000-4000-a000-000000000002', 'c0080000-0000-4000-a000-000000000005');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b30000-0000-4000-a000-000000000003', 'c0090000-0000-4000-a000-000000000004');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b30000-0000-4000-a000-000000000004', 'c0090000-0000-4000-a000-000000000005');

-- --- DEEPENING track: Línea Gestión de la Información y el Conocimiento ---
-- Gatekeeper: Bases de Datos I
INSERT INTO curriculum.sequential_track (id, plan_id, kind, label, consecutive_simultaneity_allowed, version) VALUES
('e0a40000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001',
 'DEEPENING', 'Línea de Profundización: Gestión de la Información y el Conocimiento', false, 0);

INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b40000-0000-4000-a000-000000000001', 'e0a40000-0000-4000-a000-000000000001', 1);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b40000-0000-4000-a000-000000000002', 'e0a40000-0000-4000-a000-000000000001', 2);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b40000-0000-4000-a000-000000000003', 'e0a40000-0000-4000-a000-000000000001', 3);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f0b40000-0000-4000-a000-000000000004', 'e0a40000-0000-4000-a000-000000000001', 4);

INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b40000-0000-4000-a000-000000000001', 'c0080000-0000-4000-a000-000000000004');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b40000-0000-4000-a000-000000000002', 'c0080000-0000-4000-a000-000000000005');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b40000-0000-4000-a000-000000000003', 'c0090000-0000-4000-a000-000000000004');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f0b40000-0000-4000-a000-000000000004', 'c0090000-0000-4000-a000-000000000005');

-- ============================================================
-- 9. Degree work modalities
-- ============================================================
INSERT INTO curriculum.degree_work_modality (id, plan_id, name, credits, curricular_weekly_hours, status, version) VALUES
('40100000-0000-4000-a000-000000000001', '33333333-0000-4000-a000-000000000001', 'aplicación',   6, 4, 'ACTIVE', 0);

INSERT INTO curriculum.degree_work_modality (id, plan_id, name, credits, curricular_weekly_hours, status, version) VALUES
('40100000-0000-4000-a000-000000000002', '33333333-0000-4000-a000-000000000001', 'investigación', 6, 4, 'ACTIVE', 0);

INSERT INTO curriculum.degree_work_modality (id, plan_id, name, credits, curricular_weekly_hours, status, version) VALUES
('40100000-0000-4000-a000-000000000003', '33333333-0000-4000-a000-000000000001', 'pasantía',      6, 4, 'ACTIVE', 0);
