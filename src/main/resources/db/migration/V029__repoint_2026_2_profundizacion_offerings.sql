-- V029: Re-point 2026-2 profundización offerings to their specific elective course_id (PR1b)
--
-- Change: PROP-001-profundizacion-elective-pool. Source of truth: design D6, §9, §11
-- R-offering-map (authoritative, 2026-07-22).
--
-- V022 mapped every named "Profundización ..." offering in period 2026-2 to one of the two
-- generic slot ids (c0080000-...-004 "Profundización I", c0090000-...-004 "Profundización III")
-- as an interim measure, before the specific elective courses existed (design §1). V028 (PR1a)
-- created the 8 specific DEEPENING elective courses (design §3). This migration re-points
-- offering.course_offering.course_id from the two shared slot ids to the specific elective id,
-- matched by a normalized course_name -> elective mapping:
--   1. accent-fold (á->a, é->e, í->i, ó->o, ú->u, ñ->n, ü->u, both cases)
--   2. strip the leading "Profundización"/"Profundización -" prefix
--   3. strip the trailing "(Sincrónico)"/"(Asincrónico)" modality suffix
--   4. trim + uppercase
--
-- Kept in sync with V030 (simulation_selection backfill), which reuses the SAME normalization
-- steps against simulation.simulation_selection.course_name -- if this mapping ever changes,
-- update V030's copy identically. Combining into one migration was considered (design §9) but
-- kept separate: V029 targets the curated offering catalog (fails loudly on any miss) while V030
-- targets free-form historical selection data (never fails, legacy fallback instead).
--
-- Only rows whose course_id is currently one of the two shared slot ids are touched; every other
-- offering (any other course, any other period) is left completely untouched.
--
-- Fails loudly (raises an exception) if, after the UPDATE, any offering in period 2026-2 still
-- references a shared slot course_id -- this means the normalization did not cover some
-- course_name variant and must NOT be silently left on the shared id (design §11 R-offering-map).
--
-- Idempotent: keyed on offering id; safe to re-run (a second run finds zero remaining shared-slot
-- rows in scope and updates nothing).

UPDATE offering.course_offering co
   SET course_id = mapped.elective_id
  FROM (
      SELECT
          co2.id AS offering_id,
          CASE
              UPPER(TRIM(
                  regexp_replace(
                      regexp_replace(
                          translate(co2.course_name, 'áéíóúÁÉÍÓÚñÑüÜ', 'aeiouAEIOUnNuU'),
                          '\s*\((Sincronico|Asincronico)\)\s*$', '', 'i'
                      ),
                      '^\s*Profundizacion\s*-?\s*', '', 'i'
                  )
              ))
              WHEN 'DISENO CENTRADO EN EL USUARIO'
                  THEN 'c0dee000-0000-4000-a000-000000000001'::uuid
              WHEN 'DESARROLLO DE APLICACIONES PARA DISPOSITIVOS MOVILES'
                  THEN 'c0dee000-0000-4000-a000-000000000002'::uuid
              WHEN 'USABILIDAD Y EXPERIENCIA DE USUARIO'
                  THEN 'c0dee000-0000-4000-a000-000000000003'::uuid
              WHEN 'COMPUTACION EN LA NUBE'
                  THEN 'c0dee000-0000-4000-a000-000000000004'::uuid
              WHEN 'ARQUITECTURA ORIENTADA A MICROSERVICIOS'
                  THEN 'c0dee000-0000-4000-a000-000000000005'::uuid
              WHEN 'VISUALIZACION DE DATOS'
                  THEN 'c0dee000-0000-4000-a000-000000000006'::uuid
              WHEN 'VISION ARTIFICIAL'
                  THEN 'c0dee000-0000-4000-a000-000000000007'::uuid
              WHEN 'INTRODUCCION AL BIG DATA'
                  THEN 'c0dee000-0000-4000-a000-000000000008'::uuid
              ELSE NULL
          END AS elective_id
      FROM offering.course_offering co2
      JOIN offering.academic_period ap ON ap.id = co2.period_id
      WHERE ap.name = '2026-2'
        AND co2.course_id IN (
            'c0080000-0000-4000-a000-000000000004'::uuid,
            'c0090000-0000-4000-a000-000000000004'::uuid
        )
  ) mapped
 WHERE co.id = mapped.offering_id
   AND mapped.elective_id IS NOT NULL;

-- Fail loudly: zero offerings in period 2026-2 may remain on a shared slot id after the re-point.
DO $$
DECLARE
    remaining INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO remaining
      FROM offering.course_offering co
      JOIN offering.academic_period ap ON ap.id = co.period_id
     WHERE ap.name = '2026-2'
       AND co.course_id IN (
           'c0080000-0000-4000-a000-000000000004'::uuid,
           'c0090000-0000-4000-a000-000000000004'::uuid
       );

    IF remaining > 0 THEN
        RAISE EXCEPTION
            'V029: % profundización offering(s) in period 2026-2 still reference a shared slot course_id after re-point -- the normalized course_name mapping does not cover every V022 name variant (design §11 R-offering-map)',
            remaining;
    END IF;
END $$;
