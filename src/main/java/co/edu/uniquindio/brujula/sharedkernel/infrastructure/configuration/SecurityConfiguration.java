package co.edu.uniquindio.brujula.sharedkernel.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.AntPathMatcher;

/**
 * Security configuration for Fase 1.
 *
 * <p>Replaces the Fase 0 permissive stub with a production-grade security filter chain:
 *
 * <ul>
 *   <li>OIDC login via Keycloak ({@code oauth2Login}) — RF-AUT-001, ADR-007.
 *   <li>CSRF protection via {@code csrf.spa()} (Security 7 verified fact #1) — RNF-SEG-004,
 *       ADR-F1-001.
 *   <li>Role-based authorization ({@code authorizeHttpRequests}) — RF-AUT-002, ADR-F1-002.
 *   <li>Spring Session JDBC persistence (auto-configured via properties).
 *   <li>Session fixation protection (new session on login).
 *   <li>Logout at {@code POST /auth/logout} — RF-AUT-003.
 *   <li>Method-level security via {@code @EnableMethodSecurity} — BOLA defense.
 * </ul>
 *
 * <p>The {@code SecurityFilterChain} is the cross-cutting composition root and stays in {@code
 * sharedkernel} (ADR-F1-002). OIDC provisioning logic is in {@code
 * identity.infrastructure.adapter.in.oidc} (wired in WU-B).
 *
 * <p>Satisfies: S1-A-8. ADR-F1-001, ADR-F1-002, RNF-SEG-001..005.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

  /**
   * Frontend SPA base URL injected from {@code brujula.frontend.base-url}.
   *
   * <p>Used as the post-login redirect target so that a successful Keycloak OIDC login sends the
   * browser to the SPA instead of the API-only backend root (fix/oauth-success-redirect). Override
   * per environment via the {@code BRUJULA_FRONTEND_BASE_URL} environment variable or a
   * profile-specific YAML entry.
   */
  @Value("${brujula.frontend.base-url}")
  private String frontendBaseUrl;

  /**
   * Keycloak login client id injected from {@code
   * spring.security.oauth2.client.registration.keycloak.client-id} (value {@code brujula-backend}).
   *
   * <p>Sent as the {@code client_id} query parameter on the Keycloak RP-initiated logout URL —
   * belt-and-suspenders for Keycloak's post-logout redirect validation. Reuses the same property
   * that configures the OIDC login registration so there is a single source of truth.
   */
  @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
  private String keycloakClientId;

  /**
   * OIDC user service — wired from identity module (WU-B, S1-B-5). Triggers user provisioning on
   * Keycloak OIDC login. Injected by the generic Spring interface so this sharedkernel config does
   * NOT import a concrete class from the identity module (Modulith boundary: sharedkernel has no
   * allowed outbound module dependencies). Optional: allows startup when identity beans are absent
   * (e.g. slice tests that don't load the full context).
   */
  @Autowired(required = false)
  private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;

  /**
   * Production security filter chain.
   *
   * <p>Authorization matchers:
   *
   * <ul>
   *   <li>{@code permitAll}: actuator health, springdoc (local profile), auth registration +
   *       verification + recovery.
   *   <li>{@code /admin/**}: ADMIN role only.
   *   <li>{@code /staff/**}: STAFF or ADMIN role.
   *   <li>{@code /me/**}, {@code /auth/logout}: any authenticated user.
   *   <li>Anything else: must be authenticated.
   * </ul>
   *
   * <p>G2 RESOLUTION: The OpenAPI contract declares {@code /auth/logout} with {@code operationId:
   * logout}. Spring Security's {@code logout()} handler is configured at {@code POST /auth/logout}
   * — no explicit controller needed. Spring handles the session invalidation and cookie clearing;
   * the generated {@code AuthApi} interface is implemented in WU-B (identity module) with a thin
   * shim that delegates to Spring's session invalidation. For WU-A the logout endpoint is provided
   * purely by Spring Security (no controller required — the Spring Security logout filter handles
   * it before the DispatcherServlet is reached).
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // ----------------------------------------------------------------
        // OIDC login — Keycloak provider (registration id: keycloak)
        // ----------------------------------------------------------------
        .oauth2Login(
            oauth2 -> {
              // Default authorization endpoint: /oauth2/authorization/keycloak
              // Default redirect-uri (callback): /login/oauth2/code/keycloak
              // On success: redirect to the frontend SPA (fix/oauth-success-redirect).
              // alwaysUse=true: ignore any saved backend request (saved request would be an
              // API path with no page — landing there produces a Whitelabel 404 for the browser).
              oauth2.successHandler(oauthSuccessHandler());
              // Wire IdentityOidcUserService for user provisioning (WU-B, S1-B-5).
              // When present, maps Keycloak claims → AppUser and sets ROLE_ authorities.
              if (oidcUserService != null) {
                oauth2.userInfoEndpoint(ui -> ui.oidcUserService(oidcUserService));
              }
            })

        // ----------------------------------------------------------------
        // CSRF — SPA mode (Security 7 verified fact #1)
        // Auto-configures:
        //   • CookieCsrfTokenRepository with non-HttpOnly XSRF-TOKEN cookie
        //   • SPA request handler that resolves the raw (un-XOR'd) token
        // Frontend reads XSRF-TOKEN cookie, sends X-XSRF-TOKEN header.
        // ----------------------------------------------------------------
        .csrf(csrf -> csrf.spa())

        // ----------------------------------------------------------------
        // Authorization rules
        // ----------------------------------------------------------------
        .authorizeHttpRequests(
            authorize ->
                authorize
                    // Public: health, springdoc (local profile guard in config)
                    .requestMatchers(
                        "/actuator/health/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs")
                    .permitAll()
                    // Public: auth registration, verification, recovery (security: [] in spec)
                    .requestMatchers(
                        "/auth/registration",
                        "/auth/email-verifications/**",
                        "/auth/password-recovery/**")
                    .permitAll()
                    // ADMIN-only: admin endpoints
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    // STAFF or ADMIN: staff endpoints
                    .requestMatchers("/staff/**")
                    .hasAnyRole("STAFF", "ADMIN")
                    // S2-E: Academic functions — require ONBOARDING_ACTIVE authority.
                    // PENDING_VERIFICATION users lack this authority → 403 with problem detail.
                    // ACTIVE users have this authority (added by IdentityOidcUserService on login).
                    // Rationale (Option B): authority carried in session — no DB hit per request.
                    // Authority refreshed on next login after email verification (sync-on-login,
                    // obs #1574). SecurityConfiguration stays in sharedkernel with no identity
                    // module import — the authority name is a plain String constant.
                    .requestMatchers("/api/v1/academic/**")
                    .hasAuthority("ONBOARDING_ACTIVE")
                    // Authenticated: profile and logout
                    .requestMatchers("/me/**", "/auth/logout")
                    .authenticated()
                    // Anything else: must be authenticated
                    .anyRequest()
                    .authenticated())

        // ----------------------------------------------------------------
        // Exception handling — path-scoped AccessDeniedHandler (W-001)
        // ONLY /api/v1/academic/** receives EMAIL_VERIFICATION_REQUIRED problem detail.
        // All other access-denied cases (e.g. STUDENT→/admin/**) get the default generic 403.
        // Modulith-safe: no identity module import; uses plain String code + path check only.
        // ----------------------------------------------------------------
        .exceptionHandling(
            exceptions -> exceptions.accessDeniedHandler(academicAccessDeniedHandler()))

        // ----------------------------------------------------------------
        // Session management — new session on login (session fixation rotation)
        // Storage: Spring Session JDBC via spring-boot-starter-session-jdbc
        // (properties: spring.session.jdbc.table-name, initialize-schema=never)
        // ----------------------------------------------------------------
        .sessionManagement(session -> session.sessionFixation(fixation -> fixation.newSession()))

        // ----------------------------------------------------------------
        // Logout — POST /auth/logout (G2 RESOLUTION) + RP-initiated single logout (RF-AUT-003)
        // Spring Security handles this BEFORE DispatcherServlet.
        // Session invalidation + SESSION cookie deletion stay here; the success handler keeps the
        // 204 contract and adds the X-Logout-Redirect header (Keycloak end_session URL) so the SPA
        // can end the Keycloak SSO session too (not just the local backend session).
        // ----------------------------------------------------------------
        .logout(
            logout ->
                logout
                    .logoutUrl("/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("SESSION")
                    .logoutSuccessHandler(oidcLogoutSuccessHandler()));

    return http.build();
  }

  /**
   * OAuth2 login success handler — redirects to the frontend SPA after a successful Keycloak OIDC
   * authentication (fix/oauth-success-redirect).
   *
   * <p>Uses {@link SimpleUrlAuthenticationSuccessHandler} with {@code alwaysUse=true} so the
   * browser always lands on the SPA root, ignoring any saved backend request URL. A saved request
   * pointing at an API path would produce a Whitelabel 404 — not a valid SPA landing page.
   *
   * <p>The target URL is injected from {@code brujula.frontend.base-url} (default {@code
   * http://localhost:5173} for local dev; override via {@code BRUJULA_FRONTEND_BASE_URL} env var in
   * production or Docker Compose).
   *
   * <p>Exposed as a {@code @Bean} to allow behavioral test assertions on the configured redirect
   * target (see {@code OAuthLoginSuccessRedirectTest}).
   */
  @Bean
  public AuthenticationSuccessHandler oauthSuccessHandler() {
    SimpleUrlAuthenticationSuccessHandler handler =
        new SimpleUrlAuthenticationSuccessHandler(frontendBaseUrl);
    handler.setAlwaysUseDefaultTargetUrl(true);
    return handler;
  }

  /**
   * RP-initiated (single) logout success handler — RF-AUT-003.
   *
   * <p>On a successful {@code POST /auth/logout} it keeps the {@code 204} contract and adds the
   * Keycloak {@code end_session} URL as the {@code X-Logout-Redirect} response header so the SPA
   * can redirect the browser to Keycloak and end the SSO session (a local-only logout lets Keycloak
   * silently re-authenticate the user via SSO).
   *
   * <p>The {@code post_logout_redirect_uri} is the SPA {@code /login} page derived from {@code
   * brujula.frontend.base-url} (single source of truth for the SPA host). A trailing slash on the
   * base URL is normalized so the redirect never becomes {@code //login}. The end-session host
   * itself is taken from the ID token issuer inside the handler — see {@link
   * KeycloakRpInitiatedLogoutSuccessHandler} for why {@code brujula.keycloak.auth-server-url} (the
   * Docker-internal Admin API URL) must NOT be used for a browser redirect.
   *
   * <p>Exposed as a {@code @Bean} for testability, matching {@link #oauthSuccessHandler()}.
   */
  @Bean
  public LogoutSuccessHandler oidcLogoutSuccessHandler() {
    String base =
        frontendBaseUrl.endsWith("/")
            ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
            : frontendBaseUrl;
    return new KeycloakRpInitiatedLogoutSuccessHandler(keycloakClientId, base + "/login");
  }

  /**
   * Path-scoped {@link AccessDeniedHandler} for the academic guard (W-001).
   *
   * <p>Behavior:
   *
   * <ul>
   *   <li>Request path matches {@code /api/v1/academic/**}: respond with 403 + {@code Content-Type:
   *       application/problem+json} + body containing {@code "code": "EMAIL_VERIFICATION_REQUIRED"}
   *       (RFC 9457 ProblemDetail extension). This satisfies the spec contract: "403 with problem
   *       detail indicating email verification required".
   *   <li>Any other path: delegate to {@link AccessDeniedHandlerImpl} (default Spring Security
   *       behavior — generic 403, no body). This preserves existing semantics for admin/staff
   *       authorization failures and does NOT relabel them as email-verification issues.
   * </ul>
   *
   * <p>Modulith-safe: uses {@code AntPathMatcher} + plain String constants — no identity module
   * import. The handler lives in {@code sharedkernel} and has zero domain-module dependencies. JSON
   * is written inline to avoid an ObjectMapper dependency in sharedkernel configuration.
   */
  private AccessDeniedHandler academicAccessDeniedHandler() {
    AntPathMatcher pathMatcher = new AntPathMatcher();
    AccessDeniedHandlerImpl defaultHandler = new AccessDeniedHandlerImpl();

    return (request, response, accessDeniedException) -> {
      String path = request.getRequestURI();

      if (pathMatcher.match("/api/v1/academic/**", path)) {
        // Spec contract: 403 + application/problem+json + code=EMAIL_VERIFICATION_REQUIRED.
        // JSON is written inline — keeps sharedkernel free from ObjectMapper dependency.
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response
            .getWriter()
            .write(
                """
                {
                  "title": "Access Denied",
                  "status": 403,
                  "detail": "Se requiere verificar el correo electr\\u00f3nico para acceder a funciones acad\\u00e9micas.",
                  "code": "EMAIL_VERIFICATION_REQUIRED"
                }
                """);
      } else {
        // All other paths: default Spring Security behavior (generic 403, no body).
        defaultHandler.handle(request, response, accessDeniedException);
      }
    };
  }
}
