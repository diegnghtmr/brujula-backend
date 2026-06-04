# ADR-006: OpenAPI 3.1 Contract-First; Backend `spring` interfaceOnly + Frontend `typescript-axios`

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

The API contract between the backend and frontend must be:
1. Machine-verifiable (not hand-written documentation that drifts)
2. Type-safe on both sides
3. The single source of truth that both codebases compile against

We evaluated three approaches:
1. **Hand-written contract**: backend and frontend independently agree on shape — high drift risk
2. **Contract-first with validation only**: generate DTOs but hand-write controllers — partial protection
3. **Contract-first with full code generation**: generate both server interfaces and client — maximum protection

---

## Decision

We adopt an **OpenAPI 3.1 contract-first** approach where `openapi/brujula-api.v1.yaml` is the authoritative contract owned by the backend repository.

**Backend**:
- The `org.openapi.generator` Gradle plugin (v7.14.0) generates `*Api` interfaces and model DTOs
- Generator: `spring`, library: `spring-boot`, `interfaceOnly=true`
- Generated interfaces live in `build/generated/` (not committed) and are compiled into every build
- An `openApiValidateContract` task validates the YAML on every `check` run — an invalid spec fails the build
- `useSpringBoot3=true` (NOT `useJackson3=true` — generator bug #23289): produces jakarta-compatible interfaces that compile under Boot 4 + Java 25

**Frontend**:
- `@openapitools/openapi-generator-cli` generates `typescript-axios` client into `src/api/generated/` (gitignored, regenerated via `pnpm api:generate`)
- A shared Axios instance in `src/api/client.ts` handles base URL, CSRF, and session cookie; generated `*Api` classes are instantiated once against this instance
- ESLint rule `no-restricted-imports` on `axios` prevents direct Axios usage outside `src/api/client.ts`

The contract copy in `brujula-frontend/openapi/brujula-api.v1.yaml` is tracked but not the source of truth; it must be synced from the backend after every contract bump.

---

## Consequences

**Positive**:
- Contract violations are caught at build time on both sides
- No hand-written API plumbing — controllers implement generated interfaces; frontend uses generated hooks
- TypeScript types are always in sync with the contract

**Negative**:
- openapi-generator's Spring Boot 4 support is still maturing (as of 2025-Q4); code generation may require minor patches on major spec changes
- Generated client is not committed — CI must have network access to run `pnpm api:generate`

**Neutral**:
- The `const: true` OpenAPI 3.1 construct is NOT used in the contract (generator bug #23289 produces broken String→Boolean enum); boolean fields with description constraints are used instead
