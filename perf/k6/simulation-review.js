/**
 * k6 smoke script: Simulation Review / Detail endpoint (QAL-PERF-003)
 *
 * Measures p95 latency for GET /me/simulations/{id} (the read-side detail endpoint
 * that triggers buildValidationSnapshot — the O(N) hot path fixed in DEBT-F7-001).
 *
 * Auth: OIDC authorization-code flow via session-cookie (see lib/auth.js).
 * The backend uses Spring Security oauth2Login() — Bearer tokens are rejected.
 * Login is performed once per VU iteration (session-cookie mode).
 *
 * Endpoint confirmed: /me/simulations/{id} (no /api/v1 prefix — backend serves directly).
 * Role: STUDENT (simulation must be owned by the student user).
 *
 * Setup: creates one simulation in setup() and passes its ID to VUs.
 * VUs log in independently (session cookies cannot be shared across VUs in k6).
 *
 * Prerequisites:
 *   - Stack running: docker compose -f ops/local/compose.full.yaml up -d
 *   - Seed loaded: docker exec -i brujula-postgres psql -U brujula -d planner_db < perf/seed/synthetic.sql
 *   - Synthetic period exists: '99990000-0000-4000-a000-000000000001'
 *   - Synthetic program exists: the first program from /programs endpoint
 *
 * Usage (against compose.full.yaml with synthetic seed loaded):
 *   docker run --rm --network=host \
 *     -v "$PWD/perf/k6:/scripts:ro" \
 *     grafana/k6 run --vus 5 --duration 30s \
 *     -e KC_URL=http://localhost:8090 \
 *     -e KC_REALM=brujula-dev \
 *     -e BASE_URL=http://localhost:8080 \
 *     -e STUDENT_USER=student@uniquindio.edu.co \
 *     -e STUDENT_PASS=Student_Dev_2024! \
 *     /scripts/simulation-review.js
 */

import http from "k6/http";
import { check, sleep, fail } from "k6";
import { login, buildHeaders } from "./lib/auth.js";
import { uuidv4 } from "./lib/uuidv4.js"; // local impl — no outbound fetch at init (air-gapped CI safe)

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------
const KC_URL    = __ENV.KC_URL       || "http://localhost:8090";
const KC_REALM  = __ENV.KC_REALM     || "brujula-dev";
const BASE_URL  = __ENV.BASE_URL     || "http://localhost:8080";
const USERNAME  = __ENV.STUDENT_USER || "student@uniquindio.edu.co";
const PASSWORD  = __ENV.STUDENT_PASS || "Student_Dev_2024!";

// Synthetic period created by perf/seed/synthetic.sql
const SYNTHETIC_PERIOD_ID = "99990000-0000-4000-a000-000000000001";

// ---------------------------------------------------------------------------
// Thresholds — informational smoke gates (not hard SLOs per QAL-PERF-003 / SA-003)
// Scoped to the "simulation-review" tag so OIDC redirect requests (302) are
// excluded from the failure rate count.
// ---------------------------------------------------------------------------
export const options = {
  thresholds: {
    "http_req_duration{name:simulation-review}": ["p(95)<3000"],  // 3s smoke budget
    "http_req_failed{name:simulation-review}":   ["rate<0.05"],   // <5% error rate on actual endpoint
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
};

// ---------------------------------------------------------------------------
// Setup: create one simulation and pass its ID to VUs
// k6 runs setup() once before VUs start. The return value is passed to
// default() as the `data` parameter.
// ---------------------------------------------------------------------------
export function setup() {
  // Login as student to create the simulation
  const { sessionCookie, xsrfToken } = login(BASE_URL, KC_URL, KC_REALM, USERNAME, PASSWORD);
  const headers = buildHeaders(sessionCookie, xsrfToken, false);

  // First, get the first available program ID from the catalog
  const programsRes = http.get(`${BASE_URL}/programs`, { headers });
  check(programsRes, { "setup: GET /programs 200": (r) => r.status === 200 });
  if (programsRes.status !== 200) {
    fail(`setup: GET /programs failed with status ${programsRes.status}`);
  }

  let programId;
  try {
    const programs = JSON.parse(programsRes.body);
    const items = programs.content || programs;
    if (!Array.isArray(items) || items.length === 0) {
      fail("setup: no programs available — run perf/seed/synthetic.sql first");
    }
    programId = items[0].id;
  } catch (e) {
    fail(`setup: could not parse programs response: ${e}`);
  }

  // Create a simulation owned by the student user
  const createHeaders = buildHeaders(sessionCookie, xsrfToken, true);
  const createRes = http.post(
    `${BASE_URL}/me/simulations`,
    JSON.stringify({
      name: "k6 Perf Baseline Simulation",
      periodId: SYNTHETIC_PERIOD_ID,
      programIds: [programId],
    }),
    {
      headers: {
        ...createHeaders,
        "Content-Type": "application/json",
        "Idempotency-Key": uuidv4(),
      },
      tags: { name: "setup-create-simulation" },
    }
  );

  check(createRes, {
    "setup: POST /me/simulations 201": (r) => r.status === 201,
  });

  if (createRes.status !== 201) {
    fail(
      `setup: could not create simulation (status=${createRes.status}): ${createRes.body}`
    );
  }

  let simulationId;
  try {
    const sim = JSON.parse(createRes.body);
    simulationId = sim.id;
  } catch (e) {
    fail(`setup: could not parse simulation response: ${e}`);
  }

  return { simulationId };
}

// ---------------------------------------------------------------------------
// Main VU logic — login once per iteration, then measure the target endpoint
// ---------------------------------------------------------------------------
export default function (data) {
  const { simulationId } = data;

  // Each VU logs in independently (session cookies are user-specific and
  // cannot be shared across k6 VUs — each VU is a separate simulated user
  // session; sharing cookies would serialize login and invalidate sessions).
  const { sessionCookie, xsrfToken } = login(BASE_URL, KC_URL, KC_REALM, USERNAME, PASSWORD);
  const headers = buildHeaders(sessionCookie, xsrfToken, false);

  // --- Target request: GET simulation detail (triggers buildValidationSnapshot) ---
  const res = http.get(
    `${BASE_URL}/me/simulations/${simulationId}`,
    {
      headers,
      tags: { name: "simulation-review" },
    }
  );

  check(res, {
    "simulation-review 200": (r) => r.status === 200,
    "simulation-review has id": (r) => {
      if (r.status !== 200) return false;
      try {
        const body = JSON.parse(r.body);
        return typeof body.id === "string";
      } catch (_) {
        return false;
      }
    },
  });

  sleep(1);
}
