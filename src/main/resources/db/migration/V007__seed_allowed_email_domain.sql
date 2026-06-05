-- V007: Seed allowed email domains
--
-- Module: administration
-- Schema: administration
-- Satisfies: Spec §Allowed Email Domains (RN-007), Design §3 V007
-- Note: MIG-005 — data migration separate from structure (V003 creates the table; this seeds it).

INSERT INTO administration.allowed_email_domain (domain)
VALUES ('uniquindio.edu.co')
ON CONFLICT (domain) DO NOTHING;
