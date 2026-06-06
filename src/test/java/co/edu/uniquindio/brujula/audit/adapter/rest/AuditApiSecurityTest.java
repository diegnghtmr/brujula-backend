package co.edu.uniquindio.brujula.audit.adapter.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
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
 * Security tests for audit REST endpoints.
 *
 * <p>Uses G3-verified pattern: {@code oidcLogin().authorities(new
 * SimpleGrantedAuthority("ROLE_X"))}.
 *
 * <p>Covers: unauthenticated→4xx, wrong role→403, correct role→200 for admin and staff endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AuditApiSecurityTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // -------------------------------------------------------------------------
  // GET /admin/audit-events
  // -------------------------------------------------------------------------

  @Test
  void unauthenticated_admin_audit_events_returns_4xx() throws Exception {
    mockMvc
        .perform(get("/admin/audit-events"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              if (status >= 200 && status < 300) {
                throw new AssertionError(
                    "GET /admin/audit-events unauthenticated must not return 2xx but got "
                        + status);
              }
            });
  }

  @Test
  void student_gets_403_on_admin_audit_events() throws Exception {
    mockMvc
        .perform(
            get("/admin/audit-events")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void staff_gets_403_on_admin_audit_events() throws Exception {
    mockMvc
        .perform(
            get("/admin/audit-events")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void admin_gets_200_on_admin_audit_events() throws Exception {
    mockMvc
        .perform(
            get("/admin/audit-events")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // GET /staff/audit-events
  // -------------------------------------------------------------------------

  @Test
  void unauthenticated_staff_audit_events_returns_4xx() throws Exception {
    mockMvc
        .perform(get("/staff/audit-events").param("programId", UUID.randomUUID().toString()))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              if (status >= 200 && status < 300) {
                throw new AssertionError(
                    "GET /staff/audit-events unauthenticated must not return 2xx but got "
                        + status);
              }
            });
  }

  @Test
  void student_gets_403_on_staff_audit_events() throws Exception {
    mockMvc
        .perform(
            get("/staff/audit-events")
                .param("programId", UUID.randomUUID().toString())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void staff_gets_200_on_staff_audit_events() throws Exception {
    mockMvc
        .perform(
            get("/staff/audit-events")
                .param("programId", UUID.randomUUID().toString())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk());
  }

  @Test
  void admin_gets_200_on_staff_audit_events() throws Exception {
    mockMvc
        .perform(
            get("/staff/audit-events")
                .param("programId", UUID.randomUUID().toString())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
  }
}
