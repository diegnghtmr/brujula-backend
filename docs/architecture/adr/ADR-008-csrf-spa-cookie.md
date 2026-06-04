# ADR-008: Cookie-Based CSRF Protection for the SPA

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

Because Brújula uses cookie-based session authentication (ADR-007), it is vulnerable to Cross-Site Request Forgery (CSRF) attacks. Any mutating request (POST, PUT, PATCH, DELETE) from the SPA must be protected.

Options evaluated:
1. **Synchronizer Token Pattern (STP)**: server embeds a token in HTML; SPA must read from DOM
2. **Double Submit Cookie**: server sets a CSRF token in a readable cookie; SPA reads and includes it as a header
3. **SameSite cookie attribute only**: relies on browser enforcement — not universal across all contexts

---

## Decision

We adopt the **Double Submit Cookie** pattern using Spring Security's `CookieCsrfTokenRepository`:
- Spring sets an `XSRF-TOKEN` cookie (not HTTP-only, so JavaScript can read it)
- The SPA reads the `XSRF-TOKEN` cookie and sends it as the `X-XSRF-TOKEN` request header on all mutating requests
- Spring validates that the cookie value matches the header value

The Axios interceptor in `src/api/client.ts` handles this automatically:
```typescript
// CSRF interceptor — reads XSRF-TOKEN cookie → X-XSRF-TOKEN header on writes
```

In Fase 0, CSRF is disabled (`csrf.disable()`) because there are no real mutating endpoints. The interceptor seam is present in the frontend but inert until Fase 1 wires `CookieCsrfTokenRepository` into the `SecurityFilterChain`.

---

## Consequences

**Positive**:
- Works correctly with SPAs (no hidden form fields needed)
- Spring Security's `CookieCsrfTokenRepository` handles token generation and rotation
- The frontend interceptor is a one-time setup — all generated API calls get CSRF protection automatically

**Negative**:
- `XSRF-TOKEN` cookie must NOT be HttpOnly (JavaScript must read it) — XSS can still steal it, but CSRF protection is maintained even under XSS
- Requires careful cookie configuration in production (Secure flag, proper domain)

**Neutral**:
- `SameSite=Strict` or `SameSite=Lax` on the `SESSION` cookie provides additional defense-in-depth against CSRF even before the token check
- Cookie configuration (domain, path, Secure, SameSite) is a Fase 1 production hardening concern
