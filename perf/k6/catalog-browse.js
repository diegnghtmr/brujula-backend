/**
 * k6 smoke script: Catalog Browse endpoint (QAL-PERF-003)
 *
 * Measures p95 latency for GET /api/v1/programs (authenticated catalog browse).
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
 *     perf/k6/catalog-browse.js
 *
 * NOTE: Actual p95 baseline values are populated in BASELINE.md after the first real run (WU8).
 */

import http from "k6/http";
import { check, sleep } from "k6";

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------
const KC_URL    = __ENV.KC_URL    || "http://localhost:9090";
const KC_REALM  = __ENV.KC_REALM  || "brujula-dev";
const KC_CLIENT = __ENV.KC_CLIENT || "brujula-frontend";
const BASE_URL  = __ENV.BASE_URL  || "http://localhost:8080";
const USERNAME  = __ENV.STUDENT_USER || "student@uniquindio.edu.co";
const PASSWORD  = __ENV.STUDENT_PASS || "REPLACE_ME";

// ---------------------------------------------------------------------------
// Thresholds
// ---------------------------------------------------------------------------
export const options = {
  thresholds: {
    http_req_duration: ["p(95)<1000"],   // 1s budget for catalog list (lightweight query)
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

  // Browse active academic programs
  const res = http.get(
    `${BASE_URL}/api/v1/programs`,
    { headers }
  );

  check(res, {
    "catalog-browse 200": (r) => r.status === 200,
    "catalog-browse has programs": (r) => {
      try {
        const body = JSON.parse(r.body);
        return Array.isArray(body) && body.length > 0;
      } catch (_) {
        return false;
      }
    },
  });

  sleep(1);
}
