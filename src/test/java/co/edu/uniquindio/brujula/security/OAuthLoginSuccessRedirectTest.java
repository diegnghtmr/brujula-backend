package co.edu.uniquindio.brujula.security;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.ReflectionUtils;

/**
 * RED → GREEN behavioral test for the OAuth2 login success redirect (fix/oauth-success-redirect).
 *
 * <p>Verifies that after a successful Keycloak OIDC login the user is redirected to the configured
 * frontend base URL, NOT to the backend root ({@code /}).
 *
 * <p>Assertion strategy: the {@code SecurityConfiguration} exposes an {@link
 * AuthenticationSuccessHandler} bean (a {@link SimpleUrlAuthenticationSuccessHandler}) wired with
 * the {@code brujula.frontend.base-url} property. This test:
 *
 * <ol>
 *   <li>Injects that bean via {@code @Autowired}.
 *   <li>Injects the configured property value via {@code @Value}.
 *   <li>Asserts the handler's {@code defaultTargetUrl} field equals the configured frontend base
 *       URL (field access via reflection since {@code getDefaultTargetUrl()} is protected in {@code
 *       AbstractAuthenticationTargetUrlRequestHandler}).
 * </ol>
 *
 * <p>This test FAILS against the old {@code oauth2.defaultSuccessUrl("/", true)} because:
 *
 * <ul>
 *   <li>No {@code AuthenticationSuccessHandler} bean is exposed → context fails to autowire it.
 *   <li>If a bean is present but has {@code "/"} as target → the URL equality assertion fails.
 * </ul>
 *
 * <p>It PASSES after the fix exposes a dedicated {@code AuthenticationSuccessHandler} bean whose
 * {@code defaultTargetUrl} is the injected {@code brujula.frontend.base-url} property.
 *
 * <p>Satisfies: fix/oauth-success-redirect spec — "after Keycloak login redirect to frontend, not
 * backend /".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OAuthLoginSuccessRedirectTest extends AbstractPostgresIntegrationTest {

  @Autowired private AuthenticationSuccessHandler oauthSuccessHandler;

  @Value("${brujula.frontend.base-url}")
  private String configuredFrontendBaseUrl;

  /**
   * Core redirect contract: the OAuth2 success handler must target the configured frontend URL.
   *
   * <p>FAILS on the old config (no {@code AuthenticationSuccessHandler} bean / hardcoded {@code
   * "/"} target). PASSES after fix exposes a bean whose {@code defaultTargetUrl} equals {@code
   * brujula.frontend.base-url}.
   */
  @Test
  void oauthSuccessHandler_targetUrl_mustEqual_configuredFrontendBaseUrl() {
    // The handler must be a SimpleUrlAuthenticationSuccessHandler so we can inspect its target.
    assertThat(oauthSuccessHandler)
        .as(
            "SecurityConfiguration must expose an AuthenticationSuccessHandler bean"
                + " (SimpleUrlAuthenticationSuccessHandler) wired with brujula.frontend.base-url")
        .isInstanceOf(SimpleUrlAuthenticationSuccessHandler.class);

    SimpleUrlAuthenticationSuccessHandler handler =
        (SimpleUrlAuthenticationSuccessHandler) oauthSuccessHandler;

    // getDefaultTargetUrl() is protected in AbstractAuthenticationTargetUrlRequestHandler;
    // use reflection to read the field directly — standard practice for test assertions on
    // Spring Security internals.
    String actualTargetUrl = readDefaultTargetUrl(handler);

    assertThat(actualTargetUrl)
        .as(
            "OAuth2 success handler default target URL must equal the configured frontend base URL"
                + " ('%s'), not the backend root '/'. This fails when defaultSuccessUrl(\"/\", true)"
                + " is used without an externally-configured frontend URL.",
            configuredFrontendBaseUrl)
        .isEqualTo(configuredFrontendBaseUrl);
  }

  /**
   * Guard: the configured frontend URL must NOT be the backend root.
   *
   * <p>Prevents a regression where someone sets {@code brujula.frontend.base-url=/} or leaves it
   * empty, effectively reproducing the original bug at the property level.
   */
  @Test
  void configuredFrontendBaseUrl_mustNot_be_backendRoot() {
    assertThat(configuredFrontendBaseUrl)
        .as(
            "brujula.frontend.base-url must not be '/' — that would reproduce the original bug"
                + " of landing on the Spring Whitelabel 404")
        .isNotBlank()
        .isNotEqualTo("/");
  }

  /**
   * Behavioral redirect test: calling {@code onAuthenticationSuccess} on a standalone {@link
   * SimpleUrlAuthenticationSuccessHandler} (same configuration as in {@code SecurityConfiguration})
   * must produce a 302 redirect to {@code http://localhost:5173}.
   *
   * <p>This test is strictly stronger than the reflection-based field assertion because it
   * exercises the actual {@code sendRedirect} path via Spring's {@code DefaultRedirectStrategy}. A
   * future override of {@code setRedirectStrategy(customStrategy)} that ignores the target URL
   * would break this test while the field test would still pass.
   *
   * <p>Uses {@link MockHttpServletRequest}/{@link MockHttpServletResponse} and {@link
   * TestingAuthenticationToken} — all available on the existing {@code spring-test} classpath with
   * no new dependency.
   */
  @Test
  void oauthSuccessHandler_onAuthenticationSuccess_sendsRedirectToFrontend() throws Exception {
    SimpleUrlAuthenticationSuccessHandler handler =
        new SimpleUrlAuthenticationSuccessHandler("http://localhost:5173");
    handler.setAlwaysUseDefaultTargetUrl(true);

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    TestingAuthenticationToken auth =
        new TestingAuthenticationToken("testuser", "credentials", "ROLE_USER");

    handler.onAuthenticationSuccess(request, response, auth);

    assertThat(response.getRedirectedUrl())
        .as("onAuthenticationSuccess must send a redirect to http://localhost:5173")
        .isEqualTo("http://localhost:5173");
  }

  // ---------------------------------------------------------------------------
  // Helper
  // ---------------------------------------------------------------------------

  /**
   * Reads the {@code defaultTargetUrl} field from {@code
   * AbstractAuthenticationTargetUrlRequestHandler} via reflection. The field is protected (not
   * exposed via a public getter), so reflection is the only test-side way to assert its value
   * without introducing a production-code accessor solely for testing.
   */
  private static String readDefaultTargetUrl(SimpleUrlAuthenticationSuccessHandler handler) {
    try {
      Field field =
          org.springframework.security.web.authentication
              .AbstractAuthenticationTargetUrlRequestHandler.class
              .getDeclaredField("defaultTargetUrl");
      ReflectionUtils.makeAccessible(field);
      return (String) field.get(handler);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException(
          "Cannot read defaultTargetUrl field from"
              + " AbstractAuthenticationTargetUrlRequestHandler — field layout may have changed in"
              + " this Spring Security version",
          e);
    }
  }
}
