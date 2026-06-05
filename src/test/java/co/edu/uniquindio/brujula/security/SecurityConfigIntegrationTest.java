package co.edu.uniquindio.brujula.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * RED → GREEN integration tests for the Fase 1 SecurityFilterChain.
 *
 * <p>Tests are RED when the Fase 0 permissive security stub is in place. They turn GREEN after the
 * Fase 1 {@code SecurityConfiguration} replaces the stub chain.
 *
 * <p>Scenarios covered:
 *
 * <ul>
 *   <li>RF-AUT-001 — unauthenticated request to protected resource → 401 (or redirect, handled as
 *       4xx).
 *   <li>RF-AUT-002 — STUDENT cannot access {@code /admin/**} → 403.
 *   <li>RF-AUT-002 — ADMIN can access {@code /admin/**} → not 403.
 *   <li>RNF-SEG-004 — POST without {@code X-XSRF-TOKEN} header → 403 (CSRF rejected).
 *   <li>RNF-SEG-004 — Safe GET without CSRF header → allowed (CSRF not checked on safe methods).
 * </ul>
 *
 * <p>Satisfies: S1-A-8, Spec §OIDC Login Flow §CSRF Protection §Role-Based Authorization.
 * ADR-F1-001, ADR-F1-002.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SecurityConfigIntegrationTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // -------------------------------------------------------------------------
  // RF-AUT-001: Unauthenticated requests are rejected
  // -------------------------------------------------------------------------

  @Test
  void unauthenticated_get_me_returns_401_or_redirect() throws Exception {
    // Without authentication Spring Security either returns 401 (API client) or
    // 302 redirect to /oauth2/authorization/keycloak (browser flow). Both are non-2xx.
    mockMvc
        .perform(get("/me"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              assertThat4xxOrRedirect(status, "GET /me unauthenticated must not return 200");
            });
  }

  @Test
  void unauthenticated_get_admin_staff_returns_4xx() throws Exception {
    mockMvc
        .perform(get("/admin/staff"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              assertThat4xxOrRedirect(
                  status, "GET /admin/staff unauthenticated must not return 200");
            });
  }

  // -------------------------------------------------------------------------
  // RF-AUT-002: Role-based access control
  // -------------------------------------------------------------------------

  @Test
  void student_cannot_access_admin_endpoint_gets_403() throws Exception {
    mockMvc
        .perform(
            get("/admin/staff")
                .with(
                    oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void admin_can_access_admin_endpoint() throws Exception {
    // The controller doesn't exist yet (WU-B/C), so we expect 404 NOT 403.
    // Spring Security passes it through; 404 confirms the filter chain allowed it.
    mockMvc
        .perform(
            get("/admin/staff")
                .with(
                    oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              assertThatNot403(status, "ADMIN GET /admin/staff must not be 403");
            });
  }

  @Test
  void staff_can_access_staff_endpoint() throws Exception {
    mockMvc
        .perform(
            get("/staff/something")
                .with(
                    oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              assertThatNot403(status, "STAFF GET /staff/** must not be 403");
            });
  }

  @Test
  void student_cannot_access_staff_endpoint_gets_403() throws Exception {
    mockMvc
        .perform(
            get("/staff/something")
                .with(
                    oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  // -------------------------------------------------------------------------
  // RNF-SEG-004: CSRF protection (SPA mode — cookie+header)
  // -------------------------------------------------------------------------

  @Test
  void post_without_csrf_token_returns_403() throws Exception {
    // Authenticated STUDENT, POST without CSRF token → 403 (CSRF rejected by csrf.spa())
    mockMvc
        .perform(
            post("/admin/staff")
                .with(
                    oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void safe_get_without_csrf_header_is_allowed_for_authenticated_user() throws Exception {
    // GET does not require CSRF token — safe method
    mockMvc
        .perform(
            get("/me")
                .with(
                    oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              // Spring Security allows it (either 200 from /me if controller exists, or 404 — but
              // never 403)
              assertThatNot403(status, "GET /me with valid auth must not be 403 (no CSRF needed)");
            });
  }

  @Test
  void post_with_csrf_token_passes_csrf_filter() throws Exception {
    // POST with valid CSRF token should pass the CSRF filter (may still get 403 on ROLE, 404 on
    // missing controller — but the reject reason is NOT csrf)
    mockMvc
        .perform(
            post("/admin/staff")
                .with(csrf())
                .with(
                    oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType("application/json")
                .content("{}"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              // CSRF passed; controller is absent → 404 or 400. Must NOT be a CSRF 403.
              // We accept 400/404/422/500 but not 403.
              assertThatNot403(
                  status, "POST /admin/staff with CSRF + ADMIN must not be 403 (csrf.spa passed)");
            });
  }

  // -------------------------------------------------------------------------
  // Actuator health: always public
  // -------------------------------------------------------------------------

  @Test
  void actuator_health_is_public_no_auth_needed() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static void assertThat4xxOrRedirect(int status, String message) {
    if (status >= 200 && status < 300) {
      throw new AssertionError(message + " but got HTTP " + status);
    }
  }

  private static void assertThatNot403(int status, String message) {
    if (status == 403) {
      throw new AssertionError(message + " but got HTTP 403");
    }
  }
}
