-- V003: Create administration module tables
--
-- Module: administration
-- Schema: administration
-- Satisfies: Admin Staff Account Management spec, Allowed Email Domains spec

-- Staff account — one row per staff member
CREATE TABLE IF NOT EXISTS administration.staff_account (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    email       TEXT        NOT NULL,
    full_name   TEXT        NOT NULL,
    status      TEXT        NOT NULL DEFAULT 'ACTIVE',
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT staff_account_pk         PRIMARY KEY (id),
    CONSTRAINT staff_account_email_uq   UNIQUE (email),
    CONSTRAINT staff_account_status_chk CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

COMMENT ON TABLE  administration.staff_account IS 'Administrative staff accounts managed by ADMIN users.';
COMMENT ON COLUMN administration.staff_account.email IS 'Institutional email; must match an allowed_email_domain.';
COMMENT ON COLUMN administration.staff_account.status IS 'Account status: ACTIVE | INACTIVE.';
COMMENT ON COLUMN administration.staff_account.version IS 'Optimistic locking version counter.';

-- Allowed email domains — controls which email domains can access the platform
CREATE TABLE IF NOT EXISTS administration.allowed_email_domain (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    domain      TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT allowed_email_domain_pk      PRIMARY KEY (id),
    CONSTRAINT allowed_email_domain_uq      UNIQUE (domain)
);

COMMENT ON TABLE  administration.allowed_email_domain IS 'Allowed email domains enforced at identity provisioning and student registration.';
COMMENT ON COLUMN administration.allowed_email_domain.domain IS 'Normalized lowercase hostname, e.g. uniquindio.edu.co.';

-- Staff program assignments — lifecycle writes deferred to a later phase
CREATE TABLE IF NOT EXISTS administration.staff_program_assignment (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    staff_id    UUID        NOT NULL,
    program_id  UUID        NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT staff_program_assignment_pk           PRIMARY KEY (id),
    CONSTRAINT staff_program_assignment_staff_fk     FOREIGN KEY (staff_id)
        REFERENCES administration.staff_account (id) ON DELETE CASCADE,
    CONSTRAINT staff_program_assignment_unique       UNIQUE (staff_id, program_id)
);

COMMENT ON TABLE  administration.staff_program_assignment IS 'Assignment of staff to academic programs. Write lifecycle deferred to a later phase.';
COMMENT ON COLUMN administration.staff_program_assignment.program_id IS 'Reference to programcatalog.program (no FK across schemas).';
