/**
 * k6 smoke script: List Imports endpoint (QAL-PERF-003)
 *
 * Measures p95 latency for GET /api/v1/imports?page=0&size=20 under STAFF role.
 * Auth: ROPC via Keycloak directAccessGrantsEnabled=true on the brujula-frontend client.
 *
 * Usage (against compose.full.yaml):
 *   k6 run --vus 5 --duration 30s \
 *     -e KC_URL=http://localhost:9090 \
 *     -e KC_REALM=brujula-dev \
 *     -e KC_CLIENT=brujula-frontend \
 *     -e STAFF_USER=staff@uniquindio.edu.co \
 *     -e STAFF_PASS=staff-test-password \
 *     -e BASE_URL=http://localhost:8080 \
 *     perf/k6/list-imports.js
 *
 * NOTE: Actual p95 baseline values are populated in BASELINE.md after the first real run (WU8).
 */

import http from "k6/http";
import { check, sleep } from "k6";

// ---------------------------------------------------------------------------
// Configuration — all values can be overridden via -e flags or environment
// ---------------------------------------------------------------------------
const KC_URL    = __ENV.KC_URL    || "http://localhost:9090";
const KC_REALM  = __ENV.KC_REALM  || "brujula-dev";
const KC_CLIENT = __ENV.KC_CLIENT || "brujula-frontend";
const BASE_URL  = __ENV.BASE_URL  || "http://localhost:8080";
const USERNAME  = __ENV.STAFF_USER || "staff@uniquindio.edu.co";
const PASSWORD  = __ENV.STAFF_PASS || "REPLACE_ME";

// ---------------------------------------------------------------------------
// Thresholds — informational smoke gates (not hard SLOs per QAL-PERF-003)
// ---------------------------------------------------------------------------
export const options = {
  thresholds: {
    http_req_duration: ["p(95)<2000"],   // 2s budget for first baseline run
    http_req_failed:   ["rate<0.05"],    // <5% error rate
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

  // List imports — STAFF actor scoped to assigned programs
  const res = http.get(
    `${BASE_URL}/api/v1/imports?page=0&size=20`,
    { headers }
  );

  check(res, {
    "list-imports 200": (r) => r.status === 200,
    "list-imports has content": (r) => r.body && r.body.length > 0,
  });

  sleep(1);
}
