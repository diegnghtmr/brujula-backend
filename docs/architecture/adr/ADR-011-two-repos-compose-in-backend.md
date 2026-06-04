# ADR-011: Two Repositories; Compose and Keycloak Owned by Backend

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

Brújula is deployed as two separate processes (backend Spring Boot, frontend SPA). The project team debated whether to use a single monorepo or two separate repositories, and where to host the Docker Compose infrastructure and the Keycloak realm configuration.

Options evaluated:
1. **Single monorepo**: all code in one repository — simpler cross-repo changes, but mixes concerns
2. **Two repos, shared infra repo**: backend + frontend + infra separate — three-repo complexity
3. **Two repos, infra owned by backend**: infrastructure lives alongside the application that owns it

---

## Decision

We adopt **two separate repositories** (`brujula-backend` and `brujula-frontend`) with **infrastructure owned by the backend repository**.

Rationale:
- Backend owns the database schema, Flyway migrations, Keycloak realm config, and Compose files — these are tightly coupled to the Spring Boot application lifecycle
- Frontend is independently deployable (static files served by a CDN or preview server) and does not need to reference infrastructure concerns
- `ops/local/` in `brujula-backend` contains `compose.yaml`, `compose.full.yaml`, Keycloak realm export, and `.env.example`

The two repos are synchronized via the OpenAPI contract:
- `brujula-backend/openapi/brujula-api.v1.yaml` is the contract source of truth
- `brujula-frontend/openapi/brujula-api.v1.yaml` is a tracked copy; sync is manual (or automated via CI)

---

## Consequences

**Positive**:
- Backend and frontend can have independent CI pipelines and release cadences
- Clear ownership: backend team owns migrations, infra, and contract; frontend team owns the SPA
- `brujula-frontend` contributors do not need Docker or Java installed to develop UI components

**Negative**:
- Contract sync between repos is manual — a missed sync can cause type errors in the frontend
- Cross-repo atomic changes (API + UI) require coordinated PRs

**Neutral**:
- A future monorepo migration (using git subtree or workspaces) is possible but not planned
- The `docs/openapi/` directory in `brujula-general` (the parent planning repo) holds the master copy for documentation purposes only; `brujula-backend/openapi/` is the authoritative build input
