/**
 * k6 smoke script: List Imports endpoint (QAL-PERF-003)
 *
 * Measures p95 latency for GET /staff/imports?page=0&size=20 under STAFF role.
 *
 * Auth: OIDC authorization-code flow via session-cookie (see lib/auth.js).
 * The backend uses Spring Security oauth2Login() — Bearer tokens are rejected.
 * Login is performed once per VU iteration (session-cookie mode).
 *
 * Endpoint confirmed: /staff/imports (no /api/v1 prefix — backend serves directly).
 * Role: STAFF (requires STAFF or ADMIN role + at least one assigned program).
 *
 * Prerequisites:
 *   - Stack running: docker compose -f ops/local/compose.full.yaml up -d
 *   - Seed loaded: docker exec -i brujula-postgres psql -U brujula -d planner_db < perf/seed/synthetic.sql
 *   - Staff account seeded with program assignments (see perf/seed/synthetic.sql or run seed patch below)
 *
 * Staff seed patch (run once if list returns empty):
 *   docker exec brujula-postgres psql -U brujula -d planner_db -c "
 *   INSERT INTO administration.staff_account (id, email, full_name, status, version, created_at, updated_at)
 *   VALUES ('19000000-0000-4000-0001-000000000001', 'staff@uniquindio.edu.co', 'Staff Test',
 *           'ACTIVE', 0, NOW(), NOW()) ON CONFLICT (email) DO NOTHING;
 *   INSERT INTO administration.staff_program_assignment (id, staff_id, program_id, active, assigned_at, version)
 *   SELECT ('19100000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
 *          '19000000-0000-4000-0001-000000000001',
 *          ('a0000000-0000-4000-' || LPAD(TO_HEX(i), 4, '0') || '-000000000001')::UUID,
 *          true, NOW(), 0 FROM generate_series(1, 10) AS i
 *   ON CONFLICT (staff_id, program_id) DO NOTHING;"
 *
 * Usage (against compose.full.yaml with synthetic seed loaded):
 *   docker run --rm --network=host \
 *     -v "$PWD/perf/k6:/scripts:ro" \
 *     grafana/k6 run --vus 5 --duration 30s \
 *     -e KC_URL=http://localhost:8090 \
 *     -e KC_REALM=brujula-dev \
 *     -e BASE_URL=http://localhost:8080 \
 *     -e STAFF_USER=staff@uniquindio.edu.co \
 *     -e STAFF_PASS=Staff_Dev_2024! \
 *     /scripts/list-imports.js
 */

import http from "k6/http";
import { check, sleep } from "k6";
import { login, buildHeaders } from "./lib/auth.js";

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------
const KC_URL   = __ENV.KC_URL    || "http://localhost:8090";
const KC_REALM = __ENV.KC_REALM  || "brujula-dev";
const BASE_URL = __ENV.BASE_URL  || "http://localhost:8080";
const USERNAME = __ENV.STAFF_USER || "staff@uniquindio.edu.co";
const PASSWORD = __ENV.STAFF_PASS || "Staff_Dev_2024!";

// ---------------------------------------------------------------------------
// Thresholds — informational smoke gates (not hard SLOs per QAL-PERF-003 / SA-003)
// Scoped to the "list-imports" tag so OIDC redirect requests (302) are
// excluded from the failure rate count.
// ---------------------------------------------------------------------------
export const options = {
  thresholds: {
    "http_req_duration{name:list-imports}": ["p(95)<2000"],  // 2s smoke budget
    "http_req_failed{name:list-imports}":   ["rate<0.05"],   // <5% error rate on actual endpoint
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

  // --- Target request: list imports scoped to staff's assigned programs ---
  const res = http.get(
    `${BASE_URL}/staff/imports?page=0&size=20`,
    {
      headers,
      tags: { name: "list-imports" },
    }
  );

  check(res, {
    "list-imports 200": (r) => r.status === 200,
    "list-imports has content": (r) => {
      if (r.status !== 200) return false;
      try {
        const body = JSON.parse(r.body);
        // ImportPage: { page, size, totalElements, totalPages, content }
        // Require a non-empty content array so an empty/unseeded response fails
        // rather than silently measuring a fast empty-page path.
        return Array.isArray(body.content) && body.content.length > 0;
      } catch (_) {
        return false;
      }
    },
  });

  sleep(1);
}
