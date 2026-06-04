# ADR-002: Hexagonal Architecture with Spring Modulith

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

Brújula requires a clean separation between business logic and infrastructure concerns (database, HTTP, external services). Without a clear layering strategy, domain logic tends to leak into controllers and JPA entities, making the codebase hard to test and evolve independently.

We evaluated three approaches:
1. Layered (MVC-style): simple but leads to tight coupling between layers over time
2. Domain-Driven Design (tactical only): aggregates and repositories without enforced boundaries
3. Hexagonal architecture (ports and adapters): explicit boundary enforcement; domain is pure Java

---

## Decision

We adopt **hexagonal architecture (ports and adapters)** for all functional modules. Each module follows a three-layer package structure:

```
<module>/
├── domain/              — pure business logic (entities, value objects, domain services)
├── application/         — use-case orchestration (ports/in, ports/out, commands, queries)
└── infrastructure/      — adapters (REST controllers, JPA repositories, config)
```

ArchUnit rules (ARQ-001 through ARQ-008) enforce the boundary at build time:
- Domain has no Spring or JPA imports
- Application depends only on domain and port interfaces (not adapters)
- Adapters implement ports; domain never sees adapter types

Spring Modulith's `@ApplicationModule` enforces inter-module dependency declarations.

---

## Consequences

**Positive**:
- Domain is fully testable with plain JUnit (no Spring context needed)
- Adapters are replaceable (e.g., swap JPA for a different persistence technology) without touching domain
- ArchUnit catches violations at build time before they reach production
- Aligns with contract-first API design: generated DTOs are confined to REST adapters

**Negative**:
- More boilerplate: each use case requires a port interface, a command/query object, and a mapper
- Steeper onboarding curve for contributors unfamiliar with hexagonal patterns

**Neutral**:
- Mapper strategy (MapStruct vs manual) is deferred to Fase 1 when real endpoints exist
