-- V024__add_simulation_scope_table.sql
-- Forward-only migration. NEVER edit this file after it is committed.
-- Adds the simulation.simulation_scope table to persist the (programId, journeyCode) pairs
-- that define the scope of a simulation.
-- The pre-existing simulation.simulation.program_ids column is kept as a backward-compatible
-- derived field (distinct programIds from the scope rows).
-- Design: INV-002-v2. Cross-schema opaque UUIDs, NO cross-schema FK (ADR-F3-009).

-- ---------------------------------------------------------------------------
-- simulation.simulation_scope
-- One row per (programId, journeyCode) pair selected when the simulation was created.
-- At most 4 rows per simulation_id (at most 2 distinct program_ids, 1-2 jornadas each).
-- ---------------------------------------------------------------------------
CREATE TABLE simulation.simulation_scope (
    simulation_id  UUID   NOT NULL,
    program_id     UUID   NOT NULL,
    journey_code   TEXT   NOT NULL,

    CONSTRAINT simulation_scope_pk
        PRIMARY KEY (simulation_id, program_id, journey_code),

    CONSTRAINT simulation_scope_fk_simulation
        FOREIGN KEY (simulation_id)
        REFERENCES simulation.simulation (id)
        ON DELETE CASCADE
);

-- Index for loading all scope rows for a simulation
CREATE INDEX idx_simulation_scope_sim
    ON simulation.simulation_scope (simulation_id);

-- ---------------------------------------------------------------------------
-- Backfill: for each existing simulation row insert one scope row per
-- active subscription the owner has for each program in program_ids.
--
-- Column reference (V015): progress.student_program_subscription
--   user_id, program_id, reference_journey_code, status ('ACTIVE'/'DELETED')
--
-- If a program in program_ids has NO active subscription for the owner,
-- insert a fallback row with journey_code = 'DIURNA' so no simulation
-- is left without scope rows.
-- ---------------------------------------------------------------------------

-- Step 1: insert rows backed by active subscriptions
INSERT INTO simulation.simulation_scope (simulation_id, program_id, journey_code)
SELECT DISTINCT
    sim.id            AS simulation_id,
    unnested.pid      AS program_id,
    sub.reference_journey_code AS journey_code
FROM simulation.simulation sim
CROSS JOIN LATERAL unnest(sim.program_ids) AS unnested(pid)
JOIN progress.student_program_subscription sub
    ON sub.user_id    = sim.owner_id
   AND sub.program_id = unnested.pid
   AND sub.status     = 'ACTIVE'
ON CONFLICT (simulation_id, program_id, journey_code) DO NOTHING;

-- Step 2: fallback for programs with no active subscription (insert 'DIURNA')
INSERT INTO simulation.simulation_scope (simulation_id, program_id, journey_code)
SELECT
    sim.id       AS simulation_id,
    unnested.pid AS program_id,
    'DIURNA'     AS journey_code
FROM simulation.simulation sim
CROSS JOIN LATERAL unnest(sim.program_ids) AS unnested(pid)
WHERE NOT EXISTS (
    SELECT 1
    FROM simulation.simulation_scope sc
    WHERE sc.simulation_id = sim.id
      AND sc.program_id    = unnested.pid
)
ON CONFLICT (simulation_id, program_id, journey_code) DO NOTHING;
