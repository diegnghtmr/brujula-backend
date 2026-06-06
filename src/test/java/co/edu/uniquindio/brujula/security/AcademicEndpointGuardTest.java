package co.edu.uniquindio.brujula.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * RED → GREEN integration tests for the academic-functions guard (S2-E).
 *
 * <p>STRICT TDD: tests written RED before the guard is implemented.
 *
 * <p>Spec: "Unverified user cannot access academic functions" (§App-Driven Registration, S2-E).
 * A user with onboardingState=PENDING_VERIFICATION MUST be blocked (403) from /api/v1/academic/**.
 * ACTIVE users are NOT blocked by this rule.
 *
 * <p>Authorization mechanism (Option B — authority-based, no DB hit per request):
 * {@code IdentityOidcUserService} adds {@code ONBOARDING_ACTIVE} authority when
 * {@code appUser.onboardingState() == ACTIVE}. The {@code SecurityConfiguration} adds an
 * {@code authorizeHttpRequests} rule for {@code /api/v1/academic/**} requiring this authority.
 * Mock tests inject/omit this authority via {@code oidcLogin().authorities(...)}.
 *
 * <p>Rationale for Option B over Option A (custom AuthorizationManager):
 * - No DB hit per request: the authority is established at login time and carried in the session.
 * - Consistent with existing ROLE_ authority pattern (G3 verified).
 * - Authority refreshed correctly on sync-on-login: when PENDING_VERIFICATION user re-authenticates
 *   after email verification, ProvisionUserOnLoginService activates the user and the new session
 *   includes ONBOARDING_ACTIVE.
 * - Keeps SecurityConfiguration clean (just one more requestMatcher rule).
 * - Avoids importing identity module internals into sharedkernel (Modulith safe: authority name
 *   is a String constant, not a domain type).
 *
 * <p>Test seam: no real /api/v1/academic/** endpoints exist yet (Fase 2). The test asserts
 * the authorization DECISION via the HTTP status: 403 for PENDING, not-403 for ACTIVE.
 * A 404 from an ACTIVE user is acceptable and expected (endpoint does not exist yet).
 *
 * <p>Satisfies: S2-E, Spec §Unverified user cannot access academic functions, RN-008.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AcademicEndpointGuardTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // -------------------------------------------------------------------------
  // S2-E-1: PENDING_VERIFICATION user → 403 with proper problem detail
  // -------------------------------------------------------------------------

  @Test
  void pendingVerificationUser_cannotAccess_academicEndpoint_returns403() throws Exception {
    // Arrange: PENDING_VERIFICATION user has ROLE_STUDENT but NOT ONBOARDING_ACTIVE authority
    mockMvc
        .perform(
            get("/api/v1/academic/courses")
                .with(
                    oidcLogin()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_STUDENT"))))
        // ONBOARDING_ACTIVE authority absent → security rule blocks → 403
        .andExpect(status().isForbidden());
  }

  // -------------------------------------------------------------------------
  // S2-E-2: ACTIVE user → NOT blocked (may 404 since endpoint doesn't exist yet)
  // -------------------------------------------------------------------------

  @Test
  void activeUser_canAccess_academicEndpoint_notBlockedByVerificationRule() throws Exception {
    // Arrange: ACTIVE user has both ROLE_STUDENT and ONBOARDING_ACTIVE authority
    mockMvc
        .perform(
            get("/api/v1/academic/courses")
                .with(
                    oidcLogin()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_STUDENT"),
                            new SimpleGrantedAuthority("ONBOARDING_ACTIVE"))))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              // 404 is fine (endpoint doesn't exist yet in Fase 1).
              // Must NOT be 403 (that would mean the verification guard is wrongly blocking ACTIVE).
              assertThat(status)
                  .as(
                      "ACTIVE user must NOT be blocked by verification guard on /api/v1/academic/**"
                          + " (may 404 since no endpoint exists yet, but must not be 403)")
                  .isNotEqualTo(403);
            });
  }

  // -------------------------------------------------------------------------
  // S2-E-3: Unauthenticated → 401 (existing rule, unchanged by the new guard)
  // -------------------------------------------------------------------------

  @Test
  void unauthenticated_academicEndpoint_returns_401_or_redirect() throws Exception {
    mockMvc
        .perform(get("/api/v1/academic/courses"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              // Unauthenticated → 401 or 302 redirect to login (existing rule)
              assertThat(status)
                  .as("Unauthenticated access to /api/v1/academic/** must not return 200")
                  .isGreaterThanOrEqualTo(300);
            });
  }

  // -------------------------------------------------------------------------
  // S2-E-4: /me is NOT gated by the verification rule (control test)
  // -------------------------------------------------------------------------

  @Test
  void pendingVerificationUser_canStillAccess_meEndpoint() throws Exception {
    // PENDING_VERIFICATION user can access /me (not an academic endpoint)
    mockMvc
        .perform(
            get("/me")
                .with(
                    oidcLogin()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              // /me must not be blocked by the academic verification guard.
              // May return 200 (if profile controller exists) or 404, but NOT 403 from this guard.
              assertThat(status)
                  .as("PENDING_VERIFICATION user must access /me (not gated by academic guard)")
                  .isNotEqualTo(403);
            });
  }

  // -------------------------------------------------------------------------
  // W-001: PENDING → 403 MUST carry EMAIL_VERIFICATION_REQUIRED problem detail
  // -------------------------------------------------------------------------

  /**
   * W-001 spec compliance: a PENDING_VERIFICATION user hitting /api/v1/academic/** must receive a
   * 403 response with Content-Type application/problem+json and a body containing
   * {@code "code": "EMAIL_VERIFICATION_REQUIRED"} (RFC 9457 ProblemDetail extension).
   *
   * <p>Currently fails because Spring Security's default {@code AccessDeniedHandlerImpl} returns a
   * generic 403 with no body. This test is RED until a custom {@code AccessDeniedHandler} is
   * registered in {@code SecurityConfiguration.exceptionHandling()}.
   */
  @Test
  void pendingVerificationUser_academicEndpoint_returns403_withEmailVerificationRequiredCode()
      throws Exception {
    mockMvc
        .perform(
            get("/api/v1/academic/courses")
                .with(
                    oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_REQUIRED"));
  }

  // -------------------------------------------------------------------------
  // W-001 regression guard: STUDENT → /admin/** MUST NOT get EMAIL_VERIFICATION_REQUIRED
  // -------------------------------------------------------------------------

  /**
   * Regression guard for W-001: the custom AccessDeniedHandler must be SCOPED to
   * /api/v1/academic/** only. A STUDENT hitting /admin/** must receive a generic 403 (missing role,
   * not missing email verification). The response must NOT contain
   * {@code "code": "EMAIL_VERIFICATION_REQUIRED"} — that would mean we accidentally relabelled
   * every access-denied as an email-verification issue, breaking admin authorization semantics.
   *
   * <p>ONBOARDING_ACTIVE is present so the academic guard is not triggered; the failure comes from
   * lacking ROLE_ADMIN, which is a regular authorization denial.
   */
  @Test
  void studentWithOnboardingActive_hitting_adminEndpoint_gets_generic403_notEmailVerificationCode()
      throws Exception {
    mockMvc
        .perform(
            get("/admin/staff")
                .with(
                    oidcLogin()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_STUDENT"),
                            new SimpleGrantedAuthority("ONBOARDING_ACTIVE"))))
        .andExpect(status().isForbidden())
        .andExpect(
            result -> {
              String body = result.getResponse().getContentAsString();
              assertThat(body)
                  .as(
                      "STUDENT blocked from /admin/** must NOT have code=EMAIL_VERIFICATION_REQUIRED"
                          + " — the handler must be scoped to academic paths only")
                  .doesNotContain("EMAIL_VERIFICATION_REQUIRED");
            });
  }

  // -------------------------------------------------------------------------
  // W-002: ADMIN with ONBOARDING_ACTIVE is NOT blocked from /api/v1/academic/**
  // -------------------------------------------------------------------------

  /**
   * W-002 edge-case coverage: an ADMIN user with ONBOARDING_ACTIVE authority must NOT be blocked
   * by the academic verification guard. OIDC-provisioned ADMIN accounts start with
   * {@code OnboardingState.ACTIVE} and receive ONBOARDING_ACTIVE at login time, so they satisfy
   * the guard. A 404 is acceptable (no academic endpoint exists yet); any result other than 403
   * proves the guard is correctly scoped.
   */
  @Test
  void adminWithOnboardingActive_hitting_academicEndpoint_isNotBlockedByVerificationGuard()
      throws Exception {
    mockMvc
        .perform(
            get("/api/v1/academic/courses")
                .with(
                    oidcLogin()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_ADMIN"),
                            new SimpleGrantedAuthority("ONBOARDING_ACTIVE"))))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              assertThat(status)
                  .as(
                      "ADMIN with ONBOARDING_ACTIVE must NOT be blocked by academic verification"
                          + " guard (may 404 — endpoint not yet implemented)")
                  .isNotEqualTo(403);
            });
  }
}
