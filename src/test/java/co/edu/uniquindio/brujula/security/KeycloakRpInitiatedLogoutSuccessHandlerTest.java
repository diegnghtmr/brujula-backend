package co.edu.uniquindio.brujula.security;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.sharedkernel.infrastructure.configuration.KeycloakRpInitiatedLogoutSuccessHandler;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Pure unit test (no Spring context / Testcontainers) for {@link
 * KeycloakRpInitiatedLogoutSuccessHandler}.
 *
 * <p>Verifies RF-AUT-003: on logout the handler keeps the {@code 204} contract and — for an OIDC
 * login — adds the {@code X-Logout-Redirect} header carrying the Keycloak {@code end_session} URL
 * composed from the ID token issuer, with {@code id_token_hint}, {@code post_logout_redirect_uri}
 * and {@code client_id} (all URL-encoded). For a non-OIDC authentication no header is added.
 */
class KeycloakRpInitiatedLogoutSuccessHandlerTest {

  private static final String CLIENT_ID = "brujula-backend";
  private static final String POST_LOGOUT_REDIRECT_URI = "http://localhost:5173/login";
  private static final String ISSUER = "http://localhost:8090/realms/brujula-dev";
  private static final String ID_TOKEN_VALUE = "header.payload.signature";

  private final KeycloakRpInitiatedLogoutSuccessHandler handler =
      new KeycloakRpInitiatedLogoutSuccessHandler(CLIENT_ID, POST_LOGOUT_REDIRECT_URI);

  @Test
  void oidcLogout_setsStatus204_andComposesEndSessionRedirectHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onLogoutSuccess(request, response, oidcAuthentication());

    assertThat(response.getStatus()).as("logout must keep the 204 contract").isEqualTo(204);

    String redirect =
        response.getHeader(KeycloakRpInitiatedLogoutSuccessHandler.LOGOUT_REDIRECT_HEADER);
    assertThat(redirect)
        .as("X-Logout-Redirect must be the browser-facing Keycloak end_session URL")
        .isNotNull()
        .startsWith(ISSUER + "/protocol/openid-connect/logout?")
        .contains("id_token_hint=" + encode(ID_TOKEN_VALUE))
        .contains("post_logout_redirect_uri=" + encode(POST_LOGOUT_REDIRECT_URI))
        .contains("client_id=" + encode(CLIENT_ID));
  }

  @Test
  void nonOidcAuthentication_setsStatus204_andOmitsRedirectHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onLogoutSuccess(
        request,
        response,
        new TestingAuthenticationToken("plain-user", "credentials", "ROLE_USER"));

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getHeader(KeycloakRpInitiatedLogoutSuccessHandler.LOGOUT_REDIRECT_HEADER))
        .as("no id_token_hint is available for a non-OIDC login → no RP-initiated logout header")
        .isNull();
  }

  @Test
  void nullAuthentication_setsStatus204_andOmitsRedirectHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onLogoutSuccess(request, response, null);

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getHeader(KeycloakRpInitiatedLogoutSuccessHandler.LOGOUT_REDIRECT_HEADER))
        .isNull();
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static OAuth2AuthenticationToken oidcAuthentication() {
    Instant now = Instant.now();
    OidcIdToken idToken =
        OidcIdToken.withTokenValue(ID_TOKEN_VALUE)
            .issuer(ISSUER)
            .subject("test-subject")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .build();
    OidcUser oidcUser =
        new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")), idToken, "sub");
    return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "keycloak");
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
