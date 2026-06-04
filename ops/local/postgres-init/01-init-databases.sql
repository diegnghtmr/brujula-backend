-- Initialize databases and user for local development.
-- This script runs once on first container start (PostgreSQL initdb).
-- Creates: planner_db (application), keycloak_db (Keycloak IAM).
-- The POSTGRES_DB env var already creates the first DB; we only need keycloak_db here.

SELECT 'CREATE DATABASE keycloak_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak_db')\gexec

GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO current_user;
