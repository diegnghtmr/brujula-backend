-- V008: Add consent evidence columns and onboarding_state to identity.app_user
--
-- Module: identity
-- Schema: identity
-- Satisfies: Successful registration stores consent evidence
--
-- Adds:
--   consent_timestamp TIMESTAMPTZ — when the user accepted terms (null = OIDC-provisioned, no self-registration)
--   terms_version TEXT            — the terms-of-use version the user accepted (null = OIDC-provisioned)
--   privacy_version TEXT          — the privacy-policy version the user accepted (null = OIDC-provisioned)
--   onboarding_state TEXT         — authoritative onboarding state
--
-- onboarding_completed (bool, from V002) is kept for backward compat and set to TRUE for ACTIVE rows.
-- onboarding_state is the NEW authoritative field. Default 'ACTIVE' for existing rows provisioned
-- via OIDC login (they bypassed the registration flow and are already active).
-- New self-registered students start as 'PENDING_VERIFICATION'.

ALTER TABLE identity.app_user
    ADD COLUMN IF NOT EXISTS consent_timestamp  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS terms_version      TEXT,
    ADD COLUMN IF NOT EXISTS privacy_version    TEXT,
    ADD COLUMN IF NOT EXISTS onboarding_state   TEXT NOT NULL DEFAULT 'ACTIVE';

-- Backfill note: the ADD COLUMN above sets all existing rows to onboarding_state='ACTIVE' via the
-- DEFAULT clause. No explicit UPDATE is needed — the DEFAULT handles the backfill atomically.
-- Existing OIDC-provisioned rows correctly get ACTIVE (they bypassed the registration flow and
-- are already active). The status field handles account deactivation separately.

COMMENT ON COLUMN identity.app_user.consent_timestamp IS
    'Timestamp when the user accepted terms and privacy policy during self-registration. NULL for OIDC-provisioned users.';
COMMENT ON COLUMN identity.app_user.terms_version IS
    'Version of the terms of use accepted at registration time. NULL for OIDC-provisioned users.';
COMMENT ON COLUMN identity.app_user.privacy_version IS
    'Version of the privacy policy accepted at registration time. NULL for OIDC-provisioned users.';
COMMENT ON COLUMN identity.app_user.onboarding_state IS
    'Onboarding lifecycle state: PENDING_VERIFICATION (registered, email not yet verified) | ACTIVE (email verified or OIDC-provisioned). The legacy onboarding_completed bool column is kept for backward compatibility.';
