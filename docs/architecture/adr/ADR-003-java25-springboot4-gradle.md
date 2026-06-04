# ADR-003: Java 25, Spring Boot 4, Gradle Kotlin DSL

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

The backend technology stack determines the lifecycle of the product. The choice involves:
- **Java version**: Java 21 LTS (mature) vs Java 25 (GA September 2025, non-LTS)
- **Spring Boot version**: Boot 3.x (stable) vs Boot 4.0.x (GA November 2025, requires Java 25)
- **Build tool**: Maven vs Gradle; Groovy DSL vs Kotlin DSL

Brújula is a greenfield project starting in late 2025. Choosing Spring Boot 4 + Java 25 gives the longest useful runway before a forced upgrade is needed.

---

## Decision

We adopt **Java 25** as the toolchain language version, **Spring Boot 4.0.3** as the application framework, and **Gradle 9.x with Kotlin DSL** as the build tool.

Key rationale:
- Spring Boot 4.0 requires Java 25 and brings Spring Framework 7, jakarta.* namespaces, and Jackson 3 by default — starting here avoids a future breaking migration
- Spring Modulith 2.0.2 is the first version aligned to Boot 4 / Framework 7
- springdoc-openapi 3.0.x is the Boot 4-compatible release line
- Gradle Kotlin DSL provides type-safe build scripts and IDE autocomplete
- Version catalog (`libs.versions.toml`) pins all dependency versions centrally

Notable Boot 4 breaking changes to be aware of:
- Autoconfiguration is decomposed into modules (e.g., Flyway requires explicit `spring-boot-flyway` dep)
- `TestRestTemplate` and `@AutoConfigureMockMvc` removed from test utilities
- jackson-databind 3.x is the default (Jackson 2.x still works via `jackson-databind-nullable` compat)

---

## Consequences

**Positive**:
- Longest runway before another major Spring upgrade
- Virtual threads (Java 21+) available for improved throughput without reactive complexity
- Pattern matching, records, and sealed interfaces available (Java 21+ features)

**Negative**:
- Some tooling (openapi-generator plugin, google-java-format) needed patching for Java 25 / Boot 4
- Community resources and StackOverflow answers are sparser for Boot 4 vs Boot 3
- JDK 25 is non-LTS; a JDK 25 → 26 upgrade is expected within 6 months

**Neutral**:
- Gradle version catalog (`libs.versions.toml`) is the single source of truth for all dependency versions — all version changes must go through the catalog
