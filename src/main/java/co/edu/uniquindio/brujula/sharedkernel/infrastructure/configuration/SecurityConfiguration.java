package co.edu.uniquindio.brujula.sharedkernel.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

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
 *   <li>Spring Session JDBC persistence (auto-configured via properties) — ADR-F1-004.
 *   <li>Session fixation protection (new session on login) — TRD §10.
 *   <li>Logout at {@code POST /auth/logout} — RF-AUT-003.
 *   <li>Method-level security via {@code @EnableMethodSecurity} — BOLA defense, TRD §10.
 * </ul>
 *
 * <p>The {@code SecurityFilterChain} is the cross-cutting composition root and stays in {@code
 * sharedkernel} (ADR-F1-002). OIDC provisioning logic is in {@code
 * identity.infrastructure.adapter.in.oidc} (wired in WU-B).
 *
 * <p>Satisfies: S1-A-8, Spec §OIDC Login Flow §CSRF Protection §Role-Based Authorization §Logout.
 * ADR-F1-001, ADR-F1-002, ADR-F1-004, RNF-SEG-001..005.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

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
   * <p>Authorization matchers (Design §1.3):
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
              // On success: session established via Spring Session JDBC.
              oauth2.defaultSuccessUrl("/", true);
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
        // Authorization rules (Design §1.3)
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
                    // Authenticated: profile and logout
                    .requestMatchers("/me/**", "/auth/logout")
                    .authenticated()
                    // Anything else: must be authenticated
                    .anyRequest()
                    .authenticated())

        // ----------------------------------------------------------------
        // Session management — new session on login (TRD §10 rotation)
        // Storage: Spring Session JDBC via spring-boot-starter-session-jdbc
        // (properties: spring.session.jdbc.table-name, initialize-schema=never)
        // ----------------------------------------------------------------
        .sessionManagement(session -> session.sessionFixation(fixation -> fixation.newSession()))

        // ----------------------------------------------------------------
        // Logout — POST /auth/logout (G2 RESOLUTION)
        // Spring Security handles this BEFORE DispatcherServlet.
        // No explicit controller needed for WU-A; WU-B may add a thin shim.
        // ----------------------------------------------------------------
        .logout(
            logout ->
                logout
                    .logoutUrl("/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("SESSION")
                    .logoutSuccessHandler(
                        (request, response, authentication) -> response.setStatus(204)));

    return http.build();
  }
}
