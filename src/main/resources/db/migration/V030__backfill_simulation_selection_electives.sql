-- V030: Backfill simulation.simulation_selection.course_id for existing profundización
-- selections (PR1b)
--
-- Change: PROP-001-profundizacion-elective-pool. Source of truth: design D6, §9, §11 R1
-- (authoritative, 2026-07-22).
--
-- Existing simulation_selection rows persisted before V029 (the re-seeded 2026-2 simulacros)
-- store course_id pointing at one of the two shared slot ids (design D6). This migration remaps
-- simulation.simulation_selection.course_id using the SAME normalized course_name -> elective
-- mapping as V029 (kept in sync -- see V029's header comment for the exact normalization steps:
-- accent-fold, strip "Profundización"/"Profundización -" prefix, strip
-- "(Sincrónico)"/"(Asincrónico)" suffix, trim + uppercase).
--
-- Unlike V029, this migration does NOT fail on an unmatched row: simulation_selection.course_name
-- is free-form data captured at simulation time (not curated catalog data), so an unrecognized
-- course_name is a legacy/unknown row that is left untouched on its original shared slot id
-- rather than aborting the whole migration (tasks.md PR1b task 2.2).
--
-- No course_attempt backfill is needed: Diego (the only real student on this data) has zero
-- progress.course_attempt rows at the time of this migration (design D6).
--
-- Idempotent: keyed on selection id; safe to re-run (a second run finds zero remaining
-- recognized-name rows still on a shared slot id and updates nothing).

UPDATE simulation.simulation_selection ss
   SET course_id = mapped.elective_id
  FROM (
      SELECT
          ss2.id AS selection_id,
          CASE
              UPPER(TRIM(
                  regexp_replace(
                      regexp_replace(
                          translate(ss2.course_name, 'áéíóúÁÉÍÓÚñÑüÜ', 'aeiouAEIOUnNuU'),
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
              ELSE NULL  -- legacy/unknown fallback: leave the row untouched on its original course_id
          END AS elective_id
      FROM simulation.simulation_selection ss2
      WHERE ss2.course_id IN (
          'c0080000-0000-4000-a000-000000000004'::uuid,
          'c0090000-0000-4000-a000-000000000004'::uuid
      )
  ) mapped
 WHERE ss.id = mapped.selection_id
   AND mapped.elective_id IS NOT NULL;
