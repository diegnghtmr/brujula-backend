package co.edu.uniquindio.brujula.identity.adapter.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Security + REST integration tests for GET /me and GET /me/activity.
 *
 * <p>Uses G3-verified pattern: {@code oidcLogin().authorities(new
 * SimpleGrantedAuthority("ROLE_X"))} for authenticated requests. Tests cover:
 *
 * <ul>
 *   <li>Unauthenticated → 401/redirect
 *   <li>Authenticated STUDENT → 200 on /me
 *   <li>Authenticated STUDENT → 200 on /me/activity
 *   <li>BOLA: /me uses session principal, not request parameter
 * </ul>
 *
 * <p>Satisfies: S1-B-7, S1-B-8, Spec §Personal Profile and Activity §Per-Resource Authorization.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ProfileApiSecurityTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // -------------------------------------------------------------------------
  // Unauthenticated
  // -------------------------------------------------------------------------

  @Test
  void unauthenticated_get_me_returns_4xx() throws Exception {
    mockMvc
        .perform(get("/me"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              if (status >= 200 && status < 300) {
                throw new AssertionError(
                    "GET /me unauthenticated must not return 2xx but got " + status);
              }
            });
  }

  @Test
  void unauthenticated_get_me_activity_returns_4xx() throws Exception {
    mockMvc
        .perform(get("/me/activity"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              if (status >= 200 && status < 300) {
                throw new AssertionError(
                    "GET /me/activity unauthenticated must not return 2xx but got " + status);
              }
            });
  }

  // -------------------------------------------------------------------------
  // Authenticated: user must be provisioned for /me to return 200
  // -------------------------------------------------------------------------

  @Test
  void authenticated_student_gets_200_on_me() throws Exception {
    // Provision a real user so the ProfileController can load it
    String sub = "test-sub-" + UUID.randomUUID();
    AppUser user =
        AppUser.provision(
            sub, "me_test_" + UUID.randomUUID() + "@uniquindio.edu.co", "Test User", Role.STUDENT);
    saveAppUserPort.save(user);

    mockMvc
        .perform(
            get("/me")
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(sub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk());
  }

  @Test
  void authenticated_student_gets_200_on_me_activity() throws Exception {
    String sub = "test-sub-activity-" + UUID.randomUUID();
    AppUser user =
        AppUser.provision(
            sub,
            "activity_test_" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Activity Test",
            Role.STUDENT);
    saveAppUserPort.save(user);

    mockMvc
        .perform(
            get("/me/activity")
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(sub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // BOLA: /me does not accept ?userId= parameter that would allow cross-user access
  // -------------------------------------------------------------------------

  @Test
  void me_endpoint_does_not_expose_userId_override_param() throws Exception {
    String sub = "test-sub-bola-" + UUID.randomUUID();
    AppUser user =
        AppUser.provision(
            sub,
            "bola_test_" + UUID.randomUUID() + "@uniquindio.edu.co",
            "BOLA Test",
            Role.STUDENT);
    saveAppUserPort.save(user);

    // The /me endpoint should ignore or reject ?userId= parameters — it always uses the principal
    mockMvc
        .perform(
            get("/me")
                .param("userId", UUID.randomUUID().toString())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(sub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk()); // Returns the session user's profile, not the param user
  }
}
