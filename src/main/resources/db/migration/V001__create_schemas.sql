-- V001: Create PostgreSQL schemas for all Brujula modules
--
-- Schema-to-module mapping:
--   identity      → module: identity
--   administration → module: administration
--   catalog       → module: programcatalog  (schema name differs from module name)
--   curriculum    → module: curriculum
--   offering      → module: offering
--   progress      → module: progress
--   simulation    → module: simulation
--   imports       → module: imports
--   audit         → module: audit
--   platform      → module: sharedkernel    (schema name differs from module name; owns idempotency/session tables)

CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS administration;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS curriculum;
CREATE SCHEMA IF NOT EXISTS offering;
CREATE SCHEMA IF NOT EXISTS progress;
CREATE SCHEMA IF NOT EXISTS simulation;
CREATE SCHEMA IF NOT EXISTS imports;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS platform;
