package co.edu.uniquindio.brujula.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * G3 RESOLUTION — Probe test establishing the correct {@code oidcLogin()} authority shape for
 * Spring Security 7 and {@code @EnableMethodSecurity}.
 *
 * <p>Confirms that:
 *
 * <ul>
 *   <li>{@code oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))} correctly sets
 *       up role-bearing principal.
 *   <li>The {@code ROLE_} prefix convention is respected under {@code @EnableMethodSecurity}.
 *   <li>STUDENT is blocked from {@code /admin/**} (403) and ADMIN is not (404/200).
 *   <li>Authority shape: use {@code SimpleGrantedAuthority("ROLE_X")} — NOT {@code
 *       OidcUserAuthority} with realm_access.
 * </ul>
 *
 * <p>This test establishes the reusable pattern for security tests. When the OIDC user service is
 * wired, this test must still pass — the {@code oidcLogin()} mock bypasses the OIDC provider, so
 * tests are independent of live Keycloak.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SecurityMockOidcLoginShapeTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  /**
   * G3 core: ROLE_STUDENT authority blocks access to /admin/** → 403. This proves that:
   *
   * <ol>
   *   <li>{@code oidcLogin().authorities(...)} correctly populates the SecurityContext.
   *   <li>{@code hasRole('ADMIN')} on /admin/** correctly evaluates to false for ROLE_STUDENT.
   *   <li>The authority shape {@code SimpleGrantedAuthority("ROLE_STUDENT")} is the correct form.
   * </ol>
   */
  @Test
  void oidc_login_with_role_student_authority_is_blocked_from_admin() throws Exception {
    mockMvc
        .perform(
            get("/admin/staff")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  /**
   * G3 core: ROLE_ADMIN authority passes /admin/** filter. The response is 404 (no controller yet)
   * confirming the security layer passed it through.
   */
  @Test
  void oidc_login_with_role_admin_authority_passes_admin_filter() throws Exception {
    mockMvc
        .perform(
            get("/admin/staff")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              assertThat(status)
                  .as(
                      "ROLE_ADMIN must pass /admin/** filter (expect 404 for missing controller,"
                          + " never 403)")
                  .isNotEqualTo(403);
            });
  }

  /**
   * G3 authority-shape verification via authorization semantics: ROLE_STAFF is blocked from
   * /admin/** (403) but passes /staff/** (not 403). This proves that:
   *
   * <ol>
   *   <li>The correct authority prefix is {@code ROLE_} (not bare {@code STAFF}).
   *   <li>{@code hasAnyRole("STAFF", "ADMIN")} accepts {@code ROLE_STAFF}.
   *   <li>{@code hasRole("ADMIN")} rejects {@code ROLE_STAFF} — proving specificity.
   * </ol>
   *
   * <p>This is the canonical resolution: use {@code SimpleGrantedAuthority("ROLE_X")} with {@code
   * oidcLogin().authorities(...)} for all Spring Security 7 + {@code @EnableMethodSecurity} tests.
   * The OIDC user service must produce authorities in the same {@code ROLE_X} shape.
   */
  @Test
  void oidc_login_authority_shape_role_staff_semantics_correct() throws Exception {
    // ROLE_STAFF is blocked from /admin/** (ADMIN only)
    mockMvc
        .perform(
            get("/admin/staff")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden());

    // ROLE_STAFF passes /staff/** (STAFF or ADMIN)
    mockMvc
        .perform(
            get("/staff/something")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              assertThat(status)
                  .as(
                      "G3: ROLE_STAFF must pass /staff/** filter — SimpleGrantedAuthority('ROLE_STAFF')"
                          + " is the correct authority shape for Security 7 hasAnyRole()")
                  .isNotEqualTo(403);
            });
  }
}
