/**
 * auth.js — OIDC authorization-code login helper for k6 (QAL-PERF-003, Fase 7 debt cleanup).
 *
 * The brujula-backend uses Spring Security oauth2Login() (OIDC authorization code flow with
 * Spring Session JDBC). Bearer tokens are rejected; the API requires a valid SESSION cookie
 * established through the full OIDC redirect flow.
 *
 * Flow implemented here (manual redirect management — k6 does NOT share cookies cross-domain
 * when following redirects automatically, so we handle each hop explicitly):
 *
 *   Step 1: GET {BASE_URL}/programs (no-redirect)
 *           → 302 /oauth2/authorization/keycloak
 *
 *   Step 2: GET {BASE_URL}/oauth2/authorization/keycloak (no-redirect)
 *           → 302 http://localhost:8090/realms/.../auth?...&code_challenge=...
 *           Captures backend cookies (XSRF-TOKEN from Spring CSRF pre-auth init)
 *
 *   Step 3: GET Keycloak auth endpoint (no-redirect)
 *           → 200 HTML login form
 *           Captures Keycloak session cookies (AUTH_SESSION_ID, KC_AUTH_SESSION_HASH, etc.)
 *
 *   Step 4: Parse form action URL (one-time session_code + execution + tab_id)
 *
 *   Step 5: POST credentials to Keycloak form action (no-redirect)
 *           Sends all Keycloak session cookies
 *           → 302 {BASE_URL}/login/oauth2/code/keycloak?code=...&state=...
 *
 *   Step 6: GET {BASE_URL}/login/oauth2/code/keycloak?code=... (no-redirect)
 *           Sends backend cookies captured in Step 2 (OAuth2 state cookie critical)
 *           → 302 / (Spring exchanges code, establishes session, sets SESSION + XSRF-TOKEN)
 *           Captures new SESSION + XSRF-TOKEN cookies
 *
 *   Return: { sessionCookie, xsrfToken }
 *
 * Notes:
 *   - Each VU calls login() once per iteration (session cookies are short-lived).
 *   - GET requests are CSRF-exempt (Spring csrf.spa() only validates X-XSRF-TOKEN on
 *     state-mutating methods: POST, PUT, PATCH, DELETE).
 *   - XSRF-TOKEN cookie value must be sent as X-XSRF-TOKEN header on mutating requests.
 *
 * Usage:
 *   import { login, buildHeaders } from "./lib/auth.js";
 *
 *   export default function () {
 *     const { sessionCookie, xsrfToken } = login(BASE_URL, KC_URL, KC_REALM, USERNAME, PASSWORD);
 *     const headers = buildHeaders(sessionCookie, xsrfToken, false);
 *     const res = http.get(`${BASE_URL}/programs`, { headers });
 *   }
 */

import http from "k6/http";
import { check, fail } from "k6";

// Tell k6 not to count 302 redirects as failures.
// This is important because the OIDC login flow involves several intentional
// 302 redirects (Spring Security → Keycloak → Spring callback → app home).
// Without this, every login adds ~3 "failed" requests to http_req_failed.
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }));

/**
 * Perform OIDC authorization-code login against brujula-backend.
 *
 * @param {string} baseUrl  - Backend base URL, e.g. "http://localhost:8080"
 * @param {string} kcUrl    - Keycloak base URL, e.g. "http://localhost:8090"
 * @param {string} kcRealm  - Keycloak realm name (unused — URL comes from backend redirect)
 * @param {string} username - User email
 * @param {string} password - User password
 * @returns {{ sessionCookie: string, xsrfToken: string }}
 */
export function login(baseUrl, kcUrl, kcRealm, username, password) {
  // ------------------------------------------------------------------
  // Step 1: Hit a protected endpoint to trigger the OIDC redirect chain.
  // Spring Security responds with 302 → /oauth2/authorization/keycloak
  // ------------------------------------------------------------------
  const step1 = http.get(`${baseUrl}/programs`, {
    redirects: 0,
    tags: { name: "oidc-step1-trigger" },
  });

  if (step1.status !== 302) {
    fail(`OIDC step1 unexpected status ${step1.status} (expected 302)`);
  }

  const step2Url = normalizeRedirectUrl(step1.headers["Location"], baseUrl);

  // ------------------------------------------------------------------
  // Step 2: Follow to /oauth2/authorization/keycloak (backend-side).
  // Spring Security creates the OIDC state, generates PKCE code verifier,
  // and redirects to Keycloak. Backend sets initial session cookies here.
  // ------------------------------------------------------------------
  const step2 = http.get(step2Url, {
    redirects: 0,
    tags: { name: "oidc-step2-backend-auth" },
  });

  if (step2.status !== 302) {
    fail(`OIDC step2 unexpected status ${step2.status} (expected 302)`);
  }

  // Capture ALL backend cookies from step2 (Spring Security needs these
  // to validate the OAuth2 state parameter when the code comes back)
  const backendCookies = collectCookies(step2.cookies);

  const kcAuthUrl = step2.headers["Location"];
  if (!kcAuthUrl) {
    fail("OIDC step2: no Location header in response");
  }

  // ------------------------------------------------------------------
  // Step 3: GET Keycloak auth endpoint — returns the HTML login form.
  // Keycloak sets its own session cookies (AUTH_SESSION_ID, etc.)
  // ------------------------------------------------------------------
  const step3 = http.get(kcAuthUrl, {
    redirects: 0,
    tags: { name: "oidc-step3-kc-form" },
  });

  if (step3.status !== 200) {
    fail(`OIDC step3 Keycloak login form status ${step3.status} (expected 200), url=${kcAuthUrl}`);
  }

  const loginHtml = step3.body;
  const kcCookies = collectCookies(step3.cookies);

  // ------------------------------------------------------------------
  // Step 4: Parse the Keycloak login form action URL.
  // Contains one-time tokens: session_code, execution, tab_id.
  // ------------------------------------------------------------------
  const actionMatch = loginHtml.match(/action="([^"]+)"/);
  if (!actionMatch) {
    fail(`OIDC step4: could not find form action in Keycloak HTML. Snippet: ${loginHtml.substring(0, 300)}`);
  }
  const formAction = actionMatch[1].replace(/&amp;/g, "&");

  // ------------------------------------------------------------------
  // Step 5: POST credentials to Keycloak.
  // Keycloak authenticates, then redirects to
  // {baseUrl}/login/oauth2/code/keycloak?code=...&state=...
  // ------------------------------------------------------------------
  const step5 = http.post(
    formAction,
    {
      username: username,
      password: password,
      credentialId: "",
    },
    {
      redirects: 0,
      headers: {
        Cookie: kcCookies,
        "Content-Type": "application/x-www-form-urlencoded",
      },
      tags: { name: "oidc-step5-kc-submit" },
    }
  );

  if (step5.status !== 302) {
    // Credentials wrong or form expired — log HTML snippet for diagnosis
    const snippet = step5.body ? step5.body.substring(0, 400) : "(empty)";
    fail(`OIDC step5 Keycloak credential POST returned ${step5.status} (expected 302). HTML snippet: ${snippet}`);
  }

  // Keycloak redirects to: {baseUrl}/login/oauth2/code/keycloak?code=...&state=...
  const callbackUrl = step5.headers["Location"];
  if (!callbackUrl || callbackUrl.indexOf("login/oauth2/code") === -1) {
    fail(`OIDC step5: unexpected redirect location: ${callbackUrl}`);
  }

  // ------------------------------------------------------------------
  // Step 6: GET the Spring Security callback URL.
  // Spring Security validates the state parameter, exchanges the code for
  // tokens (back-channel call to Keycloak), provisions the user, and
  // creates a Spring Session. Sets SESSION + XSRF-TOKEN cookies.
  // We must send the backendCookies from Step 2 so Spring can validate
  // the OAuth2 state parameter stored in the session.
  // ------------------------------------------------------------------
  const step6 = http.get(callbackUrl, {
    redirects: 0,
    headers: {
      Cookie: backendCookies,
    },
    tags: { name: "oidc-step6-callback" },
  });

  // Spring Security should respond 302 → defaultSuccessUrl ("/")
  if (step6.status !== 302 && step6.status !== 200) {
    fail(`OIDC step6 callback returned ${step6.status} (expected 302 or 200). Body: ${step6.body ? step6.body.substring(0, 300) : "(empty)"}`);
  }

  // Extract the new SESSION and XSRF-TOKEN cookies set by Spring Security
  const sessionCookie = extractCookieValue(step6.cookies, "SESSION");
  const xsrfToken = extractCookieValue(step6.cookies, "XSRF-TOKEN");

  if (!sessionCookie) {
    fail(
      `SESSION cookie not found after OIDC callback. Cookies: ${JSON.stringify(step6.cookies)}. Status: ${step6.status}. Body: ${step6.body ? step6.body.substring(0, 200) : "(empty)"}`
    );
  }

  if (!xsrfToken) {
    // XSRF-TOKEN might be on the final response after the success redirect.
    // Try following one more redirect to get it.
    const successUrl = step6.headers["Location"];
    if (successUrl) {
      const step7 = http.get(normalizeRedirectUrl(successUrl, baseUrl), {
        redirects: 0,
        headers: {
          Cookie: `SESSION=${sessionCookie}`,
        },
        tags: { name: "oidc-step7-success" },
      });
      const xsrfFromStep7 = extractCookieValue(step7.cookies, "XSRF-TOKEN");
      if (xsrfFromStep7) {
        return { sessionCookie, xsrfToken: xsrfFromStep7 };
      }
    }
    // XSRF-TOKEN is required for mutating requests but not for GET.
    // Warn so the issue is visible in k6 output; a later mutating request
    // with an empty X-XSRF-TOKEN header would be rejected by Spring with 403.
    console.warn(
      `[auth] XSRF-TOKEN not captured after OIDC login for session ${sessionCookie.substring(0, 8)}... ` +
        "GET-only scripts continue; any mutating request will 403."
    );
    return { sessionCookie, xsrfToken: "" };
  }

  return { sessionCookie, xsrfToken };
}

/**
 * Collect all cookies from a k6 response into a Cookie header string.
 * Handles the k6 cookies format: { cookieName: [{ value, ... }] }
 * Skips clear-cookies (empty value) and picks the last non-empty value per name.
 *
 * @param {Object} cookies - k6 response cookies object
 * @returns {string} - Cookie header, e.g. "A=1; B=2"
 */
function collectCookies(cookies) {
  if (!cookies) return "";
  const parts = [];
  for (const name in cookies) {
    if (Object.prototype.hasOwnProperty.call(cookies, name)) {
      const list = cookies[name];
      const value = extractCookieValue(cookies, name);
      if (value) {
        parts.push(`${name}=${value}`);
      }
    }
  }
  return parts.join("; ");
}

/**
 * Extract a single cookie value from k6's response cookies object.
 * When multiple Set-Cookie headers exist for the same name (e.g. Spring Security
 * clears the old XSRF-TOKEN with an empty value and then sets a new one), the
 * LAST non-empty value is the authoritative one.
 *
 * @param {Object} cookies - k6 response cookies
 * @param {string} name    - Cookie name
 * @returns {string|null}
 */
function extractCookieValue(cookies, name) {
  if (!cookies || !cookies[name]) return null;
  const list = cookies[name];
  if (!Array.isArray(list) || list.length === 0) return null;
  // Iterate in reverse to find the last non-empty value.
  // Spring Security sends: (1) clear cookie with empty value, (2) new cookie with real value.
  for (let i = list.length - 1; i >= 0; i--) {
    if (list[i].value && list[i].value.length > 0) {
      return list[i].value;
    }
  }
  return null;
}

/**
 * Normalize a Location header URL to an absolute URL.
 * Handles relative paths (e.g. "/oauth2/authorization/keycloak").
 *
 * @param {string} location - Location header value
 * @param {string} baseUrl  - Fallback base URL for relative paths
 * @returns {string}
 */
function normalizeRedirectUrl(location, baseUrl) {
  if (!location) return baseUrl;
  if (location.startsWith("http://") || location.startsWith("https://")) {
    return location;
  }
  return `${baseUrl}${location}`;
}

/**
 * Build the standard request headers for an authenticated request.
 *
 * GET requests: Cookie only (CSRF is exempt for safe methods in Spring csrf.spa()).
 * Mutating requests: also include X-XSRF-TOKEN header.
 *
 * @param {string}  sessionCookie - SESSION cookie value
 * @param {string}  xsrfToken     - XSRF-TOKEN cookie value
 * @param {boolean} isMutating    - true for POST/PUT/PATCH/DELETE
 * @returns {Object}
 */
export function buildHeaders(sessionCookie, xsrfToken, isMutating) {
  const headers = {
    Cookie: `SESSION=${sessionCookie}; XSRF-TOKEN=${xsrfToken}`,
    Accept: "application/json",
    "Content-Type": "application/json",
  };
  if (isMutating && xsrfToken) {
    headers["X-XSRF-TOKEN"] = xsrfToken;
  }
  return headers;
}
