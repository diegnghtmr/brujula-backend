/**
 * k6 smoke script: Simulation Review / Detail endpoint (QAL-PERF-003)
 *
 * Measures p95 latency for GET /api/v1/simulations/{id} (the read-side detail endpoint
 * that triggers buildValidationSnapshot — the O(N) hot path fixed in WU3b).
 * Auth: ROPC via Keycloak directAccessGrantsEnabled=true on the brujula-frontend client.
 *
 * Usage (against compose.full.yaml with synthetic seed loaded):
 *   k6 run --vus 5 --duration 30s \
 *     -e KC_URL=http://localhost:9090 \
 *     -e KC_REALM=brujula-dev \
 *     -e KC_CLIENT=brujula-frontend \
 *     -e STUDENT_USER=student@uniquindio.edu.co \
 *     -e STUDENT_PASS=student-test-password \
 *     -e BASE_URL=http://localhost:8080 \
 *     -e SIMULATION_ID=<uuid-from-seed> \
 *     perf/k6/simulation-review.js
 *
 * NOTE: Actual p95 baseline values are populated in BASELINE.md after the first real run (WU8).
 * SIMULATION_ID must be a simulation owned by STUDENT_USER and seeded by synthetic.sql.
 */

import http from "k6/http";
import { check, sleep } from "k6";

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------
const KC_URL       = __ENV.KC_URL       || "http://localhost:9090";
const KC_REALM     = __ENV.KC_REALM     || "brujula-dev";
const KC_CLIENT    = __ENV.KC_CLIENT    || "brujula-frontend";
const BASE_URL     = __ENV.BASE_URL     || "http://localhost:8080";
const USERNAME     = __ENV.STUDENT_USER || "student@uniquindio.edu.co";
const PASSWORD     = __ENV.STUDENT_PASS || "REPLACE_ME";
const SIMULATION_ID = __ENV.SIMULATION_ID || "00000000-0000-0000-0000-000000000001";

// ---------------------------------------------------------------------------
// Thresholds
// ---------------------------------------------------------------------------
export const options = {
  thresholds: {
    http_req_duration: ["p(95)<3000"],   // 3s budget for cold baseline (validation snapshot)
    http_req_failed:   ["rate<0.05"],
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
};

// ---------------------------------------------------------------------------
// Setup: acquire ROPC token once per VU
// ---------------------------------------------------------------------------
function getToken() {
  const tokenUrl =
    `${KC_URL}/realms/${KC_REALM}/protocol/openid-connect/token`;
  const payload = {
    grant_type: "password",
    client_id:  KC_CLIENT,
    username:   USERNAME,
    password:   PASSWORD,
    scope:      "openid profile email",
  };
  const res = http.post(tokenUrl, payload);
  check(res, { "token acquired": (r) => r.status === 200 });
  return JSON.parse(res.body).access_token;
}

// ---------------------------------------------------------------------------
// Main VU logic
// ---------------------------------------------------------------------------
export default function () {
  const token = getToken();
  const headers = {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
    Accept:         "application/json",
  };

  // Simulation detail — triggers buildValidationSnapshot (the WU3b hot path)
  const res = http.get(
    `${BASE_URL}/api/v1/simulations/${SIMULATION_ID}`,
    { headers }
  );

  check(res, {
    "simulation-review 200 or 404": (r) => r.status === 200 || r.status === 404,
    "simulation-review not 500": (r) => r.status !== 500,
  });

  sleep(1);
}
