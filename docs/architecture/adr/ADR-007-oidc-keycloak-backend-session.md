# ADR-007: OIDC with Keycloak + Backend-Held Session (Spring Session JDBC)

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

Brújula requires authentication and authorization for students and staff. The options were:
1. **JWT stateless**: backend validates token on every request; no server-side session storage
2. **Backend-held session**: after OIDC login, the backend issues a session cookie and holds the token server-side
3. **Frontend-managed tokens**: SPA stores and sends tokens; exposes tokens to JavaScript

Universidad del Quindío uses Keycloak as the institutional identity provider. The application must support OIDC login via `university.uniquindio.edu.co` email constraint.

---

## Decision

We adopt **Keycloak OIDC** as the identity provider and **Spring Session JDBC** as the session store. After a successful OIDC authorization code flow, the backend holds the Keycloak token server-side and issues an HTTP-only `SESSION` cookie to the SPA.

Rationale for backend-held session over stateless JWT:
- The SPA never touches the Keycloak token — eliminates token theft from JavaScript memory or localStorage
- Server-side session allows immediate revocation (delete from `platform.sessions` table)
- Keycloak token refresh is handled entirely by the backend — the SPA only sees its own session cookie
- Spring Session JDBC stores sessions in PostgreSQL `platform` schema — compatible with the modular monolith

The Keycloak realm (`brujula-dev`) is configured in `ops/local/keycloak/realm-export.json`. Real users and full OIDC flow wiring are Fase 1 scope.

---

## Consequences

**Positive**:
- Tokens never exposed to JavaScript — significantly reduces XSS token-theft risk
- Immediate session revocation without token expiry waiting
- SPA auth flows are simpler (no token refresh logic in the frontend)

**Negative**:
- Horizontal scaling requires a shared session store (PostgreSQL is already shared — no extra infrastructure)
- Slightly higher latency per request (session lookup from DB vs JWT decode)
- Spring Session JDBC setup adds migration complexity (session table in `platform` schema — Fase 1)

**Neutral**:
- CSRF protection is required because the session is cookie-based (see ADR-008)
- The Keycloak client is configured as `publicClient: false` with `directAccessGrantsEnabled: false` in production; dev realm uses permissive settings (see `realm-export.json`)
