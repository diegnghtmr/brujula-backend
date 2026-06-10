-- V020: Rename Spring Session tables to lowercase identifiers.
--
-- Context: V006 created platform."SPRING_SESSION" and platform."SPRING_SESSION_ATTRIBUTES"
-- using double-quoted (case-sensitive) uppercase identifiers. Spring Session JDBC generates
-- unquoted SQL for column names (e.g. PRIMARY_ID, SESSION_ID) which PostgreSQL folds to
-- lowercase, causing "column primary_id does not exist" errors at runtime.
--
-- Fix: rename tables and columns to lowercase so unquoted references work correctly.
-- Spring Session config uses table-name=platform.SPRING_SESSION which maps to lowercase.
--
-- References: ADR-007, V006, G1.

-- Rename tables to lowercase
ALTER TABLE platform."SPRING_SESSION" RENAME TO spring_session;
ALTER TABLE platform."SPRING_SESSION_ATTRIBUTES" RENAME TO spring_session_attributes;

-- Rename columns in spring_session to lowercase
ALTER TABLE platform.spring_session RENAME COLUMN "PRIMARY_ID"            TO primary_id;
ALTER TABLE platform.spring_session RENAME COLUMN "SESSION_ID"            TO session_id;
ALTER TABLE platform.spring_session RENAME COLUMN "CREATION_TIME"         TO creation_time;
ALTER TABLE platform.spring_session RENAME COLUMN "LAST_ACCESS_TIME"      TO last_access_time;
ALTER TABLE platform.spring_session RENAME COLUMN "MAX_INACTIVE_INTERVAL" TO max_inactive_interval;
ALTER TABLE platform.spring_session RENAME COLUMN "EXPIRY_TIME"           TO expiry_time;
ALTER TABLE platform.spring_session RENAME COLUMN "PRINCIPAL_NAME"        TO principal_name;

-- Rename columns in spring_session_attributes to lowercase
ALTER TABLE platform.spring_session_attributes RENAME COLUMN "SESSION_PRIMARY_ID" TO session_primary_id;
ALTER TABLE platform.spring_session_attributes RENAME COLUMN "ATTRIBUTE_NAME"     TO attribute_name;
ALTER TABLE platform.spring_session_attributes RENAME COLUMN "ATTRIBUTE_BYTES"    TO attribute_bytes;

-- Rename constraints and indexes (keep them valid after rename)
ALTER TABLE platform.spring_session RENAME CONSTRAINT "SPRING_SESSION_PK" TO spring_session_pk;
ALTER TABLE platform.spring_session_attributes RENAME CONSTRAINT "SPRING_SESSION_ATTRIBUTES_PK" TO spring_session_attributes_pk;
ALTER TABLE platform.spring_session_attributes RENAME CONSTRAINT "SPRING_SESSION_ATTRIBUTES_FK" TO spring_session_attributes_fk;

ALTER INDEX platform."SPRING_SESSION_IX1" RENAME TO spring_session_ix1;
ALTER INDEX platform."SPRING_SESSION_IX2" RENAME TO spring_session_ix2;
ALTER INDEX platform."SPRING_SESSION_IX3" RENAME TO spring_session_ix3;

-- ============================================================
-- REVERSIBILITY NOTE (SA-004)
-- ============================================================
-- There is no Flyway undo migration configured for this project.
-- To revert V020 in local development:
--
--   1. docker compose down -v         # destroys the postgres volume
--   2. docker compose up -d postgres  # recreates with clean state
--   3. docker compose up -d backend   # Flyway re-runs V001..V020
--
-- WARNING: `docker compose down -v` destroys ALL data in the volume
-- (sessions, seed data, test data). This is acceptable for local dev
-- but must never be run against a production or shared database.
--
-- For production environments (if/when applicable), a reverse migration
-- would require re-quoting all column/table names to uppercase:
--   ALTER TABLE platform.spring_session RENAME TO "SPRING_SESSION"; -- etc.
-- and updating application.yaml spring.session.jdbc.table-name accordingly.
