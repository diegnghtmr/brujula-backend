-- V018__create_simulation_tables.sql
-- Forward-only migration. NEVER edit this file after it is committed.
-- Creates simulation module tables. Schema 'simulation' was created in V001__create_schemas.sql.
-- Cross-schema opaque UUIDs, NO cross-schema FK.
-- OUTDATED status is NEVER stored — only ACTIVE/DELETED lifecycle is persisted.

-- ---------------------------------------------------------------------------
-- simulation.simulation
-- ---------------------------------------------------------------------------
CREATE TABLE simulation.simulation (
    id          UUID    NOT NULL DEFAULT gen_random_uuid(),
    owner_id    UUID    NOT NULL,
    name        TEXT    NOT NULL
                        CONSTRAINT simulation_name_length_chk
                        CHECK (char_length(name) >= 1 AND char_length(name) <= 120),
    period_id   UUID    NOT NULL,
    program_ids UUID[]  NOT NULL,
    status      TEXT    NOT NULL DEFAULT 'ACTIVE'
                        CONSTRAINT simulation_status_chk
                        CHECK (status IN ('ACTIVE', 'DELETED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version     BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT simulation_pk PRIMARY KEY (id)
);

-- Index for listing simulations by owner (BOLA scoping)
CREATE INDEX idx_simulation_owner_status
    ON simulation.simulation (owner_id, status);

-- Index for filtering by owner + period
CREATE INDEX idx_simulation_owner_period
    ON simulation.simulation (owner_id, period_id);

-- ---------------------------------------------------------------------------
-- simulation.simulation_selection
-- Created here so V018 stands alone without requiring a later schema-altering migration.
-- It will remain empty until AddSimulationSelectionService is wired.
-- ---------------------------------------------------------------------------
CREATE TABLE simulation.simulation_selection (
    id                          UUID    NOT NULL DEFAULT gen_random_uuid(),
    simulation_id               UUID    NOT NULL,
    offering_id                 UUID    NOT NULL,
    schedule_option_id          UUID    NOT NULL,
    course_id                   UUID    NOT NULL,
    course_name                 TEXT    NOT NULL,
    program_id                  UUID    NOT NULL,
    credits                     NUMERIC(5, 2) NOT NULL,
    journey_code                TEXT    NOT NULL,
    offering_status             TEXT    NOT NULL,
    selected_offering_version   BIGINT  NOT NULL,
    sessions                    JSONB   NOT NULL DEFAULT '[]',
    status                      TEXT    NOT NULL DEFAULT 'ACTIVE'
                                        CONSTRAINT simulation_selection_status_chk
                                        CHECK (status IN ('ACTIVE', 'DELETED')),
    version                     BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT simulation_selection_pk PRIMARY KEY (id),
    CONSTRAINT simulation_selection_fk_simulation
        FOREIGN KEY (simulation_id) REFERENCES simulation.simulation (id) ON DELETE RESTRICT
);

-- Index for loading all active selections for a simulation
CREATE INDEX idx_simulation_selection_sim_status
    ON simulation.simulation_selection (simulation_id, status);
