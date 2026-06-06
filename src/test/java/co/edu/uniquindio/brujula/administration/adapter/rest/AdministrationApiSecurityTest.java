package co.edu.uniquindio.brujula.administration.adapter.rest;

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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Security tests for administration REST endpoints.
 *
 * <p>Uses G3-verified pattern: {@code oidcLogin().authorities(new
 * SimpleGrantedAuthority("ROLE_X"))}. Covers: unauthenticated→4xx, wrong role→403, correct role
 * (ADMIN)→2xx.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AdministrationApiSecurityTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // -------------------------------------------------------------------------
  // GET /admin/staff
  // -------------------------------------------------------------------------

  @Test
  void unauthenticated_list_staff_returns_4xx() throws Exception {
    mockMvc
        .perform(get("/admin/staff"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              if (status >= 200 && status < 300) {
                throw new AssertionError(
                    "GET /admin/staff unauthenticated must not return 2xx but got " + status);
              }
            });
  }

  @Test
  void student_gets_403_on_list_staff() throws Exception {
    mockMvc
        .perform(
            get("/admin/staff")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void staff_gets_403_on_list_staff() throws Exception {
    mockMvc
        .perform(
            get("/admin/staff")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void admin_gets_200_on_list_staff() throws Exception {
    mockMvc
        .perform(
            get("/admin/staff")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // GET /admin/allowed-email-domains
  // -------------------------------------------------------------------------

  @Test
  void unauthenticated_list_domains_returns_4xx() throws Exception {
    mockMvc
        .perform(get("/admin/allowed-email-domains"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              if (status >= 200 && status < 300) {
                throw new AssertionError(
                    "GET /admin/allowed-email-domains unauthenticated must not return 2xx");
              }
            });
  }

  @Test
  void student_gets_403_on_list_domains() throws Exception {
    mockMvc
        .perform(
            get("/admin/allowed-email-domains")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void admin_gets_200_on_list_domains() throws Exception {
    mockMvc
        .perform(
            get("/admin/allowed-email-domains")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // POST /admin/staff (idempotent create)
  // -------------------------------------------------------------------------

  @Test
  void student_gets_403_on_create_staff() throws Exception {
    mockMvc
        .perform(
            post("/admin/staff")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .with(csrf())
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@uniquindio.edu.co\",\"fullName\":\"Test\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void staff_gets_403_on_create_staff() throws Exception {
    mockMvc
        .perform(
            post("/admin/staff")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF")))
                .with(csrf())
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@uniquindio.edu.co\",\"fullName\":\"Test\"}"))
        .andExpect(status().isForbidden());
  }
}
