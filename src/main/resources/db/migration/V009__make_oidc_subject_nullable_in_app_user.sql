-- V009: Make oidc_subject nullable in identity.app_user
--
-- Module: identity
-- Schema: identity
-- Satisfies: Spec §Self-registration (self-registered students have no OIDC subject until first login);
--            Design §3 V009; S2-B-1.
--
-- Context:
--   V002 defined oidc_subject as NOT NULL — all users were OIDC-provisioned (Slice 1).
--   Slice 2 introduces AppUser.register() for self-registration: these users start in
--   PENDING_VERIFICATION state with null oidcSubject. The subject is assigned on first OIDC login
--   (sync-on-login, S2-B-4) when Keycloak provides the sub claim.
--
-- Changes:
--   • DROP the NOT NULL constraint on oidc_subject (allow null for self-registered students).
--   • The UNIQUE constraint on oidc_subject is preserved (non-null values remain unique;
--     PostgreSQL UNIQUE allows multiple NULL rows — only one subject per Keycloak user).
--
-- Backward compatibility:
--   Existing OIDC-provisioned rows already have a non-null oidc_subject — no data migration needed.

ALTER TABLE identity.app_user
    ALTER COLUMN oidc_subject DROP NOT NULL;

COMMENT ON COLUMN identity.app_user.oidc_subject IS
    'Stable Keycloak sub claim — assigned on first OIDC login. '
    'NULL for self-registered students in PENDING_VERIFICATION state (before first login).';
