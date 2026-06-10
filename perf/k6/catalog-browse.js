/**
 * k6 smoke script: Catalog Browse endpoint (QAL-PERF-003)
 *
 * Measures p95 latency for GET /programs (authenticated catalog browse).
 *
 * Auth: OIDC authorization-code flow via session-cookie (see lib/auth.js).
 * The backend uses Spring Security oauth2Login() — Bearer tokens are rejected.
 * Login is performed once per VU iteration (session-cookie mode).
 *
 * Endpoint confirmed: /programs (no /api/v1 prefix — backend serves directly).
 * Role: STUDENT (any authenticated user can browse the catalog).
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
 *     /scripts/catalog-browse.js
 */

import http from "k6/http";
import { check, sleep } from "k6";
import { login, buildHeaders } from "./lib/auth.js";

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------
const KC_URL    = __ENV.KC_URL       || "http://localhost:8090";
const KC_REALM  = __ENV.KC_REALM     || "brujula-dev";
const BASE_URL  = __ENV.BASE_URL     || "http://localhost:8080";
const USERNAME  = __ENV.STUDENT_USER || "student@uniquindio.edu.co";
const PASSWORD  = __ENV.STUDENT_PASS || "Student_Dev_2024!";

// ---------------------------------------------------------------------------
// Thresholds — informational smoke gates (not hard SLOs per QAL-PERF-003 / SA-003)
// Scoped to the "catalog-browse" tag so OIDC redirect requests (302) are
// excluded from the failure rate count.
// ---------------------------------------------------------------------------
export const options = {
  thresholds: {
    "http_req_duration{name:catalog-browse}": ["p(95)<2000"],  // 2s smoke budget
    "http_req_failed{name:catalog-browse}":   ["rate<0.05"],   // <5% error rate on actual endpoint
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
};

// ---------------------------------------------------------------------------
// Main VU logic — login once per iteration, then measure the target endpoint
// ---------------------------------------------------------------------------
export default function () {
  // Perform OIDC login (full authorization-code flow via Keycloak + Spring Session)
  const { sessionCookie, xsrfToken } = login(BASE_URL, KC_URL, KC_REALM, USERNAME, PASSWORD);
  const headers = buildHeaders(sessionCookie, xsrfToken, false);

  // --- Target request: browse active academic programs ---
  const res = http.get(
    `${BASE_URL}/programs`,
    {
      headers,
      tags: { name: "catalog-browse" },
    }
  );

  check(res, {
    "catalog-browse 200": (r) => r.status === 200,
    "catalog-browse has content": (r) => {
      if (r.status !== 200) return false;
      try {
        const body = JSON.parse(r.body);
        // ProgramPage has a 'content' array; plain list also accepted
        const items = body.content || body;
        return Array.isArray(items) && items.length > 0;
      } catch (_) {
        return false;
      }
    },
  });

  sleep(1);
}
