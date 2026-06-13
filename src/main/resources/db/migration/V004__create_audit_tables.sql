-- V004: Create audit module tables
--
-- Module: audit
-- Schema: audit
-- Satisfies: Audit Log spec
-- Note: append-only by design; no UPDATE grants assumed at application level.

CREATE TABLE IF NOT EXISTS audit.audit_event (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    actor_id        UUID        NOT NULL,
    actor_role      TEXT        NOT NULL,
    action          TEXT        NOT NULL,
    entity_type     TEXT        NOT NULL,
    entity_id       UUID,
    program_id      UUID,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    previous_value  JSONB,
    new_value       JSONB,

    CONSTRAINT audit_event_pk          PRIMARY KEY (id),
    CONSTRAINT audit_event_role_chk    CHECK (actor_role IN ('STUDENT', 'STAFF', 'ADMIN'))
);

COMMENT ON TABLE  audit.audit_event IS 'Immutable audit trail of sensitive platform actions. Append-only.';
COMMENT ON COLUMN audit.audit_event.actor_id IS 'identity.app_user.id of the user who performed the action.';
COMMENT ON COLUMN audit.audit_event.actor_role IS 'Role at time of action: STUDENT | STAFF | ADMIN.';
COMMENT ON COLUMN audit.audit_event.action IS 'Semantic action code, e.g. STAFF_CREATED, LOGIN, LOGOUT.';
COMMENT ON COLUMN audit.audit_event.entity_type IS 'Type of the affected entity, e.g. app_user, staff_account.';
COMMENT ON COLUMN audit.audit_event.entity_id IS 'PK of the affected entity (nullable for non-entity actions).';
COMMENT ON COLUMN audit.audit_event.program_id IS 'Affected academic program (nullable; used for STAFF scope filtering).';
COMMENT ON COLUMN audit.audit_event.previous_value IS 'JSON snapshot of entity state before the action (sanitized — no secrets/tokens).';
COMMENT ON COLUMN audit.audit_event.new_value IS 'JSON snapshot of entity state after the action (sanitized — no secrets/tokens).';

-- Indexes for frequent query patterns (actor activity, program scope, time range)
CREATE INDEX IF NOT EXISTS audit_event_actor_id_idx
    ON audit.audit_event (actor_id);

CREATE INDEX IF NOT EXISTS audit_event_program_id_idx
    ON audit.audit_event (program_id)
    WHERE program_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS audit_event_occurred_at_idx
    ON audit.audit_event (occurred_at DESC);
