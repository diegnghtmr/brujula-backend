# Brújula Backend

Brújula is an academic planner for Universidad del Quindío students. This backend serves the program catalog and curricula, tracks self-reported course attempts, and checks prerequisites, credit limits, and timetable overlaps when students simulate a semester. It supports personal planning; it neither certifies grades nor enrolls students in courses.

## Repositories

- [brujula-frontend](https://github.com/diegnghtmr/brujula-frontend) — React web app for browsing curricula, recording progress, and building a schedule. Its Axios client is generated from a copy of this repository's [OpenAPI contract](openapi/brujula-api.v1.yaml).

## Stack

- Java 25, Spring Boot 4.0.3, and Spring Modulith 2.0.2
- Gradle Wrapper 9.0.0 and OpenAPI Generator 7.14.0
- PostgreSQL 18.4 and Flyway migrations
- Keycloak 26.4.5 for sign-in; Mailpit 1.27.3 for local email
- JUnit, Testcontainers 1.21.0, and ArchUnit 1.4.1

## Run locally

With Docker and Compose, check out the frontend as a sibling directory named `brujula-frontend`. From the backend root, start the full stack:

```sh
docker compose -f ops/local/compose.full.yaml up -d
```

The API listens on `http://localhost:8080` and the web app on `http://localhost:5173`. The web app's development proxy strips `/api/v1` before forwarding requests to Spring. For a host JVM instead, use Java 25 and the `local` profile. See the [database setup and move guide](docs/database/change-pc-guide.md) for host environment settings. Start only the supporting services, then run the backend:

```sh
docker compose -f ops/local/compose.yaml up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Tests

```sh
./gradlew test
./gradlew check
```

Docker is needed for the PostgreSQL integration tests. `check` adds OpenAPI validation, Spotless, and the JaCoCo coverage gate.

## Repository structure

| Path | Purpose |
| --- | --- |
| `src/main/java/co/edu/uniquindio/brujula/` | Application modules, including catalog, curriculum, progress, offerings, and simulation |
| `src/main/resources/` | Runtime configuration and database migrations |
| `src/test/` | Unit, architecture, and integration tests |
| `openapi/` | API contract and input for server interface generation |
| `ops/local/` | Compose definitions for supporting services and the full stack |
| `docs/architecture/adr/` | Decisions on module boundaries, the HTTP contract, and identity |

## License

MIT, see [LICENSE](LICENSE).
