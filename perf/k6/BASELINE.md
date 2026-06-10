# k6 Performance Baseline (QAL-PERF-003)

Recorded during Fase 7 WU8. These are INFORMATIONAL baselines, not build-failing SLOs (per QAL-PERF-003 / SA-003).

---

## Authentication Constraint (WU8 Finding)

The backend uses **session-based OAuth2 Login** (OIDC authorization code flow with Spring Session),
NOT a JWT Resource Server. The k6 scripts were scaffolded for ROPC + Bearer token (a common Resource
Server pattern), but this backend redirects unauthenticated API calls to Keycloak's login page.

**Impact**: The three k6 scripts (`list-imports.js`, `simulation-review.js`, `catalog-browse.js`)
cannot currently authenticate against the backend in a k6 context. Bearer token ROPC approach yields
HTTP 302 → Keycloak login HTML, not the API JSON response.

**To fully enable k6 baseline**: The scripts need to be updated to perform a session-cookie login
flow (POST to `/login`, follow OIDC code exchange, capture the `JSESSIONID` cookie) or the backend
needs a Resource Server JWT mode enabled for API endpoints. This is a WU9 / follow-up task.

---

## Run Parameters

- **VUs**: 5
- **Duration**: 30s
- **Dataset**: `perf/seed/synthetic.sql` (50 programs × 30 courses, 1500 offerings, 1500 sessions, 50 import batches)
- **Stack**: compose.full.yaml, brujula-{postgres, keycloak, backend, frontend, mailpit}

---

## Endpoints

| Endpoint | Script | p50 | p95 | p99 | Error Rate | Auth Status |
|---|---|---|---|---|---|---|
| `GET /api/v1/imports?page=0&size=20` | `list-imports.js` | N/A | N/A | N/A | N/A | BLOCKED — see note above |
| `GET /api/v1/simulations/{id}` | `simulation-review.js` | N/A | N/A | N/A | N/A | BLOCKED — see note above |
| `GET /api/v1/programs` | `catalog-browse.js` | N/A | N/A | N/A | N/A | BLOCKED — see note above |

**Harness status**: Scripts parse and execute without errors. k6 image (`grafana/k6`) confirmed
working via `--network=host`. Synthetic dataset confirmed loaded (50 programs, 50 plans, 1500
courses, 1500 offerings, 1500 schedule options, 1500 schedule sessions, 50 import batches).

The infrastructure is ready; the outstanding work is adapting the auth flow in the k6 scripts
from bearer-token ROPC to session-cookie login (or enabling JWT resource server mode on the backend).

---

## Synthetic Dataset Row Counts (Verified WU8)

Loaded via `docker exec -i brujula-postgres psql -U brujula -d planner_db < perf/seed/synthetic.sql`:

| Table | Synthetic rows |
|---|---|
| `catalog.academic_program` | 50 |
| `catalog.study_plan` | 50 |
| `curriculum.course` | 1500 |
| `offering.course_offering` | 1500 |
| `offering.schedule_option` | 1500 (synthetic) |
| `offering.schedule_session` | 1500 (synthetic) |
| `imports.import_batch` | 50 (VALIDATED) |

---

## How to Run (Updated)

Prerequisites: `compose.full.yaml` running, synthetic seed loaded (`docker exec -i brujula-postgres psql -U brujula -d planner_db < perf/seed/synthetic.sql`).

```bash
# Run via grafana/k6 Docker image (--network=host reaches compose services)
docker run --rm --network=host \
  -v "$PWD/perf/k6:/scripts:ro" \
  grafana/k6 run --vus 5 --duration 30s \
  -e KC_URL=http://localhost:8090 \
  -e KC_REALM=brujula-dev \
  -e KC_CLIENT=brujula-frontend \
  -e STAFF_USER=staff@uniquindio.edu.co \
  -e STAFF_PASS=Staff_Dev_2024! \
  -e BASE_URL=http://localhost:8080 \
  /scripts/list-imports.js

# NOTE: As of WU8 this exits 0 but measures redirect latency, not real API latency.
# See Authentication Constraint section above.
```

---

## Notes

- Thresholds in the scripts are informational smoke gates, not hard SLOs (QAL-PERF-003, SA-003).
- Actual SLO enforcement is deferred to a future phase once baseline values are established.
- Baselines will be measured on developer hardware (local Docker); CI/staging environments will differ.
- The `schedule_session` UUID prefix was corrected from `g0000000-` (invalid hex) to `f1000000-`
  in `perf/seed/synthetic.sql` during WU8. Re-running the seed is idempotent.
