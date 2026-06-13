-- V013: Seed Licenciatura en Lenguas Modernas (LM)
--
-- !! PROVISIONAL / NON-OFFICIAL !!
-- Source: Plan rediseño curricular 2016, Acuerdo C. Superior 042/2016, 168 cr, 55 courses, 10 sem DIURNA.
-- NOCTURNA 12-semester distribution is INFERRED (topological re-binning, human-approved).
-- ALL prerequisite edges: inferred=true (no official prereq matrix — Acuerdo 042 does not publish one).
-- Confidence: ALTA → HIGH, MEDIA → MEDIUM, BAJA → LOW (per prerrequisitos-inferidos.md).
--
-- Satisfies: Program catalog and curriculum specs
-- official_code minting: {PROG}-S{diurna_sem}-{SLUG}
--   PROG=LM, SLUG = accent-fold(name) → strip non-alnum → UPPERCASE
--
-- Single transaction (Flyway default for PG DML — no DDL here).

-- ============================================================
-- 1. Academic program
-- ============================================================
INSERT INTO catalog.academic_program (id, name, faculty, status, version)
VALUES (
    '11111111-0000-4000-b000-000000000001',
    'Licenciatura en Lenguas Modernas',
    'Facultad de Educación',
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
SELECT '11111111-0000-4000-b000-000000000001', id
FROM   catalog.journey
WHERE  code = 'DIURNA';

INSERT INTO catalog.program_journey (program_id, journey_id)
SELECT '11111111-0000-4000-b000-000000000001', id
FROM   catalog.journey
WHERE  code = 'NOCTURNA';

-- ============================================================
-- 4. Study plan
-- ============================================================
INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, reference_year, version)
VALUES (
    '33333333-0000-4000-b000-000000000001',
    '11111111-0000-4000-b000-000000000001',
    'Plan Rediseño Curricular 2016 (Acuerdo C. Superior 042/2016)',
    'ACTIVE',
    168,
    2016,
    0
);

-- ============================================================
-- 5. Courses (55 total)
--    FK: plan_id = '33333333-0000-4000-b000-000000000001'
--    FK: program_id = '11111111-0000-4000-b000-000000000001'
--    Types: REQUIRED / PERSONAL_ELECTIVE / DEEPENING / INSTITUTIONAL
--    official_code: LM-S{diurna_sem}-{SLUG}
--    Note: weekly hours not published in Acuerdo 042; using 0 as placeholder.
-- ============================================================

-- --- Semester I (6 courses, 14 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0010000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S1-COMPRENSIONYPRODUCCIONORALENINGLESI', 'Comprensión y Producción Oral en Inglés I', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0010000-0000-4000-b000-000000000002', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S1-COMPRENSIONYPRODUCCIONESCRITAENINGLESI', 'Comprensión y Producción Escrita en Inglés I', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0010000-0000-4000-b000-000000000003', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S1-LECTURAYESCRITURAENCONTEXTOSACADEMICOSI', 'Lectura y Escritura en Contextos Académicos I', 'REQUIRED', 2, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0010000-0000-4000-b000-000000000004', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S1-UNIQUINDIANIDAD', 'Uniquindianidad', 'INSTITUTIONAL', 2, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0010000-0000-4000-b000-000000000005', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S1-PENSAMIENTOLOGICOMATEMATICO', 'Pensamiento Lógico Matemático', 'REQUIRED', 2, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0010000-0000-4000-b000-000000000006', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S1-ACTIVIDADESDEFORMACIONPERSONALELECTIVAI', 'Actividades de Formación Personal (Electiva I)', 'PERSONAL_ELECTIVE', 2, 0, 'ACTIVE', 0);

-- --- Semester II (5 courses, 13 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0020000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S2-COMPRENSIONYPRODUCCIONORALENINGLESII', 'Comprensión y Producción Oral en Inglés II', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0020000-0000-4000-b000-000000000002', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S2-COMPRENSIONYPRODUCCIONESCRITAENINGLESII', 'Comprensión y Producción Escrita en Inglés II', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0020000-0000-4000-b000-000000000003', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S2-LECTURAYESCRITURAENCONTEXTOSACADEMICOSII', 'Lectura y Escritura en Contextos Académicos II', 'REQUIRED', 2, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0020000-0000-4000-b000-000000000004', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S2-ESCUELAINCLUSIVAYCONTEXTOSOCIOCULTURALI', 'Escuela Inclusiva y Contexto Sociocultural I', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0020000-0000-4000-b000-000000000005', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S2-ACTIVIDADESDEFORMACIONPERSONALELECTIVAII', 'Actividades de Formación Personal (Electiva II)', 'PERSONAL_ELECTIVE', 2, 0, 'ACTIVE', 0);

-- --- Semester III (5 courses, 16 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0030000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S3-COMPRENSIONYPRODUCCIONORALENINGLESIII', 'Comprensión y Producción Oral en Inglés III', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0030000-0000-4000-b000-000000000002', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S3-COMPRENSIONYPRODUCCIONESCRITAENINGLESIII', 'Comprensión y Producción Escrita en Inglés III', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0030000-0000-4000-b000-000000000003', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S3-ACTIVIDADESDEFORMACIONPERSONALELECTIVAIII', 'Actividades de Formación Personal (Electiva III)', 'PERSONAL_ELECTIVE', 2, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0030000-0000-4000-b000-000000000004', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S3-PERSPECTIVASPEDAGOGICASYCURRICULARESI', 'Perspectivas Pedagógicas y Curriculares I', 'REQUIRED', 4, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0030000-0000-4000-b000-000000000005', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S3-HABILIDADESCOMUNICATIVASEINTEGRADASENFRANCESI', 'Habilidades Comunicativas e Integradas en Francés I', 'REQUIRED', 4, 0, 'ACTIVE', 0);

-- --- Semester IV (4 courses, 14 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0040000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S4-COMPRENSIONYPRODUCCIONORALENINGLESIV', 'Comprensión y Producción Oral en Inglés IV', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0040000-0000-4000-b000-000000000002', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S4-COMPRENSIONYPRODUCCIONESCRITAENINGLESIV', 'Comprensión y Producción Escrita en Inglés IV', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0040000-0000-4000-b000-000000000003', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S4-PROCESOSSOCIOCOGNITIVOSDELAPRENDIZAJEI', 'Procesos Sociocognitivos del Aprendizaje I', 'REQUIRED', 4, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0040000-0000-4000-b000-000000000004', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S4-HABILIDADESCOMUNICATIVASEINTEGRADASENFRANCESII', 'Habilidades Comunicativas e Integradas en Francés II', 'REQUIRED', 4, 0, 'ACTIVE', 0);

-- --- Semester V (5 courses, 17 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0050000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S5-COMPRENSIONYPRODUCCIONORALENINGLESV', 'Comprensión y Producción Oral en Inglés V', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0050000-0000-4000-b000-000000000002', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S5-COMPRENSIONYPRODUCCIONESCRITAENINGLESV', 'Comprensión y Producción Escrita en Inglés V', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0050000-0000-4000-b000-000000000003', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S5-HABILIDADESCOMUNICATIVASEINTEGRADASENFRANCESIII', 'Habilidades Comunicativas e Integradas en Francés III', 'REQUIRED', 4, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0050000-0000-4000-b000-000000000004', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S5-LINGUISTICAI', 'Lingüística I', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0050000-0000-4000-b000-000000000005', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S5-ESCUELAINCLUSIVAYCONTEXTOSOCIOCULTURALII', 'Escuela Inclusiva y Contexto Sociocultural II', 'REQUIRED', 4, 0, 'ACTIVE', 0);

-- --- Semester VI (6 courses, 19 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0060000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S6-COMPRENSIONYPRODUCCIONORALENINGLESVI', 'Comprensión y Producción Oral en Inglés VI', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0060000-0000-4000-b000-000000000002', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S6-COMPRENSIONYPRODUCCIONESCRITAENINGLESVI', 'Comprensión y Producción Escrita en Inglés VI', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0060000-0000-4000-b000-000000000003', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S6-HABILIDADESCOMUNICATIVASEINTEGRADASENFRANCESIV', 'Habilidades Comunicativas e Integradas en Francés IV', 'REQUIRED', 4, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0060000-0000-4000-b000-000000000004', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S6-LECTURAYESCRITURAENCONTEXTOSINVESTIGATIVOS', 'Lectura y Escritura en Contextos Investigativos', 'REQUIRED', 2, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0060000-0000-4000-b000-000000000005', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S6-PERSPECTIVASPEDAGOGICASYCURRICULARESII', 'Perspectivas Pedagógicas y Curriculares II', 'REQUIRED', 4, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0060000-0000-4000-b000-000000000006', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S6-LINGUISTICAII', 'Lingüística II', 'REQUIRED', 3, 0, 'ACTIVE', 0);

-- --- Semester VII (6 courses, 19 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0070000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S7-PROCESOSSOCIOCOGNITIVOSDELAPRENDIZAJEII', 'Procesos Sociocognitivos del Aprendizaje II', 'REQUIRED', 4, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0070000-0000-4000-b000-000000000002', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S7-DIDACTICADELALENGUAEXTRANJERAI', 'Didáctica de la Lengua Extranjera I', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0070000-0000-4000-b000-000000000003', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S7-SOCIOLINGUISTICA', 'Sociolingüística', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0070000-0000-4000-b000-000000000004', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S7-TIPOLOGIATEXTUALDESCRIPTIVAYNARRATIVAENFRANCES', 'Tipología Textual, Descriptiva y Narrativa en Francés', 'REQUIRED', 4, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0070000-0000-4000-b000-000000000005', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S7-LECTURACRITICAYESCRITURAENCONTEXTOSACADEMICOSENINGLES', 'Lectura Crítica y Escritura en Contextos Académicos en Inglés', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0070000-0000-4000-b000-000000000006', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S7-ETICAPROFESIONAL', 'Ética Profesional', 'REQUIRED', 2, 0, 'ACTIVE', 0);

-- --- Semester VIII (7 courses, 22 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0080000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S8-PRACTICAPEDAGOGICA', 'Práctica Pedagógica', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0080000-0000-4000-b000-000000000002', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S8-DIDACTICADELALENGUAEXTRANJERAII', 'Didáctica de la Lengua Extranjera II', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0080000-0000-4000-b000-000000000003', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S8-SEMINARIODEFORMACIONPARALAINVESTIGACIONENLENGUAEXTRANJERAI', 'Seminario de Formación para la Investigación en Lengua Extranjera I', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0080000-0000-4000-b000-000000000004', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S8-PSICOLINGUISTICA', 'Psicolingüística', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0080000-0000-4000-b000-000000000005', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S8-TIPOLOGIATEXTUALARGUMENTATIVAENFRANCES', 'Tipología Textual, Argumentativa en Francés', 'REQUIRED', 4, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0080000-0000-4000-b000-000000000006', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S8-TEORIALITERARIA', 'Teoría Literaria', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0080000-0000-4000-b000-000000000007', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S8-ELECTIVAPROFESIONALI', 'Electiva Profesional I', 'DEEPENING', 3, 0, 'ACTIVE', 0);

-- --- Semester IX (6 courses, 19 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0090000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S9-INTERCULTURALIDADYFRANCOFONIA', 'Interculturalidad y Francofonía', 'REQUIRED', 4, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0090000-0000-4000-b000-000000000002', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S9-SEMINARIODEFUNDAMENTACIONYCONSOLIDACIONPARALAINVESTIGACIONENLENGUAEXTRANJERA', 'Seminario de Fundamentación y Consolidación para la Investigación en Lengua Extranjera', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0090000-0000-4000-b000-000000000003', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S9-DIDACTICADELALENGUAEXTRANJERAIII', 'Didáctica de la Lengua Extranjera III', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0090000-0000-4000-b000-000000000004', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S9-PRACTICAPROFESIONALI', 'Práctica Profesional I', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0090000-0000-4000-b000-000000000005', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S9-LITERATURAANGLOFONA', 'Literatura Anglófona', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0090000-0000-4000-b000-000000000006', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S9-ELECTIVAPROFESIONALII', 'Electiva Profesional II', 'DEEPENING', 3, 0, 'ACTIVE', 0);

-- --- Semester X (5 courses, 15 cr) ---

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0100000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S10-LINGUISTICATEXTUAL', 'Lingüística Textual', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0100000-0000-4000-b000-000000000002', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S10-LITERATURAFRANCOFONA', 'Literatura Francófona', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0100000-0000-4000-b000-000000000003', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S10-PRACTICAPROFESIONALII', 'Práctica Profesional II', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0100000-0000-4000-b000-000000000004', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S10-SEMINARIODEEJECUCIONPARALAINVESTIGACIONENLENGUAEXTRANJERA', 'Seminario de Ejecución para la Investigación en Lengua Extranjera', 'REQUIRED', 3, 0, 'ACTIVE', 0);

INSERT INTO curriculum.course (id, plan_id, program_id, official_code, name, type, credits, curricular_weekly_hours, status, version) VALUES
('a0100000-0000-4000-b000-000000000005', '33333333-0000-4000-b000-000000000001', '11111111-0000-4000-b000-000000000001',
 'LM-S10-ELECTIVAPROFESIONALIII', 'Electiva Profesional III', 'DEEPENING', 3, 0, 'ACTIVE', 0);

-- ============================================================
-- 6. Course recommended semesters
--    DIURNA: official semesters from Acuerdo 042 malla
--    NOCTURNA: approved inferred 12-semester distribution
--    Topological order preserved: no course placed before its prerequisite
-- ============================================================

-- Semester I: DIURNA=1, NOCTURNA=1 (all no-prereq courses)
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0010000-0000-4000-b000-000000000001', 'DIURNA', 1),
('a0010000-0000-4000-b000-000000000002', 'DIURNA', 1),
('a0010000-0000-4000-b000-000000000003', 'DIURNA', 1),
('a0010000-0000-4000-b000-000000000004', 'DIURNA', 1),
('a0010000-0000-4000-b000-000000000005', 'DIURNA', 1),
('a0010000-0000-4000-b000-000000000006', 'DIURNA', 1);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0010000-0000-4000-b000-000000000001', 'NOCTURNA', 1),
('a0010000-0000-4000-b000-000000000002', 'NOCTURNA', 1),
('a0010000-0000-4000-b000-000000000003', 'NOCTURNA', 1),
('a0010000-0000-4000-b000-000000000004', 'NOCTURNA', 1),
('a0010000-0000-4000-b000-000000000005', 'NOCTURNA', 1),
('a0010000-0000-4000-b000-000000000006', 'NOCTURNA', 1);

-- Semester II: DIURNA=2, NOCTURNA=2
-- Prereqs: Oral II←Oral I(N1), Escrita II←Escrita I(N1), LECA II←LECA I(N1) → all ≤ N2 ✓
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0020000-0000-4000-b000-000000000001', 'DIURNA', 2),
('a0020000-0000-4000-b000-000000000002', 'DIURNA', 2),
('a0020000-0000-4000-b000-000000000003', 'DIURNA', 2),
('a0020000-0000-4000-b000-000000000004', 'DIURNA', 2),
('a0020000-0000-4000-b000-000000000005', 'DIURNA', 2);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0020000-0000-4000-b000-000000000001', 'NOCTURNA', 2),
('a0020000-0000-4000-b000-000000000002', 'NOCTURNA', 2),
('a0020000-0000-4000-b000-000000000003', 'NOCTURNA', 2),
-- Escuela Inclusiva I has no prereq: N2 ✓
('a0020000-0000-4000-b000-000000000004', 'NOCTURNA', 2),
('a0020000-0000-4000-b000-000000000005', 'NOCTURNA', 2);

-- Semester III: DIURNA=3, NOCTURNA=3
-- Prereqs: Oral III←Oral II(N2), Escrita III←Escrita II(N2) → ≤ N3 ✓
-- Francés I, Perspectivas I, AFP III: no prereq → N3 ✓
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0030000-0000-4000-b000-000000000001', 'DIURNA', 3),
('a0030000-0000-4000-b000-000000000002', 'DIURNA', 3),
('a0030000-0000-4000-b000-000000000003', 'DIURNA', 3),
('a0030000-0000-4000-b000-000000000004', 'DIURNA', 3),
('a0030000-0000-4000-b000-000000000005', 'DIURNA', 3);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0030000-0000-4000-b000-000000000001', 'NOCTURNA', 3),
('a0030000-0000-4000-b000-000000000002', 'NOCTURNA', 3),
('a0030000-0000-4000-b000-000000000003', 'NOCTURNA', 3),
('a0030000-0000-4000-b000-000000000004', 'NOCTURNA', 3),
('a0030000-0000-4000-b000-000000000005', 'NOCTURNA', 3);

-- Semester IV: DIURNA=4, NOCTURNA=4
-- Prereqs: Oral IV←Oral III(N3), Escrita IV←Escrita III(N3), Francés II←Francés I(N3) → ≤ N4 ✓
-- Procesos Sociocog I: no prereq → N4 ✓
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0040000-0000-4000-b000-000000000001', 'DIURNA', 4),
('a0040000-0000-4000-b000-000000000002', 'DIURNA', 4),
('a0040000-0000-4000-b000-000000000003', 'DIURNA', 4),
('a0040000-0000-4000-b000-000000000004', 'DIURNA', 4);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0040000-0000-4000-b000-000000000001', 'NOCTURNA', 4),
('a0040000-0000-4000-b000-000000000002', 'NOCTURNA', 4),
('a0040000-0000-4000-b000-000000000003', 'NOCTURNA', 4),
('a0040000-0000-4000-b000-000000000004', 'NOCTURNA', 4);

-- Semester V: DIURNA=5, NOCTURNA=5
-- Prereqs: Oral V←Oral IV(N4), Escrita V←Escrita IV(N4), Francés III←Francés II(N4),
--          Escuela Inc II←Escuela Inc I(N2) → ≤ N5 ✓; Lingüística I: no prereq → N5 ✓
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0050000-0000-4000-b000-000000000001', 'DIURNA', 5),
('a0050000-0000-4000-b000-000000000002', 'DIURNA', 5),
('a0050000-0000-4000-b000-000000000003', 'DIURNA', 5),
('a0050000-0000-4000-b000-000000000004', 'DIURNA', 5),
('a0050000-0000-4000-b000-000000000005', 'DIURNA', 5);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0050000-0000-4000-b000-000000000001', 'NOCTURNA', 5),
('a0050000-0000-4000-b000-000000000002', 'NOCTURNA', 5),
('a0050000-0000-4000-b000-000000000003', 'NOCTURNA', 5),
('a0050000-0000-4000-b000-000000000004', 'NOCTURNA', 5),
('a0050000-0000-4000-b000-000000000005', 'NOCTURNA', 5);

-- Semester VI: DIURNA=6, NOCTURNA=6
-- Prereqs: Oral VI←Oral V(N5), Escrita VI←Escrita V(N5), Francés IV←Francés III(N5),
--          Ling II←Ling I(N5), Perspectivas II←Perspectivas I(N3),
--          Contextos Inv←LECA II(N2) → all ≤ N6 ✓
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0060000-0000-4000-b000-000000000001', 'DIURNA', 6),
('a0060000-0000-4000-b000-000000000002', 'DIURNA', 6),
('a0060000-0000-4000-b000-000000000003', 'DIURNA', 6),
('a0060000-0000-4000-b000-000000000004', 'DIURNA', 6),
('a0060000-0000-4000-b000-000000000005', 'DIURNA', 6),
('a0060000-0000-4000-b000-000000000006', 'DIURNA', 6);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0060000-0000-4000-b000-000000000001', 'NOCTURNA', 6),
('a0060000-0000-4000-b000-000000000002', 'NOCTURNA', 6),
('a0060000-0000-4000-b000-000000000003', 'NOCTURNA', 6),
('a0060000-0000-4000-b000-000000000004', 'NOCTURNA', 6),
('a0060000-0000-4000-b000-000000000005', 'NOCTURNA', 6),
('a0060000-0000-4000-b000-000000000006', 'NOCTURNA', 6);

-- Semester VII: DIURNA=7
-- NOCTURNA split: Procesos Sociocog II, DLE I, Sociolingüística, Ética → N7
--                 Tipología D/N, Lect.Crítica Inglés → N8
-- Prereqs N7: ProcSoc II←ProcSoc I(N4)≤7✓, DLE I←Perspectivas II(N6)≤7✓,
--             Socioling←Ling II(N6)≤7✓, Ética: no prereq ✓
-- Prereqs N8: Tipología D/N←Francés IV(N6)≤8✓, Lect.Crítica←Oral VI(N6)+Escrita VI(N6)≤8✓
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0070000-0000-4000-b000-000000000001', 'DIURNA', 7),
('a0070000-0000-4000-b000-000000000002', 'DIURNA', 7),
('a0070000-0000-4000-b000-000000000003', 'DIURNA', 7),
('a0070000-0000-4000-b000-000000000004', 'DIURNA', 7),
('a0070000-0000-4000-b000-000000000005', 'DIURNA', 7),
('a0070000-0000-4000-b000-000000000006', 'DIURNA', 7);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
-- Procesos Sociocog II: prereq=ProcSoc I(N4) → N7 ✓
('a0070000-0000-4000-b000-000000000001', 'NOCTURNA', 7),
-- DLE I: prereq=Perspectivas II(N6) → N7 ✓
('a0070000-0000-4000-b000-000000000002', 'NOCTURNA', 7),
-- Sociolingüística: prereq=Ling II(N6) → N7 ✓
('a0070000-0000-4000-b000-000000000003', 'NOCTURNA', 7),
-- Tipología D/N: prereq=Francés IV(N6) → N8 ✓
('a0070000-0000-4000-b000-000000000004', 'NOCTURNA', 8),
-- Lect.Crítica Inglés: prereqs=Oral VI(N6)+Escrita VI(N6) → N8 ✓
('a0070000-0000-4000-b000-000000000005', 'NOCTURNA', 8),
-- Ética Profesional: no prereq → N7 ✓
('a0070000-0000-4000-b000-000000000006', 'NOCTURNA', 7);

-- Semester VIII: DIURNA=8
-- NOCTURNA split: Práctica Ped, DLE II → N8 (prereqs DLE I(N7)≤8✓)
--                 Sem.Form I, Psicoling, Tipología Arg, T.Literaria, EP I → N9
-- Prereqs N8: Práctica Ped←DLE I(N7)≤8✓, DLE II←DLE I(N7)≤8✓
-- Prereqs N9: Sem.Form I←Contextos Inv(N6)≤9✓, Psicoling←Ling II(N6)≤9✓,
--             Tipología Arg←Tipología D/N(N8)≤9✓, T.Literaria: no prereq ✓, EP I: no prereq ✓
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0080000-0000-4000-b000-000000000001', 'DIURNA', 8),
('a0080000-0000-4000-b000-000000000002', 'DIURNA', 8),
('a0080000-0000-4000-b000-000000000003', 'DIURNA', 8),
('a0080000-0000-4000-b000-000000000004', 'DIURNA', 8),
('a0080000-0000-4000-b000-000000000005', 'DIURNA', 8),
('a0080000-0000-4000-b000-000000000006', 'DIURNA', 8),
('a0080000-0000-4000-b000-000000000007', 'DIURNA', 8);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
-- Práctica Pedagógica: prereq=DLE I(N7) → N8 ✓
('a0080000-0000-4000-b000-000000000001', 'NOCTURNA', 8),
-- DLE II: prereq=DLE I(N7) → N8 ✓
('a0080000-0000-4000-b000-000000000002', 'NOCTURNA', 8),
-- Sem.Form I: prereq=Contextos Inv(N6) → N9 ✓
('a0080000-0000-4000-b000-000000000003', 'NOCTURNA', 9),
-- Psicolingüística: prereq=Ling II(N6) → N9 ✓
('a0080000-0000-4000-b000-000000000004', 'NOCTURNA', 9),
-- Tipología Arg: prereq=Tipología D/N(N8) → N9 ✓
('a0080000-0000-4000-b000-000000000005', 'NOCTURNA', 9),
-- Teoría Literaria: no prereq → N9 ✓
('a0080000-0000-4000-b000-000000000006', 'NOCTURNA', 9),
-- EP I: no prereq → N9 ✓
('a0080000-0000-4000-b000-000000000007', 'NOCTURNA', 9);

-- Semester IX: DIURNA=9
-- NOCTURNA split: Interculturalidad, Sem.Fund., DLE III, Práct Prof I → N10
--                 Lit.Anglófona, EP II → N11
-- Prereqs N10: Interculturalidad←Tipología Arg(N9)≤10✓, Sem.Fund←Sem.Form I(N9)≤10✓,
--              DLE III←DLE II(N8)≤10✓, Práct Prof I←Práctica Ped(N8)≤10✓
-- Prereqs N11: Lit.Anglófona←T.Literaria(N9)+Lect.Crítica(N8)≤11✓, EP II←EP I(N9)≤11✓
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0090000-0000-4000-b000-000000000001', 'DIURNA', 9),
('a0090000-0000-4000-b000-000000000002', 'DIURNA', 9),
('a0090000-0000-4000-b000-000000000003', 'DIURNA', 9),
('a0090000-0000-4000-b000-000000000004', 'DIURNA', 9),
('a0090000-0000-4000-b000-000000000005', 'DIURNA', 9),
('a0090000-0000-4000-b000-000000000006', 'DIURNA', 9);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
-- Interculturalidad: prereq=Tipología Arg(N9) → N10 ✓
('a0090000-0000-4000-b000-000000000001', 'NOCTURNA', 10),
-- Sem.Fundamentación: prereq=Sem.Form I(N9) → N10 ✓
('a0090000-0000-4000-b000-000000000002', 'NOCTURNA', 10),
-- DLE III: prereq=DLE II(N8) → N10 ✓
('a0090000-0000-4000-b000-000000000003', 'NOCTURNA', 10),
-- Práct Prof I: prereq=Práctica Ped(N8) → N10 ✓
('a0090000-0000-4000-b000-000000000004', 'NOCTURNA', 10),
-- Lit.Anglófona: prereqs=T.Literaria(N9)+Lect.Crítica(N8) → N11 ✓
('a0090000-0000-4000-b000-000000000005', 'NOCTURNA', 11),
-- EP II: prereq=EP I(N9) → N11 ✓
('a0090000-0000-4000-b000-000000000006', 'NOCTURNA', 11);

-- Semester X: DIURNA=10
-- NOCTURNA split: Ling.Textual, Lit.Francófona → N11
--                 Práct Prof II, Sem.Ejecución, EP III → N12
-- Prereqs N11: Ling.Textual←Ling II(N6)≤11✓, Lit.Francófona←T.Literaria(N9)+Interculturalidad(N10)≤11✓
-- Prereqs N12: Práct Prof II←Práct Prof I(N10)≤12✓, Sem.Ejecución←Sem.Fund(N10)≤12✓,
--              EP III←EP II(N11)≤12✓
INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
('a0100000-0000-4000-b000-000000000001', 'DIURNA', 10),
('a0100000-0000-4000-b000-000000000002', 'DIURNA', 10),
('a0100000-0000-4000-b000-000000000003', 'DIURNA', 10),
('a0100000-0000-4000-b000-000000000004', 'DIURNA', 10),
('a0100000-0000-4000-b000-000000000005', 'DIURNA', 10);

INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES
-- Ling.Textual: prereq=Ling II(N6) → N11 ✓
('a0100000-0000-4000-b000-000000000001', 'NOCTURNA', 11),
-- Lit.Francófona: prereqs=T.Literaria(N9)+Interculturalidad(N10) → N11 ✓
('a0100000-0000-4000-b000-000000000002', 'NOCTURNA', 11),
-- Práct Prof II: prereq=Práct Prof I(N10) → N12 ✓
('a0100000-0000-4000-b000-000000000003', 'NOCTURNA', 12),
-- Sem.Ejecución: prereq=Sem.Fundamentación(N10) → N12 ✓
('a0100000-0000-4000-b000-000000000004', 'NOCTURNA', 12),
-- EP III: prereq=EP II(N11) → N12 ✓
('a0100000-0000-4000-b000-000000000005', 'NOCTURNA', 12);

-- ============================================================
-- 7. Course requirements (prerequisite edges)
--    Source: prerrequisitos-inferidos.md
--    ALL edges: inferred=true (no official prereq matrix for Lenguas)
--    ALTA → confidence=HIGH, MEDIA → MEDIUM, BAJA → LOW
-- ============================================================

-- Oral Inglés II ← Oral Inglés I (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0020000-0000-4000-b000-000000000001', 'COURSE_PASSED', 'a0010000-0000-4000-b000-000000000001', 'HIGH', true, 'ACTIVE', 0);

-- Escrita Inglés II ← Escrita Inglés I (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0020000-0000-4000-b000-000000000002', 'COURSE_PASSED', 'a0010000-0000-4000-b000-000000000002', 'HIGH', true, 'ACTIVE', 0);

-- LECA II ← LECA I (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0020000-0000-4000-b000-000000000003', 'COURSE_PASSED', 'a0010000-0000-4000-b000-000000000003', 'HIGH', true, 'ACTIVE', 0);

-- Oral Inglés III ← Oral Inglés II (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0030000-0000-4000-b000-000000000001', 'COURSE_PASSED', 'a0020000-0000-4000-b000-000000000001', 'HIGH', true, 'ACTIVE', 0);

-- Escrita Inglés III ← Escrita Inglés II (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0030000-0000-4000-b000-000000000002', 'COURSE_PASSED', 'a0020000-0000-4000-b000-000000000002', 'HIGH', true, 'ACTIVE', 0);

-- Oral Inglés IV ← Oral Inglés III (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0040000-0000-4000-b000-000000000001', 'COURSE_PASSED', 'a0030000-0000-4000-b000-000000000001', 'HIGH', true, 'ACTIVE', 0);

-- Escrita Inglés IV ← Escrita Inglés III (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0040000-0000-4000-b000-000000000002', 'COURSE_PASSED', 'a0030000-0000-4000-b000-000000000002', 'HIGH', true, 'ACTIVE', 0);

-- Francés II ← Francés I (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0040000-0000-4000-b000-000000000004', 'COURSE_PASSED', 'a0030000-0000-4000-b000-000000000005', 'HIGH', true, 'ACTIVE', 0);

-- Oral Inglés V ← Oral Inglés IV (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0050000-0000-4000-b000-000000000001', 'COURSE_PASSED', 'a0040000-0000-4000-b000-000000000001', 'HIGH', true, 'ACTIVE', 0);

-- Escrita Inglés V ← Escrita Inglés IV (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0050000-0000-4000-b000-000000000002', 'COURSE_PASSED', 'a0040000-0000-4000-b000-000000000002', 'HIGH', true, 'ACTIVE', 0);

-- Francés III ← Francés II (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0050000-0000-4000-b000-000000000003', 'COURSE_PASSED', 'a0040000-0000-4000-b000-000000000004', 'HIGH', true, 'ACTIVE', 0);

-- Escuela Inclusiva II ← Escuela Inclusiva I (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0050000-0000-4000-b000-000000000005', 'COURSE_PASSED', 'a0020000-0000-4000-b000-000000000004', 'HIGH', true, 'ACTIVE', 0);

-- Oral Inglés VI ← Oral Inglés V (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0060000-0000-4000-b000-000000000001', 'COURSE_PASSED', 'a0050000-0000-4000-b000-000000000001', 'HIGH', true, 'ACTIVE', 0);

-- Escrita Inglés VI ← Escrita Inglés V (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0060000-0000-4000-b000-000000000002', 'COURSE_PASSED', 'a0050000-0000-4000-b000-000000000002', 'HIGH', true, 'ACTIVE', 0);

-- Francés IV ← Francés III (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0060000-0000-4000-b000-000000000003', 'COURSE_PASSED', 'a0050000-0000-4000-b000-000000000003', 'HIGH', true, 'ACTIVE', 0);

-- Contextos Investigativos ← LECA II (MEDIA → MEDIUM, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0060000-0000-4000-b000-000000000004', 'COURSE_PASSED', 'a0020000-0000-4000-b000-000000000003', 'MEDIUM', true, 'ACTIVE', 0);

-- Perspectivas Pedagógicas II ← Perspectivas Pedagógicas I (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0060000-0000-4000-b000-000000000005', 'COURSE_PASSED', 'a0030000-0000-4000-b000-000000000004', 'HIGH', true, 'ACTIVE', 0);

-- Lingüística II ← Lingüística I (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0060000-0000-4000-b000-000000000006', 'COURSE_PASSED', 'a0050000-0000-4000-b000-000000000004', 'HIGH', true, 'ACTIVE', 0);

-- Procesos Sociocog II ← Procesos Sociocog I (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0070000-0000-4000-b000-000000000001', 'COURSE_PASSED', 'a0040000-0000-4000-b000-000000000003', 'HIGH', true, 'ACTIVE', 0);

-- DLE I ← Perspectivas Pedagógicas II (MEDIA → MEDIUM, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0070000-0000-4000-b000-000000000002', 'COURSE_PASSED', 'a0060000-0000-4000-b000-000000000005', 'MEDIUM', true, 'ACTIVE', 0);

-- Sociolingüística ← Lingüística II (MEDIA → MEDIUM, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0070000-0000-4000-b000-000000000003', 'COURSE_PASSED', 'a0060000-0000-4000-b000-000000000006', 'MEDIUM', true, 'ACTIVE', 0);

-- Tipología D/N en Francés ← Francés IV (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0070000-0000-4000-b000-000000000004', 'COURSE_PASSED', 'a0060000-0000-4000-b000-000000000003', 'HIGH', true, 'ACTIVE', 0);

-- Lect.Crítica Inglés ← Oral Inglés VI (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0070000-0000-4000-b000-000000000005', 'COURSE_PASSED', 'a0060000-0000-4000-b000-000000000001', 'HIGH', true, 'ACTIVE', 0);

-- Lect.Crítica Inglés ← Escrita Inglés VI (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0070000-0000-4000-b000-000000000005', 'COURSE_PASSED', 'a0060000-0000-4000-b000-000000000002', 'HIGH', true, 'ACTIVE', 0);

-- Práctica Pedagógica ← DLE I (MEDIA → MEDIUM, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0080000-0000-4000-b000-000000000001', 'COURSE_PASSED', 'a0070000-0000-4000-b000-000000000002', 'MEDIUM', true, 'ACTIVE', 0);

-- DLE II ← DLE I (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0080000-0000-4000-b000-000000000002', 'COURSE_PASSED', 'a0070000-0000-4000-b000-000000000002', 'HIGH', true, 'ACTIVE', 0);

-- Sem.Form I ← Contextos Investigativos (MEDIA → MEDIUM, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0080000-0000-4000-b000-000000000003', 'COURSE_PASSED', 'a0060000-0000-4000-b000-000000000004', 'MEDIUM', true, 'ACTIVE', 0);

-- Psicolingüística ← Lingüística II (MEDIA → MEDIUM, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0080000-0000-4000-b000-000000000004', 'COURSE_PASSED', 'a0060000-0000-4000-b000-000000000006', 'MEDIUM', true, 'ACTIVE', 0);

-- Tipología Arg en Francés ← Tipología D/N (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0080000-0000-4000-b000-000000000005', 'COURSE_PASSED', 'a0070000-0000-4000-b000-000000000004', 'HIGH', true, 'ACTIVE', 0);

-- Interculturalidad y Francofonía ← Tipología Arg (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0090000-0000-4000-b000-000000000001', 'COURSE_PASSED', 'a0080000-0000-4000-b000-000000000005', 'HIGH', true, 'ACTIVE', 0);

-- Sem.Fundamentación ← Sem.Form I (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0090000-0000-4000-b000-000000000002', 'COURSE_PASSED', 'a0080000-0000-4000-b000-000000000003', 'HIGH', true, 'ACTIVE', 0);

-- DLE III ← DLE II (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0090000-0000-4000-b000-000000000003', 'COURSE_PASSED', 'a0080000-0000-4000-b000-000000000002', 'HIGH', true, 'ACTIVE', 0);

-- Práct Prof I ← Práctica Pedagógica (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0090000-0000-4000-b000-000000000004', 'COURSE_PASSED', 'a0080000-0000-4000-b000-000000000001', 'HIGH', true, 'ACTIVE', 0);

-- Literatura Anglófona ← Teoría Literaria (MEDIA → MEDIUM, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0090000-0000-4000-b000-000000000005', 'COURSE_PASSED', 'a0080000-0000-4000-b000-000000000006', 'MEDIUM', true, 'ACTIVE', 0);

-- Literatura Anglófona ← Lect.Crítica Inglés (MEDIA → MEDIUM, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0090000-0000-4000-b000-000000000005', 'COURSE_PASSED', 'a0070000-0000-4000-b000-000000000005', 'MEDIUM', true, 'ACTIVE', 0);

-- EP II ← EP I (BAJA → LOW, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0090000-0000-4000-b000-000000000006', 'COURSE_PASSED', 'a0080000-0000-4000-b000-000000000007', 'LOW', true, 'ACTIVE', 0);

-- Lingüística Textual ← Lingüística II (MEDIA → MEDIUM, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0100000-0000-4000-b000-000000000001', 'COURSE_PASSED', 'a0060000-0000-4000-b000-000000000006', 'MEDIUM', true, 'ACTIVE', 0);

-- Literatura Francófona ← Teoría Literaria (MEDIA → MEDIUM, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0100000-0000-4000-b000-000000000002', 'COURSE_PASSED', 'a0080000-0000-4000-b000-000000000006', 'MEDIUM', true, 'ACTIVE', 0);

-- Literatura Francófona ← Interculturalidad (MEDIA → MEDIUM, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0100000-0000-4000-b000-000000000002', 'COURSE_PASSED', 'a0090000-0000-4000-b000-000000000001', 'MEDIUM', true, 'ACTIVE', 0);

-- Práct Prof II ← Práct Prof I (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0100000-0000-4000-b000-000000000003', 'COURSE_PASSED', 'a0090000-0000-4000-b000-000000000004', 'HIGH', true, 'ACTIVE', 0);

-- Sem.Ejecución ← Sem.Fundamentación (ALTA → HIGH, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0100000-0000-4000-b000-000000000004', 'COURSE_PASSED', 'a0090000-0000-4000-b000-000000000002', 'HIGH', true, 'ACTIVE', 0);

-- EP III ← EP II (BAJA → LOW, inferred=true)
INSERT INTO curriculum.course_requirement (id, target_course_id, type, required_course_id, confidence, inferred, status, version) VALUES
(gen_random_uuid(), 'a0100000-0000-4000-b000-000000000005', 'COURSE_PASSED', 'a0090000-0000-4000-b000-000000000006', 'LOW', true, 'ACTIVE', 0);

-- ============================================================
-- 8. Sequential tracks
-- ============================================================

-- --- PERSONAL_ELECTIVE track: Actividades de Formación Personal ---
-- AFP I (S1), AFP II (S2), AFP III (S3) form a sequential track (levels 1→2→3)
INSERT INTO curriculum.sequential_track (id, plan_id, kind, label, consecutive_simultaneity_allowed, version) VALUES
('e1a10000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001',
 'PERSONAL_ELECTIVE', 'Actividades de Formación Personal', false, 0);

INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f1b10000-0000-4000-b000-000000000001', 'e1a10000-0000-4000-b000-000000000001', 1);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f1b10000-0000-4000-b000-000000000002', 'e1a10000-0000-4000-b000-000000000001', 2);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f1b10000-0000-4000-b000-000000000003', 'e1a10000-0000-4000-b000-000000000001', 3);

INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f1b10000-0000-4000-b000-000000000001', 'a0010000-0000-4000-b000-000000000006');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f1b10000-0000-4000-b000-000000000002', 'a0020000-0000-4000-b000-000000000005');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f1b10000-0000-4000-b000-000000000003', 'a0030000-0000-4000-b000-000000000003');

-- --- DEEPENING track: Electivas Profesionales ---
-- EP I (S8), EP II (S9), EP III (S10) form a sequential deepening track (levels 1→2→3)
INSERT INTO curriculum.sequential_track (id, plan_id, kind, label, consecutive_simultaneity_allowed, version) VALUES
('e1a20000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001',
 'DEEPENING', 'Electivas Profesionales', false, 0);

INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f1b20000-0000-4000-b000-000000000001', 'e1a20000-0000-4000-b000-000000000001', 1);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f1b20000-0000-4000-b000-000000000002', 'e1a20000-0000-4000-b000-000000000001', 2);
INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES
('f1b20000-0000-4000-b000-000000000003', 'e1a20000-0000-4000-b000-000000000001', 3);

INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f1b20000-0000-4000-b000-000000000001', 'a0080000-0000-4000-b000-000000000007');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f1b20000-0000-4000-b000-000000000002', 'a0090000-0000-4000-b000-000000000006');
INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES
('f1b20000-0000-4000-b000-000000000003', 'a0100000-0000-4000-b000-000000000005');

-- ============================================================
-- 9. Degree work modalities
--    Source: PEP LM — work is via the Seminario de Investigación sequence;
--    no formal separate modality list published. Seeding 1 provisional modality.
-- ============================================================
INSERT INTO curriculum.degree_work_modality (id, plan_id, name, credits, curricular_weekly_hours, status, version) VALUES
('40200000-0000-4000-b000-000000000001', '33333333-0000-4000-b000-000000000001', 'seminario de investigación', 9, 0, 'ACTIVE', 0);
