-- V027__fix_isc_prereqs_per_malla.sql
-- Forward-only migration. NEVER edit this file after it is committed.
--
-- Corrige tres aristas de prerrequisito del programa ISC (Plan 21) tras
-- verificación pixel a pixel de las flechas de la malla curricular oficial
-- (3-malla-curricular-2016-v091116-nocturno-nueva-reforma-1.jpg, Reforma 2016):
--
--   1. Inteligencia Artificial NO depende de Administración de Proyectos.
--      La flecha superior de IA nace en el fondo de Cálculo Multivariado y
--      Vectorial (elbow en x≈1315 → línea y≈602 → desciende en x≈3612 al
--      tope de IA). Entre el fondo de Adm. de Proyectos (y≈554) y esa línea
--      (y≈599) hay blanco continuo: no existe conector.
--      → Se INACTIVA  IA ← Administración de Proyectos (arista sin sustento).
--      → Se AGREGA    IA ← Cálculo Multivariado y Vectorial (HIGH).
--      Impacto real: con la arista errónea, IA quedaba bloqueada tras la
--      cadena Ing. Económica → Formulación → Adm. de Proyectos (≥3 semestres);
--      con la malla real, IA solo exige Análisis de Algoritmos (+ Cálculo
--      Multivariado, ya aprobado en cohortes avanzadas).
--
--   2. Bases de Datos II NO depende de Programación Avanzada.
--      La segunda flecha de BD II nace en el costado derecho de Dirección
--      Estratégica de T.I. (elbow x≈2232-2271 en y≈446 → vertical x≈2267
--      hasta y≈1135 → horizontal hasta el borde izquierdo de BD II x≈2640).
--      Además, en la malla nocturna Prog. Avanzada y BD II comparten el
--      semestre VII, de modo que esa arista sería insatisfacible siguiendo
--      la ruta recomendada.
--      → Se INACTIVA  BD II ← Programación Avanzada.
--      → Se AGREGA    BD II ← Dirección Estratégica de T.I. (HIGH).
--      (BD II ← Bases de Datos I se conserva intacta.)
--
--   3. Formulación y Evaluación de Proyectos también exige Seminario de
--      Investigación. El tronco vertical x≈3058 (y 464→1357) une el costado
--      de Seminario de Investigación con la flecha que entra a Formulación.
--      → Se AGREGA    Formulación ← Seminario de Investigación (HIGH).
--      (Formulación ← Ing. Económica ya existe y fue promovida en V021.)
--
-- Idempotente: las filas se identifican por la tupla estable
-- (target_course_id, required_course_id, type), NUNCA por el id aleatorio.
-- Las aristas sin sustento se INACTIVAN (no se borran) para conservar rastro.
--
-- Reversible: para revertir, ejecutar
--   UPDATE curriculum.course_requirement SET status='ACTIVE'
--    WHERE (target_course_id, required_course_id, type) IN (
--      ('c0090000-0000-4000-a000-000000000002'::uuid,'c0090000-0000-4000-a000-000000000001'::uuid,'COURSE_PASSED'),
--      ('c0070000-0000-4000-a000-000000000004'::uuid,'c0060000-0000-4000-a000-000000000004'::uuid,'COURSE_PASSED'));
--   DELETE FROM curriculum.course_requirement
--    WHERE (target_course_id, required_course_id, type) IN (
--      ('c0090000-0000-4000-a000-000000000002'::uuid,'c0040000-0000-4000-a000-000000000001'::uuid,'COURSE_PASSED'),
--      ('c0070000-0000-4000-a000-000000000004'::uuid,'c0060000-0000-4000-a000-000000000001'::uuid,'COURSE_PASSED'),
--      ('c0080000-0000-4000-a000-000000000001'::uuid,'c0070000-0000-4000-a000-000000000003'::uuid,'COURSE_PASSED'));
--
-- No hay cambio de esquema ni de código. Solo datos.

-- ---------------------------------------------------------------------------
-- 1a. INACTIVAR: Inteligencia Artificial ← Administración de Proyectos
-- ---------------------------------------------------------------------------
UPDATE curriculum.course_requirement
   SET status = 'ARCHIVED'
 WHERE target_course_id   = 'c0090000-0000-4000-a000-000000000002'::uuid  -- Inteligencia Artificial
   AND required_course_id = 'c0090000-0000-4000-a000-000000000001'::uuid  -- Administración de Proyectos
   AND type               = 'COURSE_PASSED'
   AND status             = 'ACTIVE';

-- 1b. AGREGAR: Inteligencia Artificial ← Cálculo Multivariado y Vectorial
INSERT INTO curriculum.course_requirement
    (id, target_course_id, type, required_course_id, confidence, inferred, status, version)
SELECT gen_random_uuid(),
       'c0090000-0000-4000-a000-000000000002'::uuid,                      -- Inteligencia Artificial
       'COURSE_PASSED',
       'c0040000-0000-4000-a000-000000000001'::uuid,                      -- Cálculo Multivariado y Vectorial
       'HIGH', false, 'ACTIVE', 0
 WHERE NOT EXISTS (
        SELECT 1 FROM curriculum.course_requirement
         WHERE target_course_id   = 'c0090000-0000-4000-a000-000000000002'::uuid
           AND required_course_id = 'c0040000-0000-4000-a000-000000000001'::uuid
           AND type               = 'COURSE_PASSED');

-- ---------------------------------------------------------------------------
-- 2a. INACTIVAR: Bases de Datos II ← Programación Avanzada
-- ---------------------------------------------------------------------------
UPDATE curriculum.course_requirement
   SET status = 'ARCHIVED'
 WHERE target_course_id   = 'c0070000-0000-4000-a000-000000000004'::uuid  -- Bases de Datos II
   AND required_course_id = 'c0060000-0000-4000-a000-000000000004'::uuid  -- Programación Avanzada
   AND type               = 'COURSE_PASSED'
   AND status             = 'ACTIVE';

-- 2b. AGREGAR: Bases de Datos II ← Dirección Estratégica de T.I.
INSERT INTO curriculum.course_requirement
    (id, target_course_id, type, required_course_id, confidence, inferred, status, version)
SELECT gen_random_uuid(),
       'c0070000-0000-4000-a000-000000000004'::uuid,                      -- Bases de Datos II
       'COURSE_PASSED',
       'c0060000-0000-4000-a000-000000000001'::uuid,                      -- Dirección Estratégica de T.I.
       'HIGH', false, 'ACTIVE', 0
 WHERE NOT EXISTS (
        SELECT 1 FROM curriculum.course_requirement
         WHERE target_course_id   = 'c0070000-0000-4000-a000-000000000004'::uuid
           AND required_course_id = 'c0060000-0000-4000-a000-000000000001'::uuid
           AND type               = 'COURSE_PASSED');

-- ---------------------------------------------------------------------------
-- 3. AGREGAR: Formulación y Evaluación de Proyectos ← Seminario de Investigación
-- ---------------------------------------------------------------------------
INSERT INTO curriculum.course_requirement
    (id, target_course_id, type, required_course_id, confidence, inferred, status, version)
SELECT gen_random_uuid(),
       'c0080000-0000-4000-a000-000000000001'::uuid,                      -- Formulación y Evaluación de Proyectos
       'COURSE_PASSED',
       'c0070000-0000-4000-a000-000000000003'::uuid,                      -- Seminario de Investigación
       'HIGH', false, 'ACTIVE', 0
 WHERE NOT EXISTS (
        SELECT 1 FROM curriculum.course_requirement
         WHERE target_course_id   = 'c0080000-0000-4000-a000-000000000001'::uuid
           AND required_course_id = 'c0070000-0000-4000-a000-000000000003'::uuid
           AND type               = 'COURSE_PASSED');
