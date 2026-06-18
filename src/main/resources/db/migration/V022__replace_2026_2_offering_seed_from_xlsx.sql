-- V022__replace_2026_2_offering_seed_from_xlsx.sql
-- Forward-only migration. Replaces the V017 test-fixture offering seed for period 2026-2.
-- Source workbook: Validador_Horario_2026-2.xlsx
-- Processed Catálogo Diurna rows: 193; Catálogo Nocturna rows: 160.
-- Inserted scheduled source rows: 337; omitted rows without concrete start/end time: 16.
-- Existing curriculum has generic Profundización slot courses; named deepening offerings are mapped explicitly to those slots by source level.
-- Lenguas rows in workbook sheet "Prueba 2026-2 (2)" are a filled personal schedule, not an offering catalog; V022 preserves the prior validated Lenguas fixture instead of inventing catalog mappings.

-- Remove prior 2026-2 offering rows only; schedule children cascade via FK, but explicit deletes keep intent clear.
DELETE FROM offering.schedule_session ss USING offering.schedule_option so, offering.course_offering co, offering.academic_period ap WHERE ss.schedule_option_id = so.id AND so.offering_id = co.id AND co.period_id = ap.id AND ap.name = '2026-2';
DELETE FROM offering.schedule_option so USING offering.course_offering co, offering.academic_period ap WHERE so.offering_id = co.id AND co.period_id = ap.id AND ap.name = '2026-2';
DELETE FROM offering.course_offering co USING offering.academic_period ap WHERE co.period_id = ap.id AND ap.name = '2026-2';

UPDATE offering.academic_period SET start_date = '2026-07-15', end_date = '2026-11-28', status = 'ACTIVE', version = version + 1 WHERE id = '00000001-0000-4000-a000-000000000001';

-- Real scheduled offerings from the workbook catalogs.

-- DIURNA 01 D · Introducción a la Ingeniería de Sistemas y Computación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200001-0000-4001-a000-000000000001', 'c0010000-0000-4000-a000-000000000002', 'Introducción a la Ingeniería de Sistemas y Computación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; locations/modalities include: Virtual; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200001-0000-4002-a000-000000000001', '02200001-0000-4001-a000-000000000001', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200001-0000-4003-a000-000000000001', '02200001-0000-4002-a000-000000000001', 'MONDAY', '09:00:00', '11:00:00'),
('02200001-0000-4003-a000-000000000002', '02200001-0000-4002-a000-000000000001', 'WEDNESDAY', '11:00:00', '13:00:00');

-- DIURNA 01 D · Lectura y Escritura en Ingeniería
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200002-0000-4001-a000-000000000002', 'c0010000-0000-4000-a000-000000000005', 'Lectura y Escritura en Ingeniería', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200002-0000-4002-a000-000000000002', '02200002-0000-4001-a000-000000000002', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200002-0000-4003-a000-000000000001', '02200002-0000-4002-a000-000000000002', 'MONDAY', '11:00:00', '13:00:00'),
('02200002-0000-4003-a000-000000000002', '02200002-0000-4002-a000-000000000002', 'FRIDAY', '11:00:00', '13:00:00');

-- DIURNA 01 D · Lógica de Programación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200003-0000-4001-a000-000000000003', 'c0010000-0000-4000-a000-000000000004', 'Lógica de Programación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200003-0000-4002-a000-000000000003', '02200003-0000-4001-a000-000000000003', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200003-0000-4003-a000-000000000001', '02200003-0000-4002-a000-000000000003', 'TUESDAY', '09:00:00', '11:00:00'),
('02200003-0000-4003-a000-000000000002', '02200003-0000-4002-a000-000000000003', 'THURSDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Matemáticas Generales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200004-0000-4001-a000-000000000004', 'c0010000-0000-4000-a000-000000000001', 'Matemáticas Generales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200004-0000-4002-a000-000000000004', '02200004-0000-4001-a000-000000000004', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200004-0000-4003-a000-000000000001', '02200004-0000-4002-a000-000000000004', 'MONDAY', '07:00:00', '09:00:00'),
('02200004-0000-4003-a000-000000000002', '02200004-0000-4002-a000-000000000004', 'THURSDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Práctica Lógica de Programación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200005-0000-4001-a000-000000000005', 'c0010000-0000-4000-a000-000000000004', 'Práctica Lógica de Programación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200005-0000-4002-a000-000000000005', '02200005-0000-4001-a000-000000000005', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200005-0000-4003-a000-000000000001', '02200005-0000-4002-a000-000000000005', 'TUESDAY', '11:00:00', '13:00:00');

-- DIURNA 02 D · Introducción a la Ingeniería de Sistemas y Computación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200006-0000-4001-a000-000000000006', 'c0010000-0000-4000-a000-000000000002', 'Introducción a la Ingeniería de Sistemas y Computación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: Virtual; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200006-0000-4002-a000-000000000006', '02200006-0000-4001-a000-000000000006', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200006-0000-4003-a000-000000000001', '02200006-0000-4002-a000-000000000006', 'MONDAY', '11:00:00', '13:00:00'),
('02200006-0000-4003-a000-000000000002', '02200006-0000-4002-a000-000000000006', 'THURSDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Lectura y Escritura en Ingeniería
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200007-0000-4001-a000-000000000007', 'c0010000-0000-4000-a000-000000000005', 'Lectura y Escritura en Ingeniería', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200007-0000-4002-a000-000000000007', '02200007-0000-4001-a000-000000000007', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200007-0000-4003-a000-000000000001', '02200007-0000-4002-a000-000000000007', 'TUESDAY', '11:00:00', '13:00:00'),
('02200007-0000-4003-a000-000000000002', '02200007-0000-4002-a000-000000000007', 'FRIDAY', '11:00:00', '13:00:00');

-- DIURNA 02 D · Lógica de Programación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200008-0000-4001-a000-000000000008', 'c0010000-0000-4000-a000-000000000004', 'Lógica de Programación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200008-0000-4002-a000-000000000008', '02200008-0000-4001-a000-000000000008', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200008-0000-4003-a000-000000000001', '02200008-0000-4002-a000-000000000008', 'MONDAY', '09:00:00', '11:00:00'),
('02200008-0000-4003-a000-000000000002', '02200008-0000-4002-a000-000000000008', 'FRIDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Matemáticas Generales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200009-0000-4001-a000-000000000009', 'c0010000-0000-4000-a000-000000000001', 'Matemáticas Generales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200009-0000-4002-a000-000000000009', '02200009-0000-4001-a000-000000000009', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200009-0000-4003-a000-000000000001', '02200009-0000-4002-a000-000000000009', 'MONDAY', '07:00:00', '09:00:00'),
('02200009-0000-4003-a000-000000000002', '02200009-0000-4002-a000-000000000009', 'WEDNESDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Práctica Lógica de Programación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200010-0000-4001-a000-000000000010', 'c0010000-0000-4000-a000-000000000004', 'Práctica Lógica de Programación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200010-0000-4002-a000-000000000010', '02200010-0000-4001-a000-000000000010', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200010-0000-4003-a000-000000000001', '02200010-0000-4002-a000-000000000010', 'WEDNESDAY', '11:00:00', '13:00:00');

-- DIURNA 03 D · Lógica de Programación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200011-0000-4001-a000-000000000011', 'c0010000-0000-4000-a000-000000000004', 'Lógica de Programación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200011-0000-4002-a000-000000000011', '02200011-0000-4001-a000-000000000011', '03 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200011-0000-4003-a000-000000000001', '02200011-0000-4002-a000-000000000011', 'MONDAY', '09:00:00', '11:00:00'),
('02200011-0000-4003-a000-000000000002', '02200011-0000-4002-a000-000000000011', 'FRIDAY', '09:00:00', '11:00:00');

-- DIURNA 03 D · Matemáticas Generales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200012-0000-4001-a000-000000000012', 'c0010000-0000-4000-a000-000000000001', 'Matemáticas Generales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302; source sheet: Catálogo Diurna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200012-0000-4002-a000-000000000012', '02200012-0000-4001-a000-000000000012', '03 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200012-0000-4003-a000-000000000001', '02200012-0000-4002-a000-000000000012', 'TUESDAY', '07:00:00', '09:00:00'),
('02200012-0000-4003-a000-000000000002', '02200012-0000-4002-a000-000000000012', 'THURSDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Álgebra Lineal
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200013-0000-4001-a000-000000000013', 'c0020000-0000-4000-a000-000000000002', 'Álgebra Lineal', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200013-0000-4002-a000-000000000013', '02200013-0000-4001-a000-000000000013', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200013-0000-4003-a000-000000000001', '02200013-0000-4002-a000-000000000013', 'MONDAY', '16:00:00', '18:00:00'),
('02200013-0000-4003-a000-000000000002', '02200013-0000-4002-a000-000000000013', 'WEDNESDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Cálculo Diferencial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200014-0000-4001-a000-000000000014', 'c0020000-0000-4000-a000-000000000001', 'Cálculo Diferencial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200014-0000-4002-a000-000000000014', '02200014-0000-4001-a000-000000000014', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200014-0000-4003-a000-000000000001', '02200014-0000-4002-a000-000000000014', 'MONDAY', '14:00:00', '16:00:00'),
('02200014-0000-4003-a000-000000000002', '02200014-0000-4002-a000-000000000014', 'THURSDAY', '16:00:00', '18:00:00'),
('02200014-0000-4003-a000-000000000003', '02200014-0000-4002-a000-000000000014', 'FRIDAY', '16:00:00', '18:00:00');

-- DIURNA 01 D · Fundamentos de Electricidad, Electrónica y Telecomunicaciones
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200015-0000-4001-a000-000000000015', 'c0020000-0000-4000-a000-000000000005', 'Fundamentos de Electricidad, Electrónica y Telecomunicaciones', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-401; locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200015-0000-4002-a000-000000000015', '02200015-0000-4001-a000-000000000015', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200015-0000-4003-a000-000000000001', '02200015-0000-4002-a000-000000000015', 'WEDNESDAY', '16:00:00', '18:00:00'),
('02200015-0000-4003-a000-000000000002', '02200015-0000-4002-a000-000000000015', 'FRIDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Pensamiento Sistémico
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200016-0000-4001-a000-000000000016', 'c0020000-0000-4000-a000-000000000004', 'Pensamiento Sistémico', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; source sheet: Catálogo Diurna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200016-0000-4002-a000-000000000016', '02200016-0000-4001-a000-000000000016', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200016-0000-4003-a000-000000000001', '02200016-0000-4002-a000-000000000016', 'TUESDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Programación I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200017-0000-4001-a000-000000000017', 'c0020000-0000-4000-a000-000000000003', 'Programación I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; source sheet: Catálogo Diurna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200017-0000-4002-a000-000000000017', '02200017-0000-4001-a000-000000000017', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200017-0000-4003-a000-000000000001', '02200017-0000-4002-a000-000000000017', 'TUESDAY', '16:00:00', '18:00:00'),
('02200017-0000-4003-a000-000000000002', '02200017-0000-4002-a000-000000000017', 'THURSDAY', '14:00:00', '16:00:00');

-- DIURNA 02 D · Álgebra Lineal
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200018-0000-4001-a000-000000000018', 'c0020000-0000-4000-a000-000000000002', 'Álgebra Lineal', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; source sheet: Catálogo Diurna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200018-0000-4002-a000-000000000018', '02200018-0000-4001-a000-000000000018', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200018-0000-4003-a000-000000000001', '02200018-0000-4002-a000-000000000018', 'MONDAY', '16:00:00', '18:00:00'),
('02200018-0000-4003-a000-000000000002', '02200018-0000-4002-a000-000000000018', 'WEDNESDAY', '14:00:00', '16:00:00');

-- DIURNA 02 D · Cálculo Diferencial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200019-0000-4001-a000-000000000019', 'c0020000-0000-4000-a000-000000000001', 'Cálculo Diferencial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302; locations/modalities include: D4-403; source sheet: Catálogo Diurna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200019-0000-4002-a000-000000000019', '02200019-0000-4001-a000-000000000019', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200019-0000-4003-a000-000000000001', '02200019-0000-4002-a000-000000000019', 'MONDAY', '14:00:00', '16:00:00'),
('02200019-0000-4003-a000-000000000002', '02200019-0000-4002-a000-000000000019', 'THURSDAY', '16:00:00', '18:00:00'),
('02200019-0000-4003-a000-000000000003', '02200019-0000-4002-a000-000000000019', 'FRIDAY', '16:00:00', '18:00:00');

-- DIURNA 02 D · Fundamentos de Electricidad, Electrónica y Telecomunicaciones
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200020-0000-4001-a000-000000000020', 'c0020000-0000-4000-a000-000000000005', 'Fundamentos de Electricidad, Electrónica y Telecomunicaciones', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-403; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200020-0000-4002-a000-000000000020', '02200020-0000-4001-a000-000000000020', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200020-0000-4003-a000-000000000001', '02200020-0000-4002-a000-000000000020', 'WEDNESDAY', '16:00:00', '18:00:00'),
('02200020-0000-4003-a000-000000000002', '02200020-0000-4002-a000-000000000020', 'FRIDAY', '14:00:00', '16:00:00');

-- DIURNA 02 D · Pensamiento Sistémico
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200021-0000-4001-a000-000000000021', 'c0020000-0000-4000-a000-000000000004', 'Pensamiento Sistémico', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200021-0000-4002-a000-000000000021', '02200021-0000-4001-a000-000000000021', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200021-0000-4003-a000-000000000001', '02200021-0000-4002-a000-000000000021', 'TUESDAY', '14:00:00', '16:00:00');

-- DIURNA 02 D · Programación I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200022-0000-4001-a000-000000000022', 'c0020000-0000-4000-a000-000000000003', 'Programación I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; source sheet: Catálogo Diurna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200022-0000-4002-a000-000000000022', '02200022-0000-4001-a000-000000000022', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200022-0000-4003-a000-000000000001', '02200022-0000-4002-a000-000000000022', 'TUESDAY', '16:00:00', '18:00:00'),
('02200022-0000-4003-a000-000000000002', '02200022-0000-4002-a000-000000000022', 'THURSDAY', '14:00:00', '16:00:00');

-- DIURNA 03 D · Cálculo Diferencial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200023-0000-4001-a000-000000000023', 'c0020000-0000-4000-a000-000000000001', 'Cálculo Diferencial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200023-0000-4002-a000-000000000023', '02200023-0000-4001-a000-000000000023', '03 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200023-0000-4003-a000-000000000001', '02200023-0000-4002-a000-000000000023', 'MONDAY', '14:00:00', '16:00:00'),
('02200023-0000-4003-a000-000000000002', '02200023-0000-4002-a000-000000000023', 'MONDAY', '16:00:00', '18:00:00'),
('02200023-0000-4003-a000-000000000003', '02200023-0000-4002-a000-000000000023', 'FRIDAY', '16:00:00', '18:00:00');

-- DIURNA 01 D · Cálculo Integral
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200024-0000-4001-a000-000000000024', 'c0030000-0000-4000-a000-000000000001', 'Cálculo Integral', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; locations/modalities include: D4-403; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200024-0000-4002-a000-000000000024', '02200024-0000-4001-a000-000000000024', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200024-0000-4003-a000-000000000001', '02200024-0000-4002-a000-000000000024', 'MONDAY', '11:00:00', '13:00:00'),
('02200024-0000-4003-a000-000000000002', '02200024-0000-4002-a000-000000000024', 'WEDNESDAY', '11:00:00', '13:00:00');

-- DIURNA 01 D · Cálculo Integral (cada 15 días)
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200025-0000-4001-a000-000000000025', 'c0030000-0000-4000-a000-000000000001', 'Cálculo Integral (cada 15 días)', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-401; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200025-0000-4002-a000-000000000025', '02200025-0000-4001-a000-000000000025', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200025-0000-4003-a000-000000000001', '02200025-0000-4002-a000-000000000025', 'THURSDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Física General — Práctica Lab. B
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200026-0000-4001-a000-000000000026', 'c0030000-0000-4000-a000-000000000002', 'Física General — Práctica Lab. B', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: B1-229 Ciencias Básicas; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200026-0000-4002-a000-000000000026', '02200026-0000-4001-a000-000000000026', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200026-0000-4003-a000-000000000001', '02200026-0000-4002-a000-000000000026', 'FRIDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Física General — Práctica Lab. C
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200027-0000-4001-a000-000000000027', 'c0030000-0000-4000-a000-000000000002', 'Física General — Práctica Lab. C', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: B1-230 Ciencias Básicas; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200027-0000-4002-a000-000000000027', '02200027-0000-4001-a000-000000000027', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200027-0000-4003-a000-000000000001', '02200027-0000-4002-a000-000000000027', 'WEDNESDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Física General — Teoría
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200028-0000-4001-a000-000000000028', 'c0030000-0000-4000-a000-000000000002', 'Física General — Teoría', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: D4-403; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200028-0000-4002-a000-000000000028', '02200028-0000-4001-a000-000000000028', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200028-0000-4003-a000-000000000001', '02200028-0000-4002-a000-000000000028', 'TUESDAY', '07:00:00', '09:00:00'),
('02200028-0000-4003-a000-000000000002', '02200028-0000-4002-a000-000000000028', 'THURSDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Matemáticas Discretas
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200029-0000-4001-a000-000000000029', 'c0030000-0000-4000-a000-000000000003', 'Matemáticas Discretas', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200029-0000-4002-a000-000000000029', '02200029-0000-4001-a000-000000000029', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200029-0000-4003-a000-000000000001', '02200029-0000-4002-a000-000000000029', 'TUESDAY', '09:00:00', '11:00:00'),
('02200029-0000-4003-a000-000000000002', '02200029-0000-4002-a000-000000000029', 'FRIDAY', '11:00:00', '13:00:00');

-- DIURNA 01 D · Programación II
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200030-0000-4001-a000-000000000030', 'c0030000-0000-4000-a000-000000000005', 'Programación II', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-413; locations/modalities include: F6-305 Agroindustria; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200030-0000-4002-a000-000000000030', '02200030-0000-4001-a000-000000000030', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200030-0000-4003-a000-000000000001', '02200030-0000-4002-a000-000000000030', 'WEDNESDAY', '09:00:00', '11:00:00'),
('02200030-0000-4003-a000-000000000002', '02200030-0000-4002-a000-000000000030', 'SATURDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Teoría y Diseño Organizacional
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200031-0000-4001-a000-000000000031', 'c0030000-0000-4000-a000-000000000004', 'Teoría y Diseño Organizacional', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200031-0000-4002-a000-000000000031', '02200031-0000-4001-a000-000000000031', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200031-0000-4003-a000-000000000001', '02200031-0000-4002-a000-000000000031', 'TUESDAY', '11:00:00', '13:00:00'),
('02200031-0000-4003-a000-000000000002', '02200031-0000-4002-a000-000000000031', 'FRIDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Cálculo Integral
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200032-0000-4001-a000-000000000032', 'c0030000-0000-4000-a000-000000000001', 'Cálculo Integral', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200032-0000-4002-a000-000000000032', '02200032-0000-4001-a000-000000000032', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200032-0000-4003-a000-000000000001', '02200032-0000-4002-a000-000000000032', 'MONDAY', '11:00:00', '13:00:00'),
('02200032-0000-4003-a000-000000000002', '02200032-0000-4002-a000-000000000032', 'WEDNESDAY', '11:00:00', '13:00:00');

-- DIURNA 02 D · Cálculo Integral (cada 15 días)
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200033-0000-4001-a000-000000000033', 'c0030000-0000-4000-a000-000000000001', 'Cálculo Integral (cada 15 días)', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-401; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200033-0000-4002-a000-000000000033', '02200033-0000-4001-a000-000000000033', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200033-0000-4003-a000-000000000001', '02200033-0000-4002-a000-000000000033', 'MONDAY', '08:00:00', '09:00:00');

-- DIURNA 02 D · Física General — Práctica Lab. A
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200034-0000-4001-a000-000000000034', 'c0030000-0000-4000-a000-000000000002', 'Física General — Práctica Lab. A', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: B1-225 Ciencias Básicas; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200034-0000-4002-a000-000000000034', '02200034-0000-4001-a000-000000000034', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200034-0000-4003-a000-000000000001', '02200034-0000-4002-a000-000000000034', 'WEDNESDAY', '07:00:00', '09:00:00'),
('02200034-0000-4003-a000-000000000002', '02200034-0000-4002-a000-000000000034', 'THURSDAY', '07:00:00', '09:00:00');

-- DIURNA 02 D · Física General — Teoría
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200035-0000-4001-a000-000000000035', 'c0030000-0000-4000-a000-000000000002', 'Física General — Teoría', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: D4-401; locations/modalities include: F6-306; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200035-0000-4002-a000-000000000035', '02200035-0000-4001-a000-000000000035', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200035-0000-4003-a000-000000000001', '02200035-0000-4002-a000-000000000035', 'TUESDAY', '07:00:00', '09:00:00'),
('02200035-0000-4003-a000-000000000002', '02200035-0000-4002-a000-000000000035', 'WEDNESDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Matemáticas Discretas
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200036-0000-4001-a000-000000000036', 'c0030000-0000-4000-a000-000000000003', 'Matemáticas Discretas', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302; locations/modalities include: D4-403; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200036-0000-4002-a000-000000000036', '02200036-0000-4001-a000-000000000036', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200036-0000-4003-a000-000000000001', '02200036-0000-4002-a000-000000000036', 'MONDAY', '09:00:00', '11:00:00'),
('02200036-0000-4003-a000-000000000002', '02200036-0000-4002-a000-000000000036', 'FRIDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Programación II
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200037-0000-4001-a000-000000000037', 'c0030000-0000-4000-a000-000000000005', 'Programación II', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200037-0000-4002-a000-000000000037', '02200037-0000-4001-a000-000000000037', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200037-0000-4003-a000-000000000001', '02200037-0000-4002-a000-000000000037', 'TUESDAY', '09:00:00', '11:00:00'),
('02200037-0000-4003-a000-000000000002', '02200037-0000-4002-a000-000000000037', 'SATURDAY', '07:00:00', '09:00:00');

-- DIURNA 02 D · Teoría y Diseño Organizacional
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200038-0000-4001-a000-000000000038', 'c0030000-0000-4000-a000-000000000004', 'Teoría y Diseño Organizacional', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-411; locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200038-0000-4002-a000-000000000038', '02200038-0000-4001-a000-000000000038', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200038-0000-4003-a000-000000000001', '02200038-0000-4002-a000-000000000038', 'TUESDAY', '11:00:00', '13:00:00'),
('02200038-0000-4003-a000-000000000002', '02200038-0000-4002-a000-000000000038', 'THURSDAY', '09:00:00', '11:00:00');

-- DIURNA 03 D · Cálculo Integral
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200039-0000-4001-a000-000000000039', 'c0030000-0000-4000-a000-000000000001', 'Cálculo Integral', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200039-0000-4002-a000-000000000039', '02200039-0000-4001-a000-000000000039', '03 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200039-0000-4003-a000-000000000001', '02200039-0000-4002-a000-000000000039', 'MONDAY', '11:00:00', '13:00:00'),
('02200039-0000-4003-a000-000000000002', '02200039-0000-4002-a000-000000000039', 'WEDNESDAY', '11:00:00', '13:00:00');

-- DIURNA 03 D · Cálculo Integral (cada 15 días)
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200040-0000-4001-a000-000000000040', 'c0030000-0000-4000-a000-000000000001', 'Cálculo Integral (cada 15 días)', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200040-0000-4002-a000-000000000040', '02200040-0000-4001-a000-000000000040', '03 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200040-0000-4003-a000-000000000001', '02200040-0000-4002-a000-000000000040', 'MONDAY', '07:00:00', '08:00:00');

-- DIURNA 01 D · Cálculo Multivariado Vectorial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200041-0000-4001-a000-000000000041', 'c0040000-0000-4000-a000-000000000001', 'Cálculo Multivariado Vectorial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-403; locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200041-0000-4002-a000-000000000041', '02200041-0000-4001-a000-000000000041', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200041-0000-4003-a000-000000000001', '02200041-0000-4002-a000-000000000041', 'WEDNESDAY', '16:00:00', '18:00:00'),
('02200041-0000-4003-a000-000000000002', '02200041-0000-4002-a000-000000000041', 'FRIDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Ecuaciones Diferenciales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200042-0000-4001-a000-000000000042', 'c0040000-0000-4000-a000-000000000002', 'Ecuaciones Diferenciales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-409; locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200042-0000-4002-a000-000000000042', '02200042-0000-4001-a000-000000000042', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200042-0000-4003-a000-000000000001', '02200042-0000-4002-a000-000000000042', 'MONDAY', '14:00:00', '16:00:00'),
('02200042-0000-4003-a000-000000000002', '02200042-0000-4002-a000-000000000042', 'FRIDAY', '16:00:00', '18:00:00');

-- DIURNA 01 D · Fundamentos de Infraestructura Computacional
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200043-0000-4001-a000-000000000043', 'c0040000-0000-4000-a000-000000000006', 'Fundamentos de Infraestructura Computacional', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302; source sheet: Catálogo Diurna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200043-0000-4002-a000-000000000043', '02200043-0000-4001-a000-000000000043', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200043-0000-4003-a000-000000000001', '02200043-0000-4002-a000-000000000043', 'TUESDAY', '16:00:00', '18:00:00'),
('02200043-0000-4003-a000-000000000002', '02200043-0000-4002-a000-000000000043', 'THURSDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Programación III (Elixir)
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200044-0000-4001-a000-000000000044', 'c0040000-0000-4000-a000-000000000004', 'Programación III (Elixir)', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-401; locations/modalities include: D4-403; source sheet: Catálogo Diurna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200044-0000-4002-a000-000000000044', '02200044-0000-4001-a000-000000000044', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200044-0000-4003-a000-000000000001', '02200044-0000-4002-a000-000000000044', 'TUESDAY', '14:00:00', '16:00:00'),
('02200044-0000-4003-a000-000000000002', '02200044-0000-4002-a000-000000000044', 'THURSDAY', '16:00:00', '18:00:00');

-- DIURNA 01 D · Sistemas de Información
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200045-0000-4001-a000-000000000045', 'c0040000-0000-4000-a000-000000000005', 'Sistemas de Información', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302; source sheet: Catálogo Diurna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200045-0000-4002-a000-000000000045', '02200045-0000-4001-a000-000000000045', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200045-0000-4003-a000-000000000001', '02200045-0000-4002-a000-000000000045', 'MONDAY', '16:00:00', '18:00:00'),
('02200045-0000-4003-a000-000000000002', '02200045-0000-4002-a000-000000000045', 'WEDNESDAY', '14:00:00', '16:00:00');

-- DIURNA 02 D · Cálculo Multivariado Vectorial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200046-0000-4001-a000-000000000046', 'c0040000-0000-4000-a000-000000000001', 'Cálculo Multivariado Vectorial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-302; locations/modalities include: D4-403; source sheet: Catálogo Diurna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200046-0000-4002-a000-000000000046', '02200046-0000-4001-a000-000000000046', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200046-0000-4003-a000-000000000001', '02200046-0000-4002-a000-000000000046', 'WEDNESDAY', '16:00:00', '18:00:00'),
('02200046-0000-4003-a000-000000000002', '02200046-0000-4002-a000-000000000046', 'FRIDAY', '14:00:00', '16:00:00');

-- DIURNA 02 D · Ecuaciones Diferenciales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200047-0000-4001-a000-000000000047', 'c0040000-0000-4000-a000-000000000002', 'Ecuaciones Diferenciales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; locations/modalities include: D4-405; source sheet: Catálogo Diurna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200047-0000-4002-a000-000000000047', '02200047-0000-4001-a000-000000000047', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200047-0000-4003-a000-000000000001', '02200047-0000-4002-a000-000000000047', 'MONDAY', '14:00:00', '16:00:00'),
('02200047-0000-4003-a000-000000000002', '02200047-0000-4002-a000-000000000047', 'FRIDAY', '16:00:00', '18:00:00');

-- DIURNA 02 D · Fundamentos de Infraestructura Computacional
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200048-0000-4001-a000-000000000048', 'c0040000-0000-4000-a000-000000000006', 'Fundamentos de Infraestructura Computacional', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200048-0000-4002-a000-000000000048', '02200048-0000-4001-a000-000000000048', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200048-0000-4003-a000-000000000001', '02200048-0000-4002-a000-000000000048', 'TUESDAY', '16:00:00', '18:00:00'),
('02200048-0000-4003-a000-000000000002', '02200048-0000-4002-a000-000000000048', 'THURSDAY', '14:00:00', '16:00:00');

-- DIURNA 02 D · Programación III (Elixir)
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200049-0000-4001-a000-000000000049', 'c0040000-0000-4000-a000-000000000004', 'Programación III (Elixir)', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: F3-306; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200049-0000-4002-a000-000000000049', '02200049-0000-4001-a000-000000000049', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200049-0000-4003-a000-000000000001', '02200049-0000-4002-a000-000000000049', 'TUESDAY', '14:00:00', '16:00:00'),
('02200049-0000-4003-a000-000000000002', '02200049-0000-4002-a000-000000000049', 'THURSDAY', '16:00:00', '18:00:00');

-- DIURNA 02 D · Sistemas de Información
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200050-0000-4001-a000-000000000050', 'c0040000-0000-4000-a000-000000000005', 'Sistemas de Información', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-413; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200050-0000-4002-a000-000000000050', '02200050-0000-4001-a000-000000000050', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200050-0000-4003-a000-000000000001', '02200050-0000-4002-a000-000000000050', 'MONDAY', '16:00:00', '18:00:00'),
('02200050-0000-4003-a000-000000000002', '02200050-0000-4002-a000-000000000050', 'WEDNESDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Estadística y Probabilidad
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200051-0000-4001-a000-000000000051', 'c0050000-0000-4000-a000-000000000001', 'Estadística y Probabilidad', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: B6-102 50 Años; locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200051-0000-4002-a000-000000000051', '02200051-0000-4001-a000-000000000051', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200051-0000-4003-a000-000000000001', '02200051-0000-4002-a000-000000000051', 'TUESDAY', '09:00:00', '11:00:00'),
('02200051-0000-4003-a000-000000000002', '02200051-0000-4002-a000-000000000051', 'THURSDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Estructura de Datos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200052-0000-4001-a000-000000000052', 'c0050000-0000-4000-a000-000000000004', 'Estructura de Datos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-317 Sala Inf. B; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200052-0000-4002-a000-000000000052', '02200052-0000-4001-a000-000000000052', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200052-0000-4003-a000-000000000001', '02200052-0000-4002-a000-000000000052', 'WEDNESDAY', '07:00:00', '09:00:00'),
('02200052-0000-4003-a000-000000000002', '02200052-0000-4002-a000-000000000052', 'FRIDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Infraestructura Computacional — Práctica
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200053-0000-4001-a000-000000000053', 'c0040000-0000-4000-a000-000000000006', 'Infraestructura Computacional — Práctica', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: F3-306 Económicas; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200053-0000-4002-a000-000000000053', '02200053-0000-4001-a000-000000000053', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200053-0000-4003-a000-000000000001', '02200053-0000-4002-a000-000000000053', 'WEDNESDAY', '11:00:00', '13:00:00');

-- DIURNA 01 D · Infraestructura Computacional — Teoría
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200054-0000-4001-a000-000000000054', 'c0040000-0000-4000-a000-000000000006', 'Infraestructura Computacional — Teoría', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: D4-302; locations/modalities include: F3-306 Económicas; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200054-0000-4002-a000-000000000054', '02200054-0000-4001-a000-000000000054', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200054-0000-4003-a000-000000000001', '02200054-0000-4002-a000-000000000054', 'MONDAY', '09:00:00', '11:00:00'),
('02200054-0000-4003-a000-000000000002', '02200054-0000-4002-a000-000000000054', 'WEDNESDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Ingeniería de Software I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200055-0000-4001-a000-000000000055', 'c0050000-0000-4000-a000-000000000005', 'Ingeniería de Software I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200055-0000-4002-a000-000000000055', '02200055-0000-4001-a000-000000000055', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200055-0000-4003-a000-000000000001', '02200055-0000-4002-a000-000000000055', 'TUESDAY', '07:00:00', '09:00:00'),
('02200055-0000-4003-a000-000000000002', '02200055-0000-4002-a000-000000000055', 'THURSDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Teoría de Grafos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200056-0000-4001-a000-000000000056', 'c0050000-0000-4000-a000-000000000002', 'Teoría de Grafos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; locations/modalities include: D4-402; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200056-0000-4002-a000-000000000056', '02200056-0000-4001-a000-000000000056', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200056-0000-4003-a000-000000000001', '02200056-0000-4002-a000-000000000056', 'MONDAY', '07:00:00', '09:00:00'),
('02200056-0000-4003-a000-000000000002', '02200056-0000-4002-a000-000000000056', 'FRIDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Estadística y Probabilidad
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200057-0000-4001-a000-000000000057', 'c0050000-0000-4000-a000-000000000001', 'Estadística y Probabilidad', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200057-0000-4002-a000-000000000057', '02200057-0000-4001-a000-000000000057', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200057-0000-4003-a000-000000000001', '02200057-0000-4002-a000-000000000057', 'TUESDAY', '09:00:00', '11:00:00'),
('02200057-0000-4003-a000-000000000002', '02200057-0000-4002-a000-000000000057', 'THURSDAY', '07:00:00', '09:00:00');

-- DIURNA 02 D · Estructura de Datos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200058-0000-4001-a000-000000000058', 'c0050000-0000-4000-a000-000000000004', 'Estructura de Datos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-413; locations/modalities include: F3-306 Económicas; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200058-0000-4002-a000-000000000058', '02200058-0000-4001-a000-000000000058', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200058-0000-4003-a000-000000000001', '02200058-0000-4002-a000-000000000058', 'WEDNESDAY', '07:00:00', '09:00:00'),
('02200058-0000-4003-a000-000000000002', '02200058-0000-4002-a000-000000000058', 'FRIDAY', '07:00:00', '09:00:00');

-- DIURNA 02 D · Infraestructura Computacional — Práctica
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200059-0000-4001-a000-000000000059', 'c0040000-0000-4000-a000-000000000006', 'Infraestructura Computacional — Práctica', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: D4-302; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200059-0000-4002-a000-000000000059', '02200059-0000-4001-a000-000000000059', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200059-0000-4003-a000-000000000001', '02200059-0000-4002-a000-000000000059', 'WEDNESDAY', '11:00:00', '13:00:00');

-- DIURNA 02 D · Infraestructura Computacional — Teoría
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200060-0000-4001-a000-000000000060', 'c0040000-0000-4000-a000-000000000006', 'Infraestructura Computacional — Teoría', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: F1-310 Educación; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200060-0000-4002-a000-000000000060', '02200060-0000-4001-a000-000000000060', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200060-0000-4003-a000-000000000001', '02200060-0000-4002-a000-000000000060', 'MONDAY', '09:00:00', '11:00:00'),
('02200060-0000-4003-a000-000000000002', '02200060-0000-4002-a000-000000000060', 'WEDNESDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Ingeniería de Software I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200061-0000-4001-a000-000000000061', 'c0050000-0000-4000-a000-000000000005', 'Ingeniería de Software I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-413; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200061-0000-4002-a000-000000000061', '02200061-0000-4001-a000-000000000061', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200061-0000-4003-a000-000000000001', '02200061-0000-4002-a000-000000000061', 'TUESDAY', '07:00:00', '09:00:00'),
('02200061-0000-4003-a000-000000000002', '02200061-0000-4002-a000-000000000061', 'THURSDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Teoría de Grafos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200062-0000-4001-a000-000000000062', 'c0050000-0000-4000-a000-000000000002', 'Teoría de Grafos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-406; locations/modalities include: F3-306 Económicas; source sheet: Catálogo Diurna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200062-0000-4002-a000-000000000062', '02200062-0000-4001-a000-000000000062', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200062-0000-4003-a000-000000000001', '02200062-0000-4002-a000-000000000062', 'MONDAY', '07:00:00', '09:00:00'),
('02200062-0000-4003-a000-000000000002', '02200062-0000-4002-a000-000000000062', 'FRIDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Análisis Numérico
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200063-0000-4001-a000-000000000063', 'c0060000-0000-4000-a000-000000000002', 'Análisis Numérico', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: F6-205 Agroindustria; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200063-0000-4002-a000-000000000063', '02200063-0000-4001-a000-000000000063', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200063-0000-4003-a000-000000000001', '02200063-0000-4002-a000-000000000063', 'MONDAY', '16:00:00', '18:00:00'),
('02200063-0000-4003-a000-000000000002', '02200063-0000-4002-a000-000000000063', 'FRIDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Bases de Datos I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200064-0000-4001-a000-000000000064', 'c0060000-0000-4000-a000-000000000003', 'Bases de Datos I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-317 Sala Inf. B; locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200064-0000-4002-a000-000000000064', '02200064-0000-4001-a000-000000000064', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200064-0000-4003-a000-000000000001', '02200064-0000-4002-a000-000000000064', 'TUESDAY', '16:00:00', '18:00:00'),
('02200064-0000-4003-a000-000000000002', '02200064-0000-4002-a000-000000000064', 'SATURDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Dirección Estratégica de T.I.
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200065-0000-4001-a000-000000000065', 'c0060000-0000-4000-a000-000000000001', 'Dirección Estratégica de T.I.', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200065-0000-4002-a000-000000000065', '02200065-0000-4001-a000-000000000065', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200065-0000-4003-a000-000000000001', '02200065-0000-4002-a000-000000000065', 'WEDNESDAY', '16:00:00', '18:00:00'),
('02200065-0000-4003-a000-000000000002', '02200065-0000-4002-a000-000000000065', 'FRIDAY', '16:00:00', '18:00:00');

-- DIURNA 01 D · Infraestructura de Comunicaciones — Práctica
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200066-0000-4001-a000-000000000066', 'c0060000-0000-4000-a000-000000000006', 'Infraestructura de Comunicaciones — Práctica', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: D4-317 Sala Inf. B; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200066-0000-4002-a000-000000000066', '02200066-0000-4001-a000-000000000066', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200066-0000-4003-a000-000000000001', '02200066-0000-4002-a000-000000000066', 'THURSDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Infraestructura de Comunicaciones — Teoría
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200067-0000-4001-a000-000000000067', 'c0060000-0000-4000-a000-000000000006', 'Infraestructura de Comunicaciones — Teoría', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: D4-317 Sala Inf. B; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200067-0000-4002-a000-000000000067', '02200067-0000-4001-a000-000000000067', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200067-0000-4003-a000-000000000001', '02200067-0000-4002-a000-000000000067', 'MONDAY', '07:00:00', '09:00:00'),
('02200067-0000-4003-a000-000000000002', '02200067-0000-4002-a000-000000000067', 'WEDNESDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Ingeniería de Software II
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200068-0000-4001-a000-000000000068', 'c0060000-0000-4000-a000-000000000005', 'Ingeniería de Software II', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200068-0000-4002-a000-000000000068', '02200068-0000-4001-a000-000000000068', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200068-0000-4003-a000-000000000001', '02200068-0000-4002-a000-000000000068', 'MONDAY', '14:00:00', '16:00:00'),
('02200068-0000-4003-a000-000000000002', '02200068-0000-4002-a000-000000000068', 'THURSDAY', '16:00:00', '18:00:00');

-- DIURNA 01 D · Programación Avanzada
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200069-0000-4001-a000-000000000069', 'c0060000-0000-4000-a000-000000000004', 'Programación Avanzada', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-317 Sala Inf. B; locations/modalities include: F6-206 Agroindustria; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200069-0000-4002-a000-000000000069', '02200069-0000-4001-a000-000000000069', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200069-0000-4003-a000-000000000001', '02200069-0000-4002-a000-000000000069', 'MONDAY', '09:00:00', '11:00:00'),
('02200069-0000-4003-a000-000000000002', '02200069-0000-4002-a000-000000000069', 'WEDNESDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Bases de Datos I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200070-0000-4001-a000-000000000070', 'c0060000-0000-4000-a000-000000000003', 'Bases de Datos I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302 Maestría; locations/modalities include: F6-206 Agroindustria; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200070-0000-4002-a000-000000000070', '02200070-0000-4001-a000-000000000070', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200070-0000-4003-a000-000000000001', '02200070-0000-4002-a000-000000000070', 'MONDAY', '16:00:00', '18:00:00'),
('02200070-0000-4003-a000-000000000002', '02200070-0000-4002-a000-000000000070', 'FRIDAY', '14:00:00', '16:00:00');

-- DIURNA 02 D · Dirección Estratégica de T.I.
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200071-0000-4001-a000-000000000071', 'c0060000-0000-4000-a000-000000000001', 'Dirección Estratégica de T.I.', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-405; locations/modalities include: D4-406; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200071-0000-4002-a000-000000000071', '02200071-0000-4001-a000-000000000071', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200071-0000-4003-a000-000000000001', '02200071-0000-4002-a000-000000000071', 'WEDNESDAY', '16:00:00', '18:00:00'),
('02200071-0000-4003-a000-000000000002', '02200071-0000-4002-a000-000000000071', 'FRIDAY', '16:00:00', '18:00:00');

-- DIURNA 02 D · Infraestructura de Comunicaciones — Práctica
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200072-0000-4001-a000-000000000072', 'c0060000-0000-4000-a000-000000000006', 'Infraestructura de Comunicaciones — Práctica', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200072-0000-4002-a000-000000000072', '02200072-0000-4001-a000-000000000072', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200072-0000-4003-a000-000000000001', '02200072-0000-4002-a000-000000000072', 'THURSDAY', '14:00:00', '16:00:00');

-- DIURNA 02 D · Infraestructura de Comunicaciones — Teoría
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200073-0000-4001-a000-000000000073', 'c0060000-0000-4000-a000-000000000006', 'Infraestructura de Comunicaciones — Teoría', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: D4-302 Aula Maestría; locations/modalities include: F3-305 Económicas; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200073-0000-4002-a000-000000000073', '02200073-0000-4001-a000-000000000073', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200073-0000-4003-a000-000000000001', '02200073-0000-4002-a000-000000000073', 'MONDAY', '07:00:00', '09:00:00'),
('02200073-0000-4003-a000-000000000002', '02200073-0000-4002-a000-000000000073', 'WEDNESDAY', '14:00:00', '16:00:00');

-- DIURNA 02 D · Ingeniería de Software II
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200074-0000-4001-a000-000000000074', 'c0060000-0000-4000-a000-000000000005', 'Ingeniería de Software II', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-317 Sala Inf. B; locations/modalities include: F1-210 Educación; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200074-0000-4002-a000-000000000074', '02200074-0000-4001-a000-000000000074', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200074-0000-4003-a000-000000000001', '02200074-0000-4002-a000-000000000074', 'MONDAY', '14:00:00', '16:00:00'),
('02200074-0000-4003-a000-000000000002', '02200074-0000-4002-a000-000000000074', 'THURSDAY', '16:00:00', '18:00:00');

-- DIURNA 02 D · Programación Avanzada
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200075-0000-4001-a000-000000000075', 'c0060000-0000-4000-a000-000000000004', 'Programación Avanzada', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-413; locations/modalities include: F3-306 Económicas; source sheet: Catálogo Diurna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200075-0000-4002-a000-000000000075', '02200075-0000-4001-a000-000000000075', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200075-0000-4003-a000-000000000001', '02200075-0000-4002-a000-000000000075', 'MONDAY', '09:00:00', '11:00:00'),
('02200075-0000-4003-a000-000000000002', '02200075-0000-4002-a000-000000000075', 'WEDNESDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Administración de Infraestructura de T.I.
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200076-0000-4001-a000-000000000076', 'c0070000-0000-4000-a000-000000000006', 'Administración de Infraestructura de T.I.', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302; locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200076-0000-4002-a000-000000000076', '02200076-0000-4001-a000-000000000076', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200076-0000-4003-a000-000000000001', '02200076-0000-4002-a000-000000000076', 'MONDAY', '07:00:00', '09:00:00'),
('02200076-0000-4003-a000-000000000002', '02200076-0000-4002-a000-000000000076', 'THURSDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Bases de Datos II
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200077-0000-4001-a000-000000000077', 'c0070000-0000-4000-a000-000000000004', 'Bases de Datos II', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: F3-305 Económicas; source sheet: Catálogo Diurna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200077-0000-4002-a000-000000000077', '02200077-0000-4001-a000-000000000077', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200077-0000-4003-a000-000000000001', '02200077-0000-4002-a000-000000000077', 'TUESDAY', '07:00:00', '09:00:00'),
('02200077-0000-4003-a000-000000000002', '02200077-0000-4002-a000-000000000077', 'FRIDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Fundamentos de HCI
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200078-0000-4001-a000-000000000078', 'c0070000-0000-4000-a000-000000000002', 'Fundamentos de HCI', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-413; locations/modalities include: F3-401 Económicas; source sheet: Catálogo Diurna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200078-0000-4002-a000-000000000078', '02200078-0000-4001-a000-000000000078', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200078-0000-4003-a000-000000000001', '02200078-0000-4002-a000-000000000078', 'WEDNESDAY', '09:00:00', '11:00:00'),
('02200078-0000-4003-a000-000000000002', '02200078-0000-4002-a000-000000000078', 'FRIDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Ingeniería de Software III
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200079-0000-4001-a000-000000000079', 'c0070000-0000-4000-a000-000000000005', 'Ingeniería de Software III', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: B1-114 Ciencias Básicas; locations/modalities include: D4-403; source sheet: Catálogo Diurna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200079-0000-4002-a000-000000000079', '02200079-0000-4001-a000-000000000079', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200079-0000-4003-a000-000000000001', '02200079-0000-4002-a000-000000000079', 'MONDAY', '09:00:00', '11:00:00'),
('02200079-0000-4003-a000-000000000002', '02200079-0000-4002-a000-000000000079', 'WEDNESDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Ingeniería Económica
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200080-0000-4001-a000-000000000080', 'c0070000-0000-4000-a000-000000000001', 'Ingeniería Económica', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200080-0000-4002-a000-000000000080', '02200080-0000-4001-a000-000000000080', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200080-0000-4003-a000-000000000001', '02200080-0000-4002-a000-000000000080', 'WEDNESDAY', '11:00:00', '13:00:00'),
('02200080-0000-4003-a000-000000000002', '02200080-0000-4002-a000-000000000080', 'THURSDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Seminario de Investigación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200081-0000-4001-a000-000000000081', 'c0070000-0000-4000-a000-000000000003', 'Seminario de Investigación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: B6-104 50 Años; locations/modalities include: D4-401; source sheet: Catálogo Diurna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200081-0000-4002-a000-000000000081', '02200081-0000-4001-a000-000000000081', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200081-0000-4003-a000-000000000001', '02200081-0000-4002-a000-000000000081', 'TUESDAY', '09:00:00', '11:00:00'),
('02200081-0000-4003-a000-000000000002', '02200081-0000-4002-a000-000000000081', 'THURSDAY', '11:00:00', '13:00:00');

-- DIURNA 02 D · Bases de Datos II
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200082-0000-4001-a000-000000000082', 'c0070000-0000-4000-a000-000000000004', 'Bases de Datos II', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-405; locations/modalities include: F3-303 Económicas; source sheet: Catálogo Diurna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200082-0000-4002-a000-000000000082', '02200082-0000-4001-a000-000000000082', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200082-0000-4003-a000-000000000001', '02200082-0000-4002-a000-000000000082', 'TUESDAY', '07:00:00', '09:00:00'),
('02200082-0000-4003-a000-000000000002', '02200082-0000-4002-a000-000000000082', 'FRIDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Análisis de Algoritmos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200083-0000-4001-a000-000000000083', 'c0080000-0000-4000-a000-000000000003', 'Análisis de Algoritmos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: B1-203 Ciencias Básicas; locations/modalities include: B1-303 Ciencias Básicas; source sheet: Catálogo Diurna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200083-0000-4002-a000-000000000083', '02200083-0000-4001-a000-000000000083', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200083-0000-4003-a000-000000000001', '02200083-0000-4002-a000-000000000083', 'TUESDAY', '16:00:00', '18:00:00'),
('02200083-0000-4003-a000-000000000002', '02200083-0000-4002-a000-000000000083', 'THURSDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Formulación y Evaluación de Proyectos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200084-0000-4001-a000-000000000084', 'c0080000-0000-4000-a000-000000000001', 'Formulación y Evaluación de Proyectos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'source sheet: Catálogo Diurna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200084-0000-4002-a000-000000000084', '02200084-0000-4001-a000-000000000084', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200084-0000-4003-a000-000000000001', '02200084-0000-4002-a000-000000000084', 'FRIDAY', '11:00:00', '13:00:00');

-- DIURNA 01 D · Profundización Computación en la Nube
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200085-0000-4001-a000-000000000085', 'c0080000-0000-4000-a000-000000000004', 'Profundización Computación en la Nube', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-318 Sala Inf. A; mapped to generic Profundización I slot; source sheet: Catálogo Diurna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200085-0000-4002-a000-000000000085', '02200085-0000-4001-a000-000000000085', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200085-0000-4003-a000-000000000001', '02200085-0000-4002-a000-000000000085', 'MONDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Profundización Computación en la Nube (Sincrónico)
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200086-0000-4001-a000-000000000086', 'c0080000-0000-4000-a000-000000000004', 'Profundización Computación en la Nube (Sincrónico)', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: Encuentro Sincrónico; mapped to generic Profundización I slot; source sheet: Catálogo Diurna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200086-0000-4002-a000-000000000086', '02200086-0000-4001-a000-000000000086', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200086-0000-4003-a000-000000000001', '02200086-0000-4002-a000-000000000086', 'THURSDAY', '16:00:00', '18:00:00');

-- DIURNA 01 D · Profundización Introducción al Big Data
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200087-0000-4001-a000-000000000087', 'c0080000-0000-4000-a000-000000000004', 'Profundización Introducción al Big Data', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: B1-202 Ciencias Básicas; locations/modalities include: D4-317 Sala Inf. B; mapped to generic Profundización I slot; source sheet: Catálogo Diurna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200087-0000-4002-a000-000000000087', '02200087-0000-4001-a000-000000000087', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200087-0000-4003-a000-000000000001', '02200087-0000-4002-a000-000000000087', 'MONDAY', '16:00:00', '18:00:00'),
('02200087-0000-4003-a000-000000000002', '02200087-0000-4002-a000-000000000087', 'WEDNESDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Seminario de Ingeniería
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200088-0000-4001-a000-000000000088', 'c0080000-0000-4000-a000-000000000006', 'Seminario de Ingeniería', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: F3-405 Económicas; source sheet: Catálogo Diurna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200088-0000-4002-a000-000000000088', '02200088-0000-4001-a000-000000000088', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200088-0000-4003-a000-000000000001', '02200088-0000-4002-a000-000000000088', 'WEDNESDAY', '16:00:00', '18:00:00'),
('02200088-0000-4003-a000-000000000002', '02200088-0000-4002-a000-000000000088', 'FRIDAY', '14:00:00', '16:00:00');

-- DIURNA 01 D · Teoría de Lenguajes Formales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200089-0000-4001-a000-000000000089', 'c0080000-0000-4000-a000-000000000002', 'Teoría de Lenguajes Formales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302; locations/modalities include: F6-206 Agroindustria; source sheet: Catálogo Diurna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200089-0000-4002-a000-000000000089', '02200089-0000-4001-a000-000000000089', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200089-0000-4003-a000-000000000001', '02200089-0000-4002-a000-000000000089', 'TUESDAY', '14:00:00', '16:00:00'),
('02200089-0000-4003-a000-000000000002', '02200089-0000-4002-a000-000000000089', 'FRIDAY', '16:00:00', '18:00:00');

-- DIURNA 02 D · Análisis de Algoritmos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200090-0000-4001-a000-000000000090', 'c0080000-0000-4000-a000-000000000003', 'Análisis de Algoritmos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: B6-207 50 Años; source sheet: Catálogo Diurna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200090-0000-4002-a000-000000000090', '02200090-0000-4001-a000-000000000090', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200090-0000-4003-a000-000000000001', '02200090-0000-4002-a000-000000000090', 'TUESDAY', '14:00:00', '16:00:00'),
('02200090-0000-4003-a000-000000000002', '02200090-0000-4002-a000-000000000090', 'THURSDAY', '16:00:00', '18:00:00');

-- DIURNA 03 D · Profundización Diseño Centrado en el Usuario
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200091-0000-4001-a000-000000000091', 'c0080000-0000-4000-a000-000000000004', 'Profundización Diseño Centrado en el Usuario', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-318 Sala Inf. A; locations/modalities include: F3-203 Posgrado; mapped to generic Profundización I slot; source sheet: Catálogo Diurna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200091-0000-4002-a000-000000000091', '02200091-0000-4001-a000-000000000091', '03 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200091-0000-4003-a000-000000000001', '02200091-0000-4002-a000-000000000091', 'MONDAY', '16:00:00', '18:00:00'),
('02200091-0000-4003-a000-000000000002', '02200091-0000-4002-a000-000000000091', 'WEDNESDAY', '14:00:00', '16:00:00');

-- DIURNA 03 D · Profundización Visualización de Datos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200092-0000-4001-a000-000000000092', 'c0080000-0000-4000-a000-000000000004', 'Profundización Visualización de Datos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-317 Sala Inf. B; locations/modalities include: F3-203 Posgrado; mapped to generic Profundización I slot; source sheet: Catálogo Diurna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200092-0000-4002-a000-000000000092', '02200092-0000-4001-a000-000000000092', '03 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200092-0000-4003-a000-000000000001', '02200092-0000-4002-a000-000000000092', 'MONDAY', '14:00:00', '16:00:00'),
('02200092-0000-4003-a000-000000000002', '02200092-0000-4002-a000-000000000092', 'THURSDAY', '16:00:00', '18:00:00');

-- DIURNA 01 D · Administración
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200093-0000-4001-a000-000000000093', 'c0090000-0000-4000-a000-000000000006', 'Administración', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: B6-103 50 Años; locations/modalities include: D4-302; source sheet: Catálogo Diurna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200093-0000-4002-a000-000000000093', '02200093-0000-4001-a000-000000000093', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200093-0000-4003-a000-000000000001', '02200093-0000-4002-a000-000000000093', 'TUESDAY', '09:00:00', '11:00:00'),
('02200093-0000-4003-a000-000000000002', '02200093-0000-4002-a000-000000000093', 'FRIDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Administración de Proyectos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200094-0000-4001-a000-000000000094', 'c0090000-0000-4000-a000-000000000001', 'Administración de Proyectos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: F3-306 Económicas; source sheet: Catálogo Diurna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200094-0000-4002-a000-000000000094', '02200094-0000-4001-a000-000000000094', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200094-0000-4003-a000-000000000001', '02200094-0000-4002-a000-000000000094', 'MONDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Inteligencia Artificial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200095-0000-4001-a000-000000000095', 'c0090000-0000-4000-a000-000000000002', 'Inteligencia Artificial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-317 Sala Inf. B; locations/modalities include: F6-306 Agroindustria; source sheet: Catálogo Diurna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200095-0000-4002-a000-000000000095', '02200095-0000-4001-a000-000000000095', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200095-0000-4003-a000-000000000001', '02200095-0000-4002-a000-000000000095', 'WEDNESDAY', '07:00:00', '09:00:00'),
('02200095-0000-4003-a000-000000000002', '02200095-0000-4002-a000-000000000095', 'FRIDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Legislación y Propiedad Intelectual
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200096-0000-4001-a000-000000000096', 'c0090000-0000-4000-a000-000000000003', 'Legislación y Propiedad Intelectual', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-401; source sheet: Catálogo Diurna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200096-0000-4002-a000-000000000096', '02200096-0000-4001-a000-000000000096', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200096-0000-4003-a000-000000000001', '02200096-0000-4002-a000-000000000096', 'THURSDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Profundización Arquitectura Orientada a Microservicios (Sincrónico)
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200097-0000-4001-a000-000000000097', 'c0080000-0000-4000-a000-000000000004', 'Profundización Arquitectura Orientada a Microservicios (Sincrónico)', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: Encuentro Sincrónico; mapped to generic Profundización I slot; source sheet: Catálogo Diurna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200097-0000-4002-a000-000000000097', '02200097-0000-4001-a000-000000000097', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200097-0000-4003-a000-000000000001', '02200097-0000-4002-a000-000000000097', 'MONDAY', '07:00:00', '09:00:00');

-- DIURNA 01 D · Profundización Visión Artificial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200098-0000-4001-a000-000000000098', 'c0080000-0000-4000-a000-000000000004', 'Profundización Visión Artificial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-318 Sala Inf. A; mapped to generic Profundización I slot; source sheet: Catálogo Diurna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200098-0000-4002-a000-000000000098', '02200098-0000-4001-a000-000000000098', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200098-0000-4003-a000-000000000001', '02200098-0000-4002-a000-000000000098', 'TUESDAY', '07:00:00', '09:00:00'),
('02200098-0000-4003-a000-000000000002', '02200098-0000-4002-a000-000000000098', 'WEDNESDAY', '09:00:00', '11:00:00'),
('02200098-0000-4003-a000-000000000003', '02200098-0000-4002-a000-000000000098', 'THURSDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Legislación y Propiedad Intelectual
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200099-0000-4001-a000-000000000099', 'c0090000-0000-4000-a000-000000000003', 'Legislación y Propiedad Intelectual', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-302 Maestría; source sheet: Catálogo Diurna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200099-0000-4002-a000-000000000099', '02200099-0000-4001-a000-000000000099', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200099-0000-4003-a000-000000000001', '02200099-0000-4002-a000-000000000099', 'THURSDAY', '07:00:00', '09:00:00');

-- DIURNA 02 D · Profundización Desarrollo de Aplicaciones para Dispositivos Móviles
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200100-0000-4001-a000-000000000100', 'c0080000-0000-4000-a000-000000000004', 'Profundización Desarrollo de Aplicaciones para Dispositivos Móviles', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-317 Sala Inf. B; mapped to generic Profundización I slot; source sheet: Catálogo Diurna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200100-0000-4002-a000-000000000100', '02200100-0000-4001-a000-000000000100', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200100-0000-4003-a000-000000000001', '02200100-0000-4002-a000-000000000100', 'MONDAY', '07:00:00', '09:00:00'),
('02200100-0000-4003-a000-000000000002', '02200100-0000-4002-a000-000000000100', 'WEDNESDAY', '09:00:00', '11:00:00');

-- DIURNA 02 D · Profundización Usabilidad y Experiencia de Usuario
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200101-0000-4001-a000-000000000101', 'c0080000-0000-4000-a000-000000000004', 'Profundización Usabilidad y Experiencia de Usuario', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-317 Sala Inf. B; mapped to generic Profundización I slot; source sheet: Catálogo Diurna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200101-0000-4002-a000-000000000101', '02200101-0000-4001-a000-000000000101', '02 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200101-0000-4003-a000-000000000001', '02200101-0000-4002-a000-000000000101', 'TUESDAY', '07:00:00', '09:00:00'),
('02200101-0000-4003-a000-000000000002', '02200101-0000-4002-a000-000000000101', 'THURSDAY', '09:00:00', '11:00:00');

-- DIURNA 01 D · Ética Profesional
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200102-0000-4001-a000-000000000102', 'c0100000-0000-4000-a000-000000000005', 'Ética Profesional', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302; locations/modalities include: D4-413; source sheet: Catálogo Diurna; level 10', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200102-0000-4002-a000-000000000102', '02200102-0000-4001-a000-000000000102', '01 D', 'DIURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200102-0000-4003-a000-000000000001', '02200102-0000-4002-a000-000000000102', 'TUESDAY', '07:00:00', '09:00:00'),
('02200102-0000-4003-a000-000000000002', '02200102-0000-4002-a000-000000000102', 'THURSDAY', '09:00:00', '11:00:00');

-- NOCTURNA 01 N · Introducción a la Ingeniería de Sistemas y Computación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200103-0000-4001-a000-000000000103', 'c0010000-0000-4000-a000-000000000002', 'Introducción a la Ingeniería de Sistemas y Computación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; locations/modalities include: Virtual; source sheet: Catálogo Nocturna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200103-0000-4002-a000-000000000103', '02200103-0000-4001-a000-000000000103', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200103-0000-4003-a000-000000000001', '02200103-0000-4002-a000-000000000103', 'TUESDAY', '20:00:00', '22:00:00'),
('02200103-0000-4003-a000-000000000002', '02200103-0000-4002-a000-000000000103', 'WEDNESDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Lectura y Escritura en Ingeniería
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200104-0000-4001-a000-000000000104', 'c0010000-0000-4000-a000-000000000005', 'Lectura y Escritura en Ingeniería', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; source sheet: Catálogo Nocturna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200104-0000-4002-a000-000000000104', '02200104-0000-4001-a000-000000000104', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200104-0000-4003-a000-000000000001', '02200104-0000-4002-a000-000000000104', 'MONDAY', '20:00:00', '22:00:00'),
('02200104-0000-4003-a000-000000000002', '02200104-0000-4002-a000-000000000104', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Lógica de Programación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200105-0000-4001-a000-000000000105', 'c0010000-0000-4000-a000-000000000004', 'Lógica de Programación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; source sheet: Catálogo Nocturna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200105-0000-4002-a000-000000000105', '02200105-0000-4001-a000-000000000105', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200105-0000-4003-a000-000000000001', '02200105-0000-4002-a000-000000000105', 'TUESDAY', '18:00:00', '20:00:00'),
('02200105-0000-4003-a000-000000000002', '02200105-0000-4002-a000-000000000105', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Matemáticas Generales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200106-0000-4001-a000-000000000106', 'c0010000-0000-4000-a000-000000000001', 'Matemáticas Generales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; source sheet: Catálogo Nocturna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200106-0000-4002-a000-000000000106', '02200106-0000-4001-a000-000000000106', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200106-0000-4003-a000-000000000001', '02200106-0000-4002-a000-000000000106', 'MONDAY', '18:00:00', '20:00:00'),
('02200106-0000-4003-a000-000000000002', '02200106-0000-4002-a000-000000000106', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Introducción a la Ingeniería de Sistemas y Computación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200107-0000-4001-a000-000000000107', 'c0010000-0000-4000-a000-000000000002', 'Introducción a la Ingeniería de Sistemas y Computación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-402; locations/modalities include: Virtual; source sheet: Catálogo Nocturna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200107-0000-4002-a000-000000000107', '02200107-0000-4001-a000-000000000107', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200107-0000-4003-a000-000000000001', '02200107-0000-4002-a000-000000000107', 'TUESDAY', '20:00:00', '22:00:00'),
('02200107-0000-4003-a000-000000000002', '02200107-0000-4002-a000-000000000107', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Lectura y Escritura en Ingeniería
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200108-0000-4001-a000-000000000108', 'c0010000-0000-4000-a000-000000000005', 'Lectura y Escritura en Ingeniería', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-402; source sheet: Catálogo Nocturna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200108-0000-4002-a000-000000000108', '02200108-0000-4001-a000-000000000108', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200108-0000-4003-a000-000000000001', '02200108-0000-4002-a000-000000000108', 'MONDAY', '18:00:00', '20:00:00'),
('02200108-0000-4003-a000-000000000002', '02200108-0000-4002-a000-000000000108', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Lógica de Programación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200109-0000-4001-a000-000000000109', 'c0010000-0000-4000-a000-000000000004', 'Lógica de Programación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-402; source sheet: Catálogo Nocturna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200109-0000-4002-a000-000000000109', '02200109-0000-4001-a000-000000000109', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200109-0000-4003-a000-000000000001', '02200109-0000-4002-a000-000000000109', 'TUESDAY', '18:00:00', '20:00:00'),
('02200109-0000-4003-a000-000000000002', '02200109-0000-4002-a000-000000000109', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 02 N · Matemáticas Generales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200110-0000-4001-a000-000000000110', 'c0010000-0000-4000-a000-000000000001', 'Matemáticas Generales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-402; source sheet: Catálogo Nocturna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200110-0000-4002-a000-000000000110', '02200110-0000-4001-a000-000000000110', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200110-0000-4003-a000-000000000001', '02200110-0000-4002-a000-000000000110', 'MONDAY', '20:00:00', '22:00:00'),
('02200110-0000-4003-a000-000000000002', '02200110-0000-4002-a000-000000000110', 'WEDNESDAY', '18:00:00', '20:00:00');

-- NOCTURNA 03 N · Lógica de Programación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200111-0000-4001-a000-000000000111', 'c0010000-0000-4000-a000-000000000004', 'Lógica de Programación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-303 Maestría; locations/modalities include: D4-406; source sheet: Catálogo Nocturna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200111-0000-4002-a000-000000000111', '02200111-0000-4001-a000-000000000111', '03 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200111-0000-4003-a000-000000000001', '02200111-0000-4002-a000-000000000111', 'MONDAY', '18:00:00', '20:00:00'),
('02200111-0000-4003-a000-000000000002', '02200111-0000-4002-a000-000000000111', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 03 N · Matemáticas Generales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200112-0000-4001-a000-000000000112', 'c0010000-0000-4000-a000-000000000001', 'Matemáticas Generales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; source sheet: Catálogo Nocturna; level 1', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200112-0000-4002-a000-000000000112', '02200112-0000-4001-a000-000000000112', '03 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200112-0000-4003-a000-000000000001', '02200112-0000-4002-a000-000000000112', 'TUESDAY', '20:00:00', '22:00:00'),
('02200112-0000-4003-a000-000000000002', '02200112-0000-4002-a000-000000000112', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Álgebra Lineal
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200113-0000-4001-a000-000000000113', 'c0020000-0000-4000-a000-000000000002', 'Álgebra Lineal', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-413; source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200113-0000-4002-a000-000000000113', '02200113-0000-4001-a000-000000000113', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200113-0000-4003-a000-000000000001', '02200113-0000-4002-a000-000000000113', 'WEDNESDAY', '18:00:00', '20:00:00'),
('02200113-0000-4003-a000-000000000002', '02200113-0000-4002-a000-000000000113', 'FRIDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Cálculo Diferencial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200114-0000-4001-a000-000000000114', 'c0020000-0000-4000-a000-000000000001', 'Cálculo Diferencial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-409; source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200114-0000-4002-a000-000000000114', '02200114-0000-4001-a000-000000000114', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200114-0000-4003-a000-000000000001', '02200114-0000-4002-a000-000000000114', 'MONDAY', '18:00:00', '20:00:00'),
('02200114-0000-4003-a000-000000000002', '02200114-0000-4002-a000-000000000114', 'TUESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Fundamentos de Electricidad, Electrónica y Telecomunicaciones
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200115-0000-4001-a000-000000000115', 'c0020000-0000-4000-a000-000000000005', 'Fundamentos de Electricidad, Electrónica y Telecomunicaciones', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-409; source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200115-0000-4002-a000-000000000115', '02200115-0000-4001-a000-000000000115', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200115-0000-4003-a000-000000000001', '02200115-0000-4002-a000-000000000115', 'FRIDAY', '16:00:00', '18:00:00'),
('02200115-0000-4003-a000-000000000002', '02200115-0000-4002-a000-000000000115', 'FRIDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Pensamiento Sistémico
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200116-0000-4001-a000-000000000116', 'c0020000-0000-4000-a000-000000000004', 'Pensamiento Sistémico', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-409; locations/modalities include: Virtual; source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200116-0000-4002-a000-000000000116', '02200116-0000-4001-a000-000000000116', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200116-0000-4003-a000-000000000001', '02200116-0000-4002-a000-000000000116', 'WEDNESDAY', '20:00:00', '22:00:00'),
('02200116-0000-4003-a000-000000000002', '02200116-0000-4002-a000-000000000116', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Programación I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200117-0000-4001-a000-000000000117', 'c0020000-0000-4000-a000-000000000003', 'Programación I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-409; locations/modalities include: D4-410; source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200117-0000-4002-a000-000000000117', '02200117-0000-4001-a000-000000000117', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200117-0000-4003-a000-000000000001', '02200117-0000-4002-a000-000000000117', 'TUESDAY', '18:00:00', '20:00:00'),
('02200117-0000-4003-a000-000000000002', '02200117-0000-4002-a000-000000000117', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Álgebra Lineal
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200118-0000-4001-a000-000000000118', 'c0020000-0000-4000-a000-000000000002', 'Álgebra Lineal', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-406; source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200118-0000-4002-a000-000000000118', '02200118-0000-4001-a000-000000000118', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200118-0000-4003-a000-000000000001', '02200118-0000-4002-a000-000000000118', 'MONDAY', '18:00:00', '20:00:00'),
('02200118-0000-4003-a000-000000000002', '02200118-0000-4002-a000-000000000118', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Cálculo Diferencial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200119-0000-4001-a000-000000000119', 'c0020000-0000-4000-a000-000000000001', 'Cálculo Diferencial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-406; locations/modalities include: D4-409; source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200119-0000-4002-a000-000000000119', '02200119-0000-4001-a000-000000000119', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200119-0000-4003-a000-000000000001', '02200119-0000-4002-a000-000000000119', 'TUESDAY', '20:00:00', '22:00:00'),
('02200119-0000-4003-a000-000000000002', '02200119-0000-4002-a000-000000000119', 'WEDNESDAY', '18:00:00', '20:00:00'),
('02200119-0000-4003-a000-000000000003', '02200119-0000-4002-a000-000000000119', 'FRIDAY', '18:00:00', '20:00:00');

-- NOCTURNA 02 N · Fundamentos de Electricidad, Electrónica y Telecomunicaciones
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200120-0000-4001-a000-000000000120', 'c0020000-0000-4000-a000-000000000005', 'Fundamentos de Electricidad, Electrónica y Telecomunicaciones', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-402; source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200120-0000-4002-a000-000000000120', '02200120-0000-4001-a000-000000000120', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200120-0000-4003-a000-000000000001', '02200120-0000-4002-a000-000000000120', 'FRIDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Pensamiento Sistémico
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200121-0000-4001-a000-000000000121', 'c0020000-0000-4000-a000-000000000004', 'Pensamiento Sistémico', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-411; locations/modalities include: Virtual; source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200121-0000-4002-a000-000000000121', '02200121-0000-4001-a000-000000000121', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200121-0000-4003-a000-000000000001', '02200121-0000-4002-a000-000000000121', 'MONDAY', '20:00:00', '22:00:00'),
('02200121-0000-4003-a000-000000000002', '02200121-0000-4002-a000-000000000121', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 02 N · Programación I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200122-0000-4001-a000-000000000122', 'c0020000-0000-4000-a000-000000000003', 'Programación I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-410; source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200122-0000-4002-a000-000000000122', '02200122-0000-4001-a000-000000000122', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200122-0000-4003-a000-000000000001', '02200122-0000-4002-a000-000000000122', 'TUESDAY', '18:00:00', '20:00:00'),
('02200122-0000-4003-a000-000000000002', '02200122-0000-4002-a000-000000000122', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 03 N · Cálculo Diferencial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200123-0000-4001-a000-000000000123', 'c0020000-0000-4000-a000-000000000001', 'Cálculo Diferencial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: D4-410; source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200123-0000-4002-a000-000000000123', '02200123-0000-4001-a000-000000000123', '03 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200123-0000-4003-a000-000000000001', '02200123-0000-4002-a000-000000000123', 'MONDAY', '18:00:00', '20:00:00'),
('02200123-0000-4003-a000-000000000002', '02200123-0000-4002-a000-000000000123', 'TUESDAY', '20:00:00', '22:00:00'),
('02200123-0000-4003-a000-000000000003', '02200123-0000-4002-a000-000000000123', 'FRIDAY', '20:00:00', '22:00:00');

-- NOCTURNA 03 N · Programación I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200124-0000-4001-a000-000000000124', 'c0020000-0000-4000-a000-000000000003', 'Programación I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'source sheet: Catálogo Nocturna; level 2', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200124-0000-4002-a000-000000000124', '02200124-0000-4001-a000-000000000124', '03 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200124-0000-4003-a000-000000000001', '02200124-0000-4002-a000-000000000124', 'WEDNESDAY', '18:00:00', '20:00:00'),
('02200124-0000-4003-a000-000000000002', '02200124-0000-4002-a000-000000000124', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Cálculo Integral
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200125-0000-4001-a000-000000000125', 'c0030000-0000-4000-a000-000000000001', 'Cálculo Integral', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-413; source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200125-0000-4002-a000-000000000125', '02200125-0000-4001-a000-000000000125', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200125-0000-4003-a000-000000000001', '02200125-0000-4002-a000-000000000125', 'MONDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Cálculo Integral (cada 15 días)
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200126-0000-4001-a000-000000000126', 'c0030000-0000-4000-a000-000000000001', 'Cálculo Integral (cada 15 días)', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200126-0000-4002-a000-000000000126', '02200126-0000-4001-a000-000000000126', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200126-0000-4003-a000-000000000001', '02200126-0000-4002-a000-000000000126', 'MONDAY', '13:00:00', '14:00:00');

-- NOCTURNA 01 N · Física General
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200127-0000-4001-a000-000000000127', 'c0030000-0000-4000-a000-000000000002', 'Física General', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200127-0000-4002-a000-000000000127', '02200127-0000-4001-a000-000000000127', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200127-0000-4003-a000-000000000001', '02200127-0000-4002-a000-000000000127', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Física General — Práctica Lab. 02
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200128-0000-4001-a000-000000000128', 'c0030000-0000-4000-a000-000000000002', 'Física General — Práctica Lab. 02', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: Lab. Física B Ciencias Básicas; source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200128-0000-4002-a000-000000000128', '02200128-0000-4001-a000-000000000128', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200128-0000-4003-a000-000000000001', '02200128-0000-4002-a000-000000000128', 'SATURDAY', '16:00:00', '18:00:00');

-- NOCTURNA 01 N · Matemáticas Discretas
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200129-0000-4001-a000-000000000129', 'c0030000-0000-4000-a000-000000000003', 'Matemáticas Discretas', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-303 Maestría; locations/modalities include: D4-405; source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200129-0000-4002-a000-000000000129', '02200129-0000-4001-a000-000000000129', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200129-0000-4003-a000-000000000001', '02200129-0000-4002-a000-000000000129', 'TUESDAY', '20:00:00', '22:00:00'),
('02200129-0000-4003-a000-000000000002', '02200129-0000-4002-a000-000000000129', 'WEDNESDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Programación II
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200130-0000-4001-a000-000000000130', 'c0030000-0000-4000-a000-000000000005', 'Programación II', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200130-0000-4002-a000-000000000130', '02200130-0000-4001-a000-000000000130', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200130-0000-4003-a000-000000000001', '02200130-0000-4002-a000-000000000130', 'TUESDAY', '18:00:00', '20:00:00'),
('02200130-0000-4003-a000-000000000002', '02200130-0000-4002-a000-000000000130', 'FRIDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Teoría y Diseño Organizacional
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200131-0000-4001-a000-000000000131', 'c0030000-0000-4000-a000-000000000004', 'Teoría y Diseño Organizacional', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200131-0000-4002-a000-000000000131', '02200131-0000-4001-a000-000000000131', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200131-0000-4003-a000-000000000001', '02200131-0000-4002-a000-000000000131', 'WEDNESDAY', '20:00:00', '22:00:00'),
('02200131-0000-4003-a000-000000000002', '02200131-0000-4002-a000-000000000131', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 02 N · Cálculo Integral
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200132-0000-4001-a000-000000000132', 'c0030000-0000-4000-a000-000000000001', 'Cálculo Integral', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; locations/modalities include: D4-408; source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200132-0000-4002-a000-000000000132', '02200132-0000-4001-a000-000000000132', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200132-0000-4003-a000-000000000001', '02200132-0000-4002-a000-000000000132', 'MONDAY', '18:00:00', '20:00:00'),
('02200132-0000-4003-a000-000000000002', '02200132-0000-4002-a000-000000000132', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Cálculo Integral (cada 15 días)
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200133-0000-4001-a000-000000000133', 'c0030000-0000-4000-a000-000000000001', 'Cálculo Integral (cada 15 días)', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200133-0000-4002-a000-000000000133', '02200133-0000-4001-a000-000000000133', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200133-0000-4003-a000-000000000001', '02200133-0000-4002-a000-000000000133', 'MONDAY', '13:00:00', '14:00:00');

-- NOCTURNA 02 N · Física General — Práctica Lab. 01
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200134-0000-4001-a000-000000000134', 'c0030000-0000-4000-a000-000000000002', 'Física General — Práctica Lab. 01', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: Lab. Física B Ciencias Básicas; source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200134-0000-4002-a000-000000000134', '02200134-0000-4001-a000-000000000134', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200134-0000-4003-a000-000000000001', '02200134-0000-4002-a000-000000000134', 'SATURDAY', '14:00:00', '16:00:00');

-- NOCTURNA 02 N · Matemáticas Discretas
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200135-0000-4001-a000-000000000135', 'c0030000-0000-4000-a000-000000000003', 'Matemáticas Discretas', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-408; source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200135-0000-4002-a000-000000000135', '02200135-0000-4001-a000-000000000135', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200135-0000-4003-a000-000000000001', '02200135-0000-4002-a000-000000000135', 'MONDAY', '20:00:00', '22:00:00'),
('02200135-0000-4003-a000-000000000002', '02200135-0000-4002-a000-000000000135', 'WEDNESDAY', '18:00:00', '20:00:00');

-- NOCTURNA 02 N · Programación II
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200136-0000-4001-a000-000000000136', 'c0030000-0000-4000-a000-000000000005', 'Programación II', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D2-114 Medicina; source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200136-0000-4002-a000-000000000136', '02200136-0000-4001-a000-000000000136', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200136-0000-4003-a000-000000000001', '02200136-0000-4002-a000-000000000136', 'TUESDAY', '18:00:00', '20:00:00'),
('02200136-0000-4003-a000-000000000002', '02200136-0000-4002-a000-000000000136', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Teoría y Diseño Organizacional
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200137-0000-4001-a000-000000000137', 'c0030000-0000-4000-a000-000000000004', 'Teoría y Diseño Organizacional', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-404; locations/modalities include: D4-408; source sheet: Catálogo Nocturna; level 3', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200137-0000-4002-a000-000000000137', '02200137-0000-4001-a000-000000000137', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200137-0000-4003-a000-000000000001', '02200137-0000-4002-a000-000000000137', 'TUESDAY', '20:00:00', '22:00:00'),
('02200137-0000-4003-a000-000000000002', '02200137-0000-4002-a000-000000000137', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Cálculo Multivariado Vectorial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200138-0000-4001-a000-000000000138', 'c0040000-0000-4000-a000-000000000001', 'Cálculo Multivariado Vectorial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D2-114 Medicina; locations/modalities include: D4-410; source sheet: Catálogo Nocturna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200138-0000-4002-a000-000000000138', '02200138-0000-4001-a000-000000000138', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200138-0000-4003-a000-000000000001', '02200138-0000-4002-a000-000000000138', 'MONDAY', '18:00:00', '20:00:00'),
('02200138-0000-4003-a000-000000000002', '02200138-0000-4002-a000-000000000138', 'MONDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Ecuaciones Diferenciales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200139-0000-4001-a000-000000000139', 'c0040000-0000-4000-a000-000000000002', 'Ecuaciones Diferenciales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D2-114 Medicina; source sheet: Catálogo Nocturna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200139-0000-4002-a000-000000000139', '02200139-0000-4001-a000-000000000139', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200139-0000-4003-a000-000000000001', '02200139-0000-4002-a000-000000000139', 'WEDNESDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Fundamentos de Infraestructura Computacional
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200140-0000-4001-a000-000000000140', 'c0040000-0000-4000-a000-000000000006', 'Fundamentos de Infraestructura Computacional', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D2-114 Medicina; source sheet: Catálogo Nocturna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200140-0000-4002-a000-000000000140', '02200140-0000-4001-a000-000000000140', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200140-0000-4003-a000-000000000001', '02200140-0000-4002-a000-000000000140', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Programación III
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200141-0000-4001-a000-000000000141', 'c0040000-0000-4000-a000-000000000004', 'Programación III', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302 Maestría; locations/modalities include: D4-410; source sheet: Catálogo Nocturna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200141-0000-4002-a000-000000000141', '02200141-0000-4001-a000-000000000141', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200141-0000-4003-a000-000000000001', '02200141-0000-4002-a000-000000000141', 'TUESDAY', '18:00:00', '20:00:00'),
('02200141-0000-4003-a000-000000000002', '02200141-0000-4002-a000-000000000141', 'TUESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Cálculo Multivariado Vectorial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200142-0000-4001-a000-000000000142', 'c0040000-0000-4000-a000-000000000001', 'Cálculo Multivariado Vectorial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D2-114 Medicina; locations/modalities include: D4-411; source sheet: Catálogo Nocturna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200142-0000-4002-a000-000000000142', '02200142-0000-4001-a000-000000000142', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200142-0000-4003-a000-000000000001', '02200142-0000-4002-a000-000000000142', 'TUESDAY', '18:00:00', '20:00:00'),
('02200142-0000-4003-a000-000000000002', '02200142-0000-4002-a000-000000000142', 'WEDNESDAY', '20:00:00', '22:00:00'),
('02200142-0000-4003-a000-000000000003', '02200142-0000-4002-a000-000000000142', 'FRIDAY', '18:00:00', '20:00:00');

-- NOCTURNA 02 N · Ecuaciones Diferenciales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200143-0000-4001-a000-000000000143', 'c0040000-0000-4000-a000-000000000002', 'Ecuaciones Diferenciales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D2-114 Medicina; source sheet: Catálogo Nocturna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200143-0000-4002-a000-000000000143', '02200143-0000-4001-a000-000000000143', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200143-0000-4003-a000-000000000001', '02200143-0000-4002-a000-000000000143', 'MONDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Fundamentos de Infraestructura Computacional
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200144-0000-4001-a000-000000000144', 'c0040000-0000-4000-a000-000000000006', 'Fundamentos de Infraestructura Computacional', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'source sheet: Catálogo Nocturna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200144-0000-4002-a000-000000000144', '02200144-0000-4001-a000-000000000144', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200144-0000-4003-a000-000000000001', '02200144-0000-4002-a000-000000000144', 'FRIDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Programación III
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200145-0000-4001-a000-000000000145', 'c0040000-0000-4000-a000-000000000004', 'Programación III', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-205; locations/modalities include: D4-407; source sheet: Catálogo Nocturna; level 4', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200145-0000-4002-a000-000000000145', '02200145-0000-4001-a000-000000000145', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200145-0000-4003-a000-000000000001', '02200145-0000-4002-a000-000000000145', 'WEDNESDAY', '18:00:00', '20:00:00'),
('02200145-0000-4003-a000-000000000002', '02200145-0000-4002-a000-000000000145', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Estadística y Probabilidad
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200146-0000-4001-a000-000000000146', 'c0050000-0000-4000-a000-000000000001', 'Estadística y Probabilidad', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-410; locations/modalities include: D4-411; source sheet: Catálogo Nocturna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200146-0000-4002-a000-000000000146', '02200146-0000-4001-a000-000000000146', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200146-0000-4003-a000-000000000001', '02200146-0000-4002-a000-000000000146', 'MONDAY', '20:00:00', '22:00:00'),
('02200146-0000-4003-a000-000000000002', '02200146-0000-4002-a000-000000000146', 'WEDNESDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Estructura de Datos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200147-0000-4001-a000-000000000147', 'c0050000-0000-4000-a000-000000000004', 'Estructura de Datos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-407; locations/modalities include: D4-413; source sheet: Catálogo Nocturna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200147-0000-4002-a000-000000000147', '02200147-0000-4001-a000-000000000147', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200147-0000-4003-a000-000000000001', '02200147-0000-4002-a000-000000000147', 'TUESDAY', '18:00:00', '20:00:00'),
('02200147-0000-4003-a000-000000000002', '02200147-0000-4002-a000-000000000147', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Sistema de Información
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200148-0000-4001-a000-000000000148', 'c0040000-0000-4000-a000-000000000005', 'Sistema de Información', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'alias mapped to catalog course; locations/modalities include: D4-303 Maestría; locations/modalities include: D4-411; source sheet: Catálogo Nocturna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200148-0000-4002-a000-000000000148', '02200148-0000-4001-a000-000000000148', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200148-0000-4003-a000-000000000001', '02200148-0000-4002-a000-000000000148', 'TUESDAY', '20:00:00', '22:00:00'),
('02200148-0000-4003-a000-000000000002', '02200148-0000-4002-a000-000000000148', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Teoría de Grafos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200149-0000-4001-a000-000000000149', 'c0050000-0000-4000-a000-000000000002', 'Teoría de Grafos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302 Maestría; source sheet: Catálogo Nocturna; level 5', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200149-0000-4002-a000-000000000149', '02200149-0000-4001-a000-000000000149', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200149-0000-4003-a000-000000000001', '02200149-0000-4002-a000-000000000149', 'MONDAY', '18:00:00', '20:00:00'),
('02200149-0000-4003-a000-000000000002', '02200149-0000-4002-a000-000000000149', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Análisis Numérico
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200150-0000-4001-a000-000000000150', 'c0060000-0000-4000-a000-000000000002', 'Análisis Numérico', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: D4-406; source sheet: Catálogo Nocturna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200150-0000-4002-a000-000000000150', '02200150-0000-4001-a000-000000000150', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200150-0000-4003-a000-000000000001', '02200150-0000-4002-a000-000000000150', 'WEDNESDAY', '18:00:00', '20:00:00'),
('02200150-0000-4003-a000-000000000002', '02200150-0000-4002-a000-000000000150', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Bases de Datos I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200151-0000-4001-a000-000000000151', 'c0060000-0000-4000-a000-000000000003', 'Bases de Datos I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-411; locations/modalities include: D4-411 Maestría; source sheet: Catálogo Nocturna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200151-0000-4002-a000-000000000151', '02200151-0000-4001-a000-000000000151', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200151-0000-4003-a000-000000000001', '02200151-0000-4002-a000-000000000151', 'TUESDAY', '18:00:00', '20:00:00'),
('02200151-0000-4003-a000-000000000002', '02200151-0000-4002-a000-000000000151', 'TUESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Dirección Estratégica de T.I.
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200152-0000-4001-a000-000000000152', 'c0060000-0000-4000-a000-000000000001', 'Dirección Estratégica de T.I.', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-405; locations/modalities include: D4-410; source sheet: Catálogo Nocturna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200152-0000-4002-a000-000000000152', '02200152-0000-4001-a000-000000000152', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200152-0000-4003-a000-000000000001', '02200152-0000-4002-a000-000000000152', 'THURSDAY', '18:00:00', '20:00:00'),
('02200152-0000-4003-a000-000000000002', '02200152-0000-4002-a000-000000000152', 'FRIDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Infraestructura Computacional — Práctica
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200153-0000-4001-a000-000000000153', 'c0040000-0000-4000-a000-000000000006', 'Infraestructura Computacional — Práctica', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; source sheet: Catálogo Nocturna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200153-0000-4002-a000-000000000153', '02200153-0000-4001-a000-000000000153', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200153-0000-4003-a000-000000000001', '02200153-0000-4002-a000-000000000153', 'SATURDAY', '14:00:00', '16:00:00');

-- NOCTURNA 01 N · Infraestructura Computacional — Teoría
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200154-0000-4001-a000-000000000154', 'c0040000-0000-4000-a000-000000000006', 'Infraestructura Computacional — Teoría', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: D4-405; locations/modalities include: D4-406; source sheet: Catálogo Nocturna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200154-0000-4002-a000-000000000154', '02200154-0000-4001-a000-000000000154', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200154-0000-4003-a000-000000000001', '02200154-0000-4002-a000-000000000154', 'FRIDAY', '18:00:00', '20:00:00'),
('02200154-0000-4003-a000-000000000002', '02200154-0000-4002-a000-000000000154', 'MONDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Ingeniería de Software I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200155-0000-4001-a000-000000000155', 'c0050000-0000-4000-a000-000000000005', 'Ingeniería de Software I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-407; source sheet: Catálogo Nocturna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200155-0000-4002-a000-000000000155', '02200155-0000-4001-a000-000000000155', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200155-0000-4003-a000-000000000001', '02200155-0000-4002-a000-000000000155', 'MONDAY', '18:00:00', '20:00:00'),
('02200155-0000-4003-a000-000000000002', '02200155-0000-4002-a000-000000000155', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Ingeniería de Software I
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200156-0000-4001-a000-000000000156', 'c0050000-0000-4000-a000-000000000005', 'Ingeniería de Software I', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'source sheet: Catálogo Nocturna; level 6', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200156-0000-4002-a000-000000000156', '02200156-0000-4001-a000-000000000156', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200156-0000-4003-a000-000000000001', '02200156-0000-4002-a000-000000000156', 'MONDAY', '18:00:00', '20:00:00'),
('02200156-0000-4003-a000-000000000002', '02200156-0000-4002-a000-000000000156', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Bases de Datos II
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200157-0000-4001-a000-000000000157', 'c0070000-0000-4000-a000-000000000004', 'Bases de Datos II', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: D4-405; source sheet: Catálogo Nocturna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200157-0000-4002-a000-000000000157', '02200157-0000-4001-a000-000000000157', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200157-0000-4003-a000-000000000001', '02200157-0000-4002-a000-000000000157', 'TUESDAY', '18:00:00', '20:00:00'),
('02200157-0000-4003-a000-000000000002', '02200157-0000-4002-a000-000000000157', 'TUESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Infraestructura de Comunicaciones — Práctica
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200158-0000-4001-a000-000000000158', 'c0060000-0000-4000-a000-000000000006', 'Infraestructura de Comunicaciones — Práctica', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; source sheet: Catálogo Nocturna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200158-0000-4002-a000-000000000158', '02200158-0000-4001-a000-000000000158', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200158-0000-4003-a000-000000000001', '02200158-0000-4002-a000-000000000158', 'SATURDAY', '14:00:00', '16:00:00');

-- NOCTURNA 01 N · Infraestructura de Comunicaciones — Teoría
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200159-0000-4001-a000-000000000159', 'c0060000-0000-4000-a000-000000000006', 'Infraestructura de Comunicaciones — Teoría', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'component row mapped to parent course; locations/modalities include: D4-402; locations/modalities include: D4-407; source sheet: Catálogo Nocturna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200159-0000-4002-a000-000000000159', '02200159-0000-4001-a000-000000000159', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200159-0000-4003-a000-000000000001', '02200159-0000-4002-a000-000000000159', 'FRIDAY', '18:00:00', '20:00:00'),
('02200159-0000-4003-a000-000000000002', '02200159-0000-4002-a000-000000000159', 'FRIDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Ingeniería de Software II
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200160-0000-4001-a000-000000000160', 'c0060000-0000-4000-a000-000000000005', 'Ingeniería de Software II', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-317 Sala Inf. B; source sheet: Catálogo Nocturna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200160-0000-4002-a000-000000000160', '02200160-0000-4001-a000-000000000160', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200160-0000-4003-a000-000000000001', '02200160-0000-4002-a000-000000000160', 'MONDAY', '18:00:00', '20:00:00'),
('02200160-0000-4003-a000-000000000002', '02200160-0000-4002-a000-000000000160', 'MONDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Programación Avanzada
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200161-0000-4001-a000-000000000161', 'c0060000-0000-4000-a000-000000000004', 'Programación Avanzada', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-411; source sheet: Catálogo Nocturna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200161-0000-4002-a000-000000000161', '02200161-0000-4001-a000-000000000161', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200161-0000-4003-a000-000000000001', '02200161-0000-4002-a000-000000000161', 'WEDNESDAY', '18:00:00', '20:00:00'),
('02200161-0000-4003-a000-000000000002', '02200161-0000-4002-a000-000000000161', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Teoría de Lenguajes Formales
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200162-0000-4001-a000-000000000162', 'c0080000-0000-4000-a000-000000000002', 'Teoría de Lenguajes Formales', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: D4-407; source sheet: Catálogo Nocturna; level 7', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200162-0000-4002-a000-000000000162', '02200162-0000-4001-a000-000000000162', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200162-0000-4003-a000-000000000001', '02200162-0000-4002-a000-000000000162', 'THURSDAY', '18:00:00', '20:00:00'),
('02200162-0000-4003-a000-000000000002', '02200162-0000-4002-a000-000000000162', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Administración de Infraestructura de T.I.
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200163-0000-4001-a000-000000000163', 'c0070000-0000-4000-a000-000000000006', 'Administración de Infraestructura de T.I.', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; locations/modalities include: D4-406; source sheet: Catálogo Nocturna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200163-0000-4002-a000-000000000163', '02200163-0000-4001-a000-000000000163', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200163-0000-4003-a000-000000000001', '02200163-0000-4002-a000-000000000163', 'TUESDAY', '18:00:00', '20:00:00'),
('02200163-0000-4003-a000-000000000002', '02200163-0000-4002-a000-000000000163', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Fundamentos de HCI
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200164-0000-4001-a000-000000000164', 'c0070000-0000-4000-a000-000000000002', 'Fundamentos de HCI', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-401; locations/modalities include: D4-403; source sheet: Catálogo Nocturna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200164-0000-4002-a000-000000000164', '02200164-0000-4001-a000-000000000164', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200164-0000-4003-a000-000000000001', '02200164-0000-4002-a000-000000000164', 'TUESDAY', '20:00:00', '22:00:00'),
('02200164-0000-4003-a000-000000000002', '02200164-0000-4002-a000-000000000164', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Ingeniería de Software III
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200165-0000-4001-a000-000000000165', 'c0070000-0000-4000-a000-000000000005', 'Ingeniería de Software III', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-405; locations/modalities include: D4-411; source sheet: Catálogo Nocturna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200165-0000-4002-a000-000000000165', '02200165-0000-4001-a000-000000000165', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200165-0000-4003-a000-000000000001', '02200165-0000-4002-a000-000000000165', 'MONDAY', '18:00:00', '20:00:00'),
('02200165-0000-4003-a000-000000000002', '02200165-0000-4002-a000-000000000165', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Ingeniería Económica
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200166-0000-4001-a000-000000000166', 'c0070000-0000-4000-a000-000000000001', 'Ingeniería Económica', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-404; locations/modalities include: D4-407; source sheet: Catálogo Nocturna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200166-0000-4002-a000-000000000166', '02200166-0000-4001-a000-000000000166', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200166-0000-4003-a000-000000000001', '02200166-0000-4002-a000-000000000166', 'WEDNESDAY', '18:00:00', '20:00:00'),
('02200166-0000-4003-a000-000000000002', '02200166-0000-4002-a000-000000000166', 'FRIDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Seminario de Investigación
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200167-0000-4001-a000-000000000167', 'c0070000-0000-4000-a000-000000000003', 'Seminario de Investigación', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-303 Maestría; locations/modalities include: D4-401; source sheet: Catálogo Nocturna; level 8', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200167-0000-4002-a000-000000000167', '02200167-0000-4001-a000-000000000167', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200167-0000-4003-a000-000000000001', '02200167-0000-4002-a000-000000000167', 'MONDAY', '20:00:00', '22:00:00'),
('02200167-0000-4003-a000-000000000002', '02200167-0000-4002-a000-000000000167', 'FRIDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Administración
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200168-0000-4001-a000-000000000168', 'c0090000-0000-4000-a000-000000000006', 'Administración', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-405; locations/modalities include: D4-413; source sheet: Catálogo Nocturna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200168-0000-4002-a000-000000000168', '02200168-0000-4001-a000-000000000168', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200168-0000-4003-a000-000000000001', '02200168-0000-4002-a000-000000000168', 'TUESDAY', '20:00:00', '22:00:00'),
('02200168-0000-4003-a000-000000000002', '02200168-0000-4002-a000-000000000168', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Análisis de Algoritmos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200169-0000-4001-a000-000000000169', 'c0080000-0000-4000-a000-000000000003', 'Análisis de Algoritmos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-405; locations/modalities include: D4-410; source sheet: Catálogo Nocturna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200169-0000-4002-a000-000000000169', '02200169-0000-4001-a000-000000000169', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200169-0000-4003-a000-000000000001', '02200169-0000-4002-a000-000000000169', 'MONDAY', '20:00:00', '22:00:00'),
('02200169-0000-4003-a000-000000000002', '02200169-0000-4002-a000-000000000169', 'WEDNESDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Formulación y Evaluación de Proyectos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200170-0000-4001-a000-000000000170', 'c0080000-0000-4000-a000-000000000001', 'Formulación y Evaluación de Proyectos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-403; source sheet: Catálogo Nocturna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200170-0000-4002-a000-000000000170', '02200170-0000-4001-a000-000000000170', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200170-0000-4003-a000-000000000001', '02200170-0000-4002-a000-000000000170', 'FRIDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Profundización - Introducción al Big Data
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200171-0000-4001-a000-000000000171', 'c0080000-0000-4000-a000-000000000004', 'Profundización - Introducción al Big Data', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'mapped to generic Profundización I slot; source sheet: Catálogo Nocturna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200171-0000-4002-a000-000000000171', '02200171-0000-4001-a000-000000000171', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200171-0000-4003-a000-000000000001', '02200171-0000-4002-a000-000000000171', 'TUESDAY', '18:00:00', '20:00:00'),
('02200171-0000-4003-a000-000000000002', '02200171-0000-4002-a000-000000000171', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Profundización - Visualización de Datos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200172-0000-4001-a000-000000000172', 'c0080000-0000-4000-a000-000000000004', 'Profundización - Visualización de Datos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-318 Sala Inf. A; locations/modalities include: D4-404; mapped to generic Profundización I slot; source sheet: Catálogo Nocturna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200172-0000-4002-a000-000000000172', '02200172-0000-4001-a000-000000000172', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200172-0000-4003-a000-000000000001', '02200172-0000-4002-a000-000000000172', 'MONDAY', '18:00:00', '20:00:00'),
('02200172-0000-4003-a000-000000000002', '02200172-0000-4002-a000-000000000172', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Profundización Computación en la Nube
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200173-0000-4001-a000-000000000173', 'c0080000-0000-4000-a000-000000000004', 'Profundización Computación en la Nube', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-318 Sala Inf. A; mapped to generic Profundización I slot; source sheet: Catálogo Nocturna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200173-0000-4002-a000-000000000173', '02200173-0000-4001-a000-000000000173', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200173-0000-4003-a000-000000000001', '02200173-0000-4002-a000-000000000173', 'TUESDAY', '18:00:00', '20:00:00');

-- NOCTURNA 02 N · Profundización Computación en la Nube (Sincrónico)
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200174-0000-4001-a000-000000000174', 'c0080000-0000-4000-a000-000000000004', 'Profundización Computación en la Nube (Sincrónico)', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: Encuentro Sincrónico; mapped to generic Profundización I slot; source sheet: Catálogo Nocturna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200174-0000-4002-a000-000000000174', '02200174-0000-4001-a000-000000000174', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200174-0000-4003-a000-000000000001', '02200174-0000-4002-a000-000000000174', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Profundización Diseño Centrado en el Usuario
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200175-0000-4001-a000-000000000175', 'c0080000-0000-4000-a000-000000000004', 'Profundización Diseño Centrado en el Usuario', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-205 Robótica 1; locations/modalities include: D4-318 Sala Inf. A; mapped to generic Profundización I slot; source sheet: Catálogo Nocturna; level 9', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200175-0000-4002-a000-000000000175', '02200175-0000-4001-a000-000000000175', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200175-0000-4003-a000-000000000001', '02200175-0000-4002-a000-000000000175', 'MONDAY', '18:00:00', '20:00:00'),
('02200175-0000-4003-a000-000000000002', '02200175-0000-4002-a000-000000000175', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Administración de Proyectos
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200176-0000-4001-a000-000000000176', 'c0090000-0000-4000-a000-000000000001', 'Administración de Proyectos', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-407; source sheet: Catálogo Nocturna; level 10', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200176-0000-4002-a000-000000000176', '02200176-0000-4001-a000-000000000176', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200176-0000-4003-a000-000000000001', '02200176-0000-4002-a000-000000000176', 'FRIDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Inteligencia Artificial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200177-0000-4001-a000-000000000177', 'c0090000-0000-4000-a000-000000000002', 'Inteligencia Artificial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302 Maestría; locations/modalities include: D4-317 Sala Inf. B; source sheet: Catálogo Nocturna; level 10', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200177-0000-4002-a000-000000000177', '02200177-0000-4001-a000-000000000177', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200177-0000-4003-a000-000000000001', '02200177-0000-4002-a000-000000000177', 'WEDNESDAY', '18:00:00', '20:00:00'),
('02200177-0000-4003-a000-000000000002', '02200177-0000-4002-a000-000000000177', 'MONDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Legislación Laboral y Propiedad Intelectual
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200178-0000-4001-a000-000000000178', 'c0090000-0000-4000-a000-000000000003', 'Legislación Laboral y Propiedad Intelectual', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-405; source sheet: Catálogo Nocturna; level 10', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200178-0000-4002-a000-000000000178', '02200178-0000-4001-a000-000000000178', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200178-0000-4003-a000-000000000001', '02200178-0000-4002-a000-000000000178', 'MONDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Profundización Usabilidad y Experiencia de Usuario
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200179-0000-4001-a000-000000000179', 'c0090000-0000-4000-a000-000000000004', 'Profundización Usabilidad y Experiencia de Usuario', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-408; mapped to generic Profundización III slot; source sheet: Catálogo Nocturna; level 10', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200179-0000-4002-a000-000000000179', '02200179-0000-4001-a000-000000000179', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200179-0000-4003-a000-000000000001', '02200179-0000-4002-a000-000000000179', 'TUESDAY', '18:00:00', '20:00:00'),
('02200179-0000-4003-a000-000000000002', '02200179-0000-4002-a000-000000000179', 'WEDNESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Profundización - Visión Artificial
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200180-0000-4001-a000-000000000180', 'c0090000-0000-4000-a000-000000000004', 'Profundización - Visión Artificial', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-302 Maestría; locations/modalities include: D4-303 Maestría; mapped to generic Profundización III slot; source sheet: Catálogo Nocturna; level 10', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200180-0000-4002-a000-000000000180', '02200180-0000-4001-a000-000000000180', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200180-0000-4003-a000-000000000001', '02200180-0000-4002-a000-000000000180', 'THURSDAY', '18:00:00', '20:00:00'),
('02200180-0000-4003-a000-000000000002', '02200180-0000-4002-a000-000000000180', 'TUESDAY', '20:00:00', '22:00:00');

-- NOCTURNA 01 N · Seminario de Ingeniería
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200181-0000-4001-a000-000000000181', 'c0080000-0000-4000-a000-000000000006', 'Seminario de Ingeniería', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-406; locations/modalities include: D4-407; source sheet: Catálogo Nocturna; level 10', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200181-0000-4002-a000-000000000181', '02200181-0000-4001-a000-000000000181', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200181-0000-4003-a000-000000000001', '02200181-0000-4002-a000-000000000181', 'FRIDAY', '18:00:00', '20:00:00'),
('02200181-0000-4003-a000-000000000002', '02200181-0000-4002-a000-000000000181', 'THURSDAY', '20:00:00', '22:00:00');

-- NOCTURNA 02 N · Profundización - Desarrollo de Aplicaciones para Dispositivos Móviles
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200182-0000-4001-a000-000000000182', 'c0090000-0000-4000-a000-000000000004', 'Profundización - Desarrollo de Aplicaciones para Dispositivos Móviles', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-408; mapped to generic Profundización III slot; source sheet: Catálogo Nocturna; level 10', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200182-0000-4002-a000-000000000182', '02200182-0000-4001-a000-000000000182', '02 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200182-0000-4003-a000-000000000001', '02200182-0000-4002-a000-000000000182', 'TUESDAY', '20:00:00', '22:00:00'),
('02200182-0000-4003-a000-000000000002', '02200182-0000-4002-a000-000000000182', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 03 N · Profundización Arquitectura Orientada a Microservicios
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200183-0000-4001-a000-000000000183', 'c0090000-0000-4000-a000-000000000004', 'Profundización Arquitectura Orientada a Microservicios', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-406; locations/modalities include: Encuentro Sincrónico; mapped to generic Profundización III slot; source sheet: Catálogo Nocturna; level 10', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200183-0000-4002-a000-000000000183', '02200183-0000-4001-a000-000000000183', '03 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200183-0000-4003-a000-000000000001', '02200183-0000-4002-a000-000000000183', 'TUESDAY', '20:00:00', '22:00:00'),
('02200183-0000-4003-a000-000000000002', '02200183-0000-4002-a000-000000000183', 'THURSDAY', '18:00:00', '20:00:00');

-- NOCTURNA 01 N · Ética Profesional
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('02200184-0000-4001-a000-000000000184', 'c0100000-0000-4000-a000-000000000005', 'Ética Profesional', '11111111-0000-4000-a000-000000000001', '00000001-0000-4000-a000-000000000001', 'FINAL', 'locations/modalities include: D4-405; source sheet: Catálogo Nocturna; level 11', 0);
INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('02200184-0000-4002-a000-000000000184', '02200184-0000-4001-a000-000000000184', '01 N', 'NOCTURNA', 0);
INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at) VALUES
('02200184-0000-4003-a000-000000000001', '02200184-0000-4002-a000-000000000184', 'MONDAY', '18:00:00', '20:00:00'),
('02200184-0000-4003-a000-000000000002', '02200184-0000-4002-a000-000000000184', 'WEDNESDAY', '20:00:00', '22:00:00');

-- Preserved Lenguas fixture from V017.
-- The workbook only contains Lenguas-like selections in the personal schedule sheet "Prueba 2026-2 (2)".
-- That sheet lacks group/catalog metadata, so using it as an official offering source would be data invention.
INSERT INTO offering.course_offering (id, course_id, course_name, program_id, period_id, status, notes, version)
VALUES ('00000008-0000-4001-b000-000000000001',
        'a0050000-0000-4000-b000-000000000003',
        'Habilidades Comunicativas e Integradas en Francés III',
        '11111111-0000-4000-b000-000000000001',
        '00000001-0000-4000-a000-000000000001',
        'PRELIMINARY',
        'Preserved V017 Lenguas fixture; workbook sheet Prueba 2026-2 (2) is a personal schedule, not a source catalog; level 5',
        0);

INSERT INTO offering.schedule_option (id, offering_id, label, journey_code, version)
VALUES ('00000008-0000-4002-b000-000000000001',
        '00000008-0000-4001-b000-000000000001',
        'Grupo A', 'NOCTURNA', 0);

INSERT INTO offering.schedule_session (id, schedule_option_id, day, starts_at, ends_at)
VALUES ('00000008-0000-4003-b000-000000000001', '00000008-0000-4002-b000-000000000001', 'WEDNESDAY', '18:00', '20:00');
