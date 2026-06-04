# ADR-001: Modular Monolith Over Microservices

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

Brújula is a single-institution curricular planning platform for Universidad del Quindío. The initial scope covers one university with a bounded set of modules (identity, curriculum, progress, simulation, etc.) and a development team of 2-5 engineers. The system must be maintainable, testable, and evolvable without the operational overhead of a distributed system at inception.

Microservices were considered because they promise independent deployability and team scaling. However, the team is small, the domain boundaries are still being discovered, and there is no requirement for independent scaling of individual capabilities at this stage.

---

## Decision

We adopt a **modular monolith** architecture using Spring Modulith to enforce module boundaries at compile time and runtime. All modules (identity, administration, programcatalog, curriculum, offering, progress, simulation, imports, audit, sharedkernel) are deployed as a single Spring Boot process.

Module boundaries are enforced via:
- Package-private visibility by default (Spring Modulith treats sub-packages as internal)
- `@ApplicationModule(allowedDependencies = {...})` declaring explicit inter-module dependencies
- `ApplicationModules.of(BrujulaApplication.class).verify()` in the CI test suite

---

## Consequences

**Positive**:
- Single deployable artifact — no service discovery, no distributed tracing from day one
- Transactional consistency across modules without distributed transactions
- Easier local development (one process, one database)
- Spring Modulith allows extracting modules into microservices later with minimal code change if scaling demands it

**Negative**:
- Single point of failure (acceptable for the current scale)
- All modules share the same JVM heap and deployment lifecycle
- Technology diversity per module is not possible

**Neutral**:
- Module coupling is still enforced structurally; the option to split exists if the domain stabilizes and team grows
