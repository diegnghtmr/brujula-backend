-- V002: Create identity module tables
--
-- Module: identity
-- Schema: identity
-- Satisfies: Identity Provisioning spec

CREATE TABLE IF NOT EXISTS identity.app_user (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    oidc_subject    TEXT        NOT NULL,
    email           TEXT        NOT NULL,
    full_name       TEXT        NOT NULL,
    role            TEXT        NOT NULL,
    status          TEXT        NOT NULL DEFAULT 'ACTIVE',
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT app_user_pk                 PRIMARY KEY (id),
    CONSTRAINT app_user_oidc_subject_uq    UNIQUE (oidc_subject),
    CONSTRAINT app_user_email_uq           UNIQUE (email),
    CONSTRAINT app_user_role_chk           CHECK (role IN ('STUDENT', 'STAFF', 'ADMIN')),
    CONSTRAINT app_user_status_chk         CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING_VERIFICATION'))
);

COMMENT ON TABLE  identity.app_user IS 'Internal user record linked to a Keycloak OIDC subject.';
COMMENT ON COLUMN identity.app_user.oidc_subject IS 'Stable Keycloak sub claim — never changes.';
COMMENT ON COLUMN identity.app_user.email IS 'Email address from OIDC claims (institutional domain enforced at application level).';
COMMENT ON COLUMN identity.app_user.role IS 'Platform role: STUDENT | STAFF | ADMIN.';
COMMENT ON COLUMN identity.app_user.status IS 'Account status lifecycle: ACTIVE | INACTIVE | PENDING_VERIFICATION.';
COMMENT ON COLUMN identity.app_user.onboarding_completed IS 'True once the user completes the initial onboarding flow.';
COMMENT ON COLUMN identity.app_user.version IS 'Optimistic locking version counter.';
