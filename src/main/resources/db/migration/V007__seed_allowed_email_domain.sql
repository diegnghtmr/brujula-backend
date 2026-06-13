-- V007: Seed allowed email domains
--
-- Module: administration
-- Schema: administration
-- Satisfies: Allowed Email Domains spec
-- Note: Data migration separate from structure (V003 creates the table; this seeds it).

INSERT INTO administration.allowed_email_domain (domain)
VALUES ('uniquindio.edu.co')
ON CONFLICT (domain) DO NOTHING;
