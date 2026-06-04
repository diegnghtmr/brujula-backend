# ADR-009: Idempotency-Key for Critical Commands

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

Brújula's critical write operations (enrollment commands, progress updates, simulation saves) must be safe to retry in the face of network timeouts or client double-submission. Without idempotency protection, a retry can create duplicate records (two enrollment attempts for the same course in the same semester).

Options evaluated:
1. **Client-side deduplication**: disable the submit button after first click — fragile, no server-side guarantee
2. **Idempotency-Key header**: client sends a UUID; server stores the result and returns it on replay
3. **Natural key deduplication**: use unique constraints in the database — correct but coarse-grained

---

## Decision

We adopt the **`Idempotency-Key` header pattern** for all critical write commands. The contract (`openapi/brujula-api.v1.yaml`) marks affected endpoints with `Idempotency-Key: required`.

Implementation:
- The `platform` schema hosts an `idempotency_record` table with columns: `key` (UUID), `request_hash`, `response_status`, `response_body`, `created_at`, `expires_at`
- A shared application service (in `sharedkernel`) checks the key before processing; if found, returns the stored response immediately
- Keys expire after 24 hours (configurable)
- The SPA generates a UUIDv4 `Idempotency-Key` for each command initiation

The `Idempotency-Key` header requirement is defined in the OpenAPI contract — the generated backend interfaces carry the header parameter annotation; the frontend client passes it automatically.

---

## Consequences

**Positive**:
- Safe retries on network failure — users can retry without fear of duplication
- Supports the "at-most-once" guarantee for enrollment commands

**Negative**:
- `idempotency_record` table must be pruned (TTL-based cleanup job) to avoid unbounded growth
- Adds a DB lookup on every covered command (latency increase, mitigated by indexed `key` column)

**Neutral**:
- Idempotency table migration (`V002__idempotency_record.sql`) is Fase 1 scope; the concept and contract header are established in Fase 0
