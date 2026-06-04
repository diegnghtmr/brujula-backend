# ADR-005: PostgreSQL 18 + Flyway as Sole Schema Authority

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

Brújula requires a relational database that supports:
- Multi-schema organization (one schema per bounded context)
- ACID transactions across module boundaries (for cross-cutting operations)
- JSON columns for flexible audit payloads and simulation results
- Robust schema migration tooling compatible with Spring Boot 4

We evaluated PostgreSQL (13+) vs MySQL vs H2. For schema migration: Flyway vs Liquibase.

---

## Decision

We adopt **PostgreSQL 18** as the sole database engine and **Flyway** (managed by the Spring Boot BOM) as the sole schema migration authority.

Constraints:
- **`spring.jpa.hibernate.ddl-auto` must never be `create`, `create-drop`, or `update`** in any profile; only `validate` or `none` is permitted (DB-003)
- All schema changes go through numbered Flyway migrations (`V001__create_schemas.sql`, `V002__...`, etc.)
- Each bounded context module maps to a dedicated PostgreSQL schema (e.g., `programcatalog` module → `catalog` schema)
- The `platform` schema is owned by sharedkernel/infrastructure for cross-cutting tables (idempotency records, session tokens)

PostgreSQL schemas serve as the structural boundary between modules at the database level, mirroring the Spring Modulith module boundaries at the application level.

---

## Consequences

**Positive**:
- Schema isolation prevents accidental cross-module table access at the database layer
- Flyway provides immutable, auditable migration history
- `ddl-auto=validate` catches schema drift between migrations and JPA entity mappings at startup
- PostgreSQL 18's JSONB, window functions, and CTE support cover all known Brújula query patterns

**Negative**:
- Schema-per-module adds overhead when writing cross-schema queries (explicit schema qualification needed)
- Flyway migration ordering must be carefully managed across teams
- No H2 support in production (integration tests must use Testcontainers PostgreSQL)

**Neutral**:
- H2 is used ONLY for lightweight config-level tests (e.g., Swagger profile guard) where no schema operations occur; all persistence tests use Testcontainers
