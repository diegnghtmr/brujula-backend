# ADR-010: Optimistic Concurrency Control (version field, 409 Conflict)

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

Brújula supports concurrent updates from multiple browser tabs or sessions (e.g., a student editing their simulation in two windows). Without concurrency control, the last-write-wins and silent data loss occurs.

Options evaluated:
1. **Pessimistic locking** (`SELECT FOR UPDATE`): serializes all writes on the same aggregate — high contention, poor UX
2. **Optimistic locking** (`version` field): no lock held; conflict detected at commit time, returned as 409 to client
3. **Event sourcing**: append-only log; no conflicts possible — significant complexity for this domain

---

## Decision

We adopt **optimistic concurrency control** using a `version` (long integer) field on all mutable aggregates.

Protocol:
- Every aggregate's GET response includes a `version` field (declared in the OpenAPI contract)
- Every mutating command (PUT, PATCH) includes the `version` from the last GET in the request body
- The backend compares the submitted `version` against the stored value
- If versions match: update proceeds and `version` is incremented
- If versions differ: the backend returns `HTTP 409 Conflict` with a structured problem-detail body

The `version` field maps to `@Version` on JPA entities (Spring Data JPA handles the check transparently via `OptimisticLockException` → mapped to 409 in the error handler).

---

## Consequences

**Positive**:
- No database-level locking — high concurrency without serialization
- Clear user feedback on conflict (frontend can refresh and retry)
- `@Version` is standard JPA — minimal implementation overhead

**Negative**:
- Frontend must handle 409 responses and prompt the user to review the latest state before retrying
- Aggregates with high write contention may produce frequent 409s (not expected for individual student plans)

**Neutral**:
- The `version` field is declared in the OpenAPI contract; the generated TypeScript client includes it in all relevant request/response types
- `AggregateVersion` value object lives in `sharedkernel` to avoid duplication across modules
