# ADR-013: Configurable Credit Limit, Initial Value 23

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

Universidad del Quindío regulations define a maximum number of academic credits a student may enroll in per semester. This limit is:
1. Historically set at 23 credits for most programs
2. Subject to change by the university's academic council without software changes
3. Potentially different per program or academic period type

If the limit is hard-coded, any regulatory change requires a code deployment. If stored per-program in the database, the administrative burden increases and the initial setup is more complex.

---

## Decision

The credit limit is **configurable at the platform level**, stored as a named configurable domain entry in the database, with an **initial default value of 23 credits**.

Implementation:
- A `configurable_domains` table in the `administration` schema stores platform-wide configuration key-value pairs
- The key `MAX_CREDITS_PER_SEMESTER` has initial value `23`
- The `simulation` module reads this value via the `administration` module's use-case port (not direct table access)
- The value is validated at enrollment simulation time: if the total selected credits exceed the limit, the simulation returns a `CREDIT_LIMIT_EXCEEDED` business error
- Admin users can update the value via the administration API (an authenticated, logged, idempotent write)

The `administration` module exposes a `GetCreditLimitQuery` use-case port; `simulation` is declared as an allowed consumer in `administration`'s `@ApplicationModule(allowedDependencies)`.

---

## Consequences

**Positive**:
- Credit limit changes require only an admin API call, not a deployment
- The configurable domains pattern is reusable for other platform-wide settings (e.g., `MAX_SUBSCRIPTIONS_PER_STUDENT`, `ALLOWED_EMAIL_DOMAINS`)
- Clear audit trail: all changes to configurable domains are logged to the `audit` module

**Negative**:
- Adds a database lookup on every simulation validation (mitigated by application-level caching in Fase 2+)
- The configurable domains table must be seeded with initial values via Flyway (V010 migration — Fase 1+)

**Neutral**:
- If a per-program credit limit is needed in the future, the `configurable_domains` entry can be parameterized with a `program_id` scope without changing the API contract
- The initial value of 23 matches Universidad del Quindío's current reglamento studies as of 2025
