package co.edu.uniquindio.brujula.sharedkernel.infrastructure.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.StringUtils;

/**
 * RP-initiated (single) logout success handler for Keycloak — RF-AUT-003.
 *
 * <p>The backend is the OIDC Relying Party: it holds the session and the OIDC tokens; the SPA never
 * sees them. A local-only logout therefore does NOT end the Keycloak SSO session — Keycloak
 * silently re-authenticates the browser and bounces the user straight back in. To perform a true
 * single logout the browser must be redirected to the Keycloak {@code end_session_endpoint} with an
 * {@code id_token_hint}.
 *
 * <p>Contract (chosen to avoid breaking the generated OpenAPI client, whose {@code logout} returns
 * {@code void}): this handler keeps the response status at {@code 204 No Content} and adds the
 * Keycloak end-session URL as the {@value #LOGOUT_REDIRECT_HEADER} response header. The SPA reads
 * that header and performs {@code window.location.assign(...)}. When the header is absent (no OIDC
 * principal), the SPA falls back to a local {@code /login} navigation.
 *
 * <p><strong>Why the URL is composed from the ID token issuer, not {@code
 * brujula.keycloak.auth-server-url}:</strong> OIDC discovery is intentionally disabled (static
 * provider URIs in {@code application.yaml}), so the {@code ClientRegistration} {@code
 * configurationMetadata} carries no {@code end_session_endpoint} and Spring's {@code
 * OidcClientInitiatedLogoutSuccessHandler} cannot be used. The end-session URL is a redirect the
 * <em>browser</em> follows, so its host must be browser-reachable. {@code
 * brujula.keycloak.auth-server-url} is the Docker-internal Admin REST base ({@code
 * http://keycloak:8080} in compose) — unreachable from the host browser. The ID token issuer, by
 * contrast, is pinned to the browser-facing host via {@code KC_HOSTNAME} (e.g. {@code
 * http://localhost:8090/realms/brujula-dev}) and always matches whichever host actually issued the
 * token. Keycloak's end-session endpoint is the sibling of the authorization endpoint at {@code
 * {issuer}/protocol/openid-connect/logout}.
 *
 * <p>Query parameters (all URL-encoded):
 *
 * <ul>
 *   <li>{@code id_token_hint} — the OIDC ID token value, so Keycloak can identify the session to
 *       end without prompting.
 *   <li>{@code post_logout_redirect_uri} — the SPA landing page after Keycloak clears the SSO
 *       session (must be registered as {@code post.logout.redirect.uris} on the Keycloak client).
 *   <li>{@code client_id} — the login client id (belt-and-suspenders for Keycloak validation).
 * </ul>
 *
 * <p>Session invalidation and {@code SESSION} cookie deletion remain configured on the Spring
 * Security logout filter; this handler only augments the successful response.
 */
public final class KeycloakRpInitiatedLogoutSuccessHandler implements LogoutSuccessHandler {

  /** Response header carrying the Keycloak end-session URL for the SPA to redirect to. */
  public static final String LOGOUT_REDIRECT_HEADER = "X-Logout-Redirect";

  private static final String END_SESSION_PATH_SUFFIX = "protocol/openid-connect/logout";

  private final String clientId;
  private final String postLogoutRedirectUri;

  /**
   * @param clientId the Keycloak login client id ({@code
   *     spring.security.oauth2.client.registration.keycloak.client-id}).
   * @param postLogoutRedirectUri the absolute SPA URL Keycloak redirects to after ending the SSO
   *     session (e.g. {@code http://localhost:5173/login}).
   */
  public KeycloakRpInitiatedLogoutSuccessHandler(String clientId, String postLogoutRedirectUri) {
    this.clientId = clientId;
    this.postLogoutRedirectUri = postLogoutRedirectUri;
  }

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    String endSessionUrl = buildEndSessionUrl(authentication);
    if (endSessionUrl != null) {
      response.setHeader(LOGOUT_REDIRECT_HEADER, endSessionUrl);
    }
    // Keep the 204 contract regardless: the generated OpenAPI client expects void/no-content.
    response.setStatus(HttpStatus.NO_CONTENT.value());
  }

  /**
   * Composes the Keycloak end-session URL from the ID token of the authentication being logged out.
   *
   * @return the fully-composed, URL-encoded end-session URL, or {@code null} when the
   *     authentication is not an OIDC login (no {@code id_token_hint} available — nothing to
   *     single-logout).
   */
  private String buildEndSessionUrl(Authentication authentication) {
    if (!(authentication instanceof OAuth2AuthenticationToken token)
        || !(token.getPrincipal() instanceof OidcUser oidcUser)) {
      return null;
    }
    OidcIdToken idToken = oidcUser.getIdToken();
    if (idToken == null || idToken.getIssuer() == null) {
      return null;
    }

    String issuer = idToken.getIssuer().toString();
    StringBuilder url = new StringBuilder(issuer.length() + 256);
    url.append(issuer);
    if (!issuer.endsWith("/")) {
      url.append('/');
    }
    url.append(END_SESSION_PATH_SUFFIX);
    url.append("?id_token_hint=").append(encode(idToken.getTokenValue()));
    url.append("&post_logout_redirect_uri=").append(encode(postLogoutRedirectUri));
    if (StringUtils.hasText(clientId)) {
      url.append("&client_id=").append(encode(clientId));
    }
    return url.toString();
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
