# k6 Performance Baseline (QAL-PERF-003)

**Status**: PLACEHOLDER — populate after first real run against `compose.full.yaml` in WU8.

This file records the p95 latency baseline per endpoint measured with:
- VUs: 5
- Duration: 30s
- Dataset: synthetic.sql (50 programs × 30 courses/offerings)

---

## Endpoints

| Endpoint | Script | p50 | p95 | p99 | Error Rate |
|---|---|---|---|---|---|
| `GET /api/v1/imports?page=0&size=20` | `list-imports.js` | TBD | TBD | TBD | TBD |
| `GET /api/v1/simulations/{id}` | `simulation-review.js` | TBD | TBD | TBD | TBD |
| `GET /api/v1/programs` | `catalog-browse.js` | TBD | TBD | TBD | TBD |

---

## How to Run

Prerequisites: `compose.full.yaml` running, synthetic seed loaded, k6 installed.

```bash
# Load synthetic data
psql -U brujula -d planner_db -f perf/seed/synthetic.sql

# Run list-imports baseline
k6 run --vus 5 --duration 30s \
  -e KC_URL=http://localhost:9090 \
  -e KC_REALM=brujula-dev \
  -e KC_CLIENT=brujula-frontend \
  -e STAFF_USER=staff@uniquindio.edu.co \
  -e STAFF_PASS=staff-test-password \
  -e BASE_URL=http://localhost:8080 \
  perf/k6/list-imports.js

# Run simulation-review baseline (replace SIMULATION_ID with a seeded simulation)
k6 run --vus 5 --duration 30s \
  -e KC_URL=http://localhost:9090 \
  -e KC_REALM=brujula-dev \
  -e KC_CLIENT=brujula-frontend \
  -e STUDENT_USER=student@uniquindio.edu.co \
  -e STUDENT_PASS=student-test-password \
  -e BASE_URL=http://localhost:8080 \
  -e SIMULATION_ID=<uuid> \
  perf/k6/simulation-review.js

# Run catalog-browse baseline
k6 run --vus 5 --duration 30s \
  -e KC_URL=http://localhost:9090 \
  -e KC_REALM=brujula-dev \
  -e KC_CLIENT=brujula-frontend \
  -e STUDENT_USER=student@uniquindio.edu.co \
  -e STUDENT_PASS=student-test-password \
  -e BASE_URL=http://localhost:8080 \
  perf/k6/catalog-browse.js
```

---

## Notes

- Thresholds in the scripts are informational smoke gates, not hard SLOs (QAL-PERF-003, SA-003).
- Actual SLO enforcement is deferred to a future phase once baseline values are established.
- Baselines are measured on developer hardware (local Docker); CI/staging environments will differ.
