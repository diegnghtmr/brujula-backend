# k6 Performance Baseline (QAL-PERF-003)

Recorded during Fase 7 debt-cleanup pass (deferred k6 OIDC auth blocker resolved).
These are INFORMATIONAL baselines, not build-failing SLOs (per QAL-PERF-003 / SA-003).

---

## Authentication — Resolved

The backend uses **session-based OAuth2 Login** (OIDC authorization code flow with Spring Session),
NOT a JWT Resource Server. The WU8 scaffolding used ROPC Bearer tokens, which the backend rejected
with HTTP 302 redirects to Keycloak's login page.

**Resolution**: A shared `perf/k6/lib/auth.js` helper implements the full OIDC authorization code
flow in k6:
1. Trigger OIDC redirect chain from a protected endpoint
2. Follow 302 → `/oauth2/authorization/keycloak` → Keycloak auth endpoint
3. Parse Keycloak HTML login form (extract `action` URL with `session_code` + `execution` + `tab_id`)
4. POST credentials to Keycloak form action; follow redirect to Spring callback
5. Capture `SESSION` + `XSRF-TOKEN` cookies from the Spring Security callback response

**Key findings**:
- Cookie name is `SESSION` (Spring Session JDBC), not `JSESSIONID`
- Spring Security sends two `XSRF-TOKEN` Set-Cookie entries on login: one to clear the old token
  (empty value, expires in the past) and one to set the new token. k6 must pick the last non-empty
  value from the cookie list.
- k6's automatic redirect following (`redirects: N`) does not share cookies across domain boundaries
  (localhost:8080 ↔ localhost:8090), so each OIDC redirect hop must be followed manually with
  explicit `Cookie` headers.
- `brujula-frontend` Keycloak client has `directAccessGrantsEnabled: true` but the backend only
  accepts session-cookie auth — ROPC access tokens are not valid for backend API calls.
- GET requests are CSRF-exempt (Spring `csrf.spa()` only validates `X-XSRF-TOKEN` on POST/PUT/PATCH/DELETE).

---

## Run Parameters

- **VUs**: 5
- **Duration**: 30s per script
- **Dataset**: `perf/seed/synthetic.sql` (50 programs × 30 courses, 1500 offerings, 1500 sessions, 50 import batches)
- **Stack**: compose.full.yaml, brujula-{postgres, keycloak, backend, frontend, mailpit}
- **k6 image**: grafana/k6 (via `docker run --rm --network=host`)
- **Environment**: developer laptop, local Docker, single-node

---

## Endpoints

| Endpoint | Script | Role | p50 | p90 | p95 | p99 | Error Rate |
|---|---|---|---|---|---|---|---|
| `GET /programs` | `catalog-browse.js` | STUDENT | 13.35ms | 18.23ms | **19.03ms** | 21.21ms | 0.00% |
| `GET /staff/imports?page=0&size=20` | `list-imports.js` | STAFF | 14.44ms | 19.28ms | **20.84ms** | 23.08ms | 0.00% |
| `GET /me/simulations/{id}` | `simulation-review.js` | STUDENT | 13.88ms | 19.01ms | **19.71ms** | 23.54ms | 0.00% |

**Note**: These measurements include the endpoint latency only (tagged `{name:endpoint-name}`).
OIDC login overhead (~80–100ms per VU iteration) is excluded from the p95 numbers — it is a
per-iteration cost and would be amortized differently in a realistic load pattern.

**Total check pass rate**: 100% (270+270+274 = 814 checks, 0 failures).

---

## API Path Correction (WU8 → debt cleanup)

WU8 scaffolded scripts used `/api/v1/imports`, `/api/v1/simulations/{id}`, `/api/v1/programs`.
The backend serves WITHOUT the `/api/v1` prefix — actual paths confirmed:

| WU8 scaffolded path | Correct path |
|---|---|
| `GET /api/v1/imports` | `GET /staff/imports` |
| `GET /api/v1/simulations/{id}` | `GET /me/simulations/{id}` |
| `GET /api/v1/programs` | `GET /programs` |

---

## Synthetic Dataset Row Counts (Verified WU8 + debt-cleanup pass)

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

**Staff program assignments** (required for `list-imports.js` to return non-empty results):
The `staff@uniquindio.edu.co` user must have at least one assigned program in
`administration.staff_account` + `administration.staff_program_assignment`. Run once after seed:

```sql
INSERT INTO administration.staff_account (id, email, full_name, status, version, created_at, updated_at)
VALUES ('19000000-0000-4000-0001-000000000001', 'staff@uniquindio.edu.co', 'Staff Test',
        'ACTIVE', 0, NOW(), NOW()) ON CONFLICT (email) DO NOTHING;

INSERT INTO administration.staff_program_assignment (id, staff_id, program_id, active, assigned_at, version)
SELECT ('19100000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
       '19000000-0000-4000-0001-000000000001',
       ('a0000000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
       true, NOW(), 0
FROM generate_series(1, 10) AS i
ON CONFLICT (staff_id, program_id) DO NOTHING;
```

---

## How to Run

Prerequisites: `compose.full.yaml` running, synthetic seed loaded, staff program assignments seeded.

```bash
# Bring up the stack
docker compose -f ops/local/compose.full.yaml up -d --wait

# Load synthetic seed
docker exec -i brujula-postgres psql -U brujula -d planner_db < perf/seed/synthetic.sql

# Seed staff program assignments (run once per fresh DB)
docker exec brujula-postgres psql -U brujula -d planner_db -c "
INSERT INTO administration.staff_account (id, email, full_name, status, version, created_at, updated_at)
VALUES ('19000000-0000-4000-0001-000000000001', 'staff@uniquindio.edu.co', 'Staff Test',
        'ACTIVE', 0, NOW(), NOW()) ON CONFLICT (email) DO NOTHING;
INSERT INTO administration.staff_program_assignment (id, staff_id, program_id, active, assigned_at, version)
SELECT ('19100000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
       '19000000-0000-4000-0001-000000000001',
       ('a0000000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
       true, NOW(), 0
FROM generate_series(1, 10) AS i
ON CONFLICT (staff_id, program_id) DO NOTHING;"

# Run each script (5 VUs, 30s)
docker run --rm --network=host \
  -v "$PWD/perf/k6:/scripts:ro" \
  grafana/k6 run --vus 5 --duration 30s \
  -e KC_URL=http://localhost:8090 \
  -e KC_REALM=brujula-dev \
  -e BASE_URL=http://localhost:8080 \
  -e STUDENT_USER=student@uniquindio.edu.co \
  -e STUDENT_PASS=Student_Dev_2024! \
  /scripts/catalog-browse.js

docker run --rm --network=host \
  -v "$PWD/perf/k6:/scripts:ro" \
  grafana/k6 run --vus 5 --duration 30s \
  -e KC_URL=http://localhost:8090 \
  -e KC_REALM=brujula-dev \
  -e BASE_URL=http://localhost:8080 \
  -e STAFF_USER=staff@uniquindio.edu.co \
  -e STAFF_PASS=Staff_Dev_2024! \
  /scripts/list-imports.js

docker run --rm --network=host \
  -v "$PWD/perf/k6:/scripts:ro" \
  grafana/k6 run --vus 5 --duration 30s \
  -e KC_URL=http://localhost:8090 \
  -e KC_REALM=brujula-dev \
  -e BASE_URL=http://localhost:8080 \
  -e STUDENT_USER=student@uniquindio.edu.co \
  -e STUDENT_PASS=Student_Dev_2024! \
  /scripts/simulation-review.js

# Tear down
docker compose -f ops/local/compose.full.yaml down --remove-orphans
```

---

## Notes

- Thresholds in the scripts are informational smoke gates, not hard SLOs (QAL-PERF-003, SA-003).
- Actual SLO enforcement is deferred to a future phase once baseline values are established.
- Baselines were measured on developer hardware (local Docker); CI/staging environments will differ.
- Per-VU OIDC login (~80–100ms overhead per iteration) is normal for session-cookie-based load tests.
  In a production load test, you would use a session pool or longer per-VU session duration.
- The `simulation-review.js` script creates one simulation per run in `setup()` and shares its ID
  across all VUs. The `GET /me/simulations/{id}` call triggers `buildValidationSnapshot` (the
  DEBT-F7-001 batch-credits hot path that was optimized in WU8).
- All VUs authenticate as the same seeded user (same credentials). If Keycloak is ever configured
  with a max-sessions-per-user policy, this smoke harness may need per-VU users to avoid mid-test
  session invalidation.
