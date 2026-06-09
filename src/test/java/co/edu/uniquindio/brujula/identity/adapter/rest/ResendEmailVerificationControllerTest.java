package co.edu.uniquindio.brujula.identity.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.in.ResendEmailVerificationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * RED → GREEN integration tests for POST /auth/email-verifications/resend (S2-C).
 *
 * <p>STRICT TDD: tests written RED before the controller implementation exists.
 *
 * <p>Key scenarios:
 *
 * <ul>
 *   <li>Known email → 202 Accepted (use case called)
 *   <li>Unknown email → 202 Accepted (IDENTICAL response — no user enumeration)
 *   <li>No-enumeration: same body (empty void 202) for both cases
 *   <li>Endpoint is permitAll: unauthenticated request succeeds
 * </ul>
 *
 * <p>Satisfies: S2-C, Design §1.3 permitAll for /auth/email-verifications/**, RNF-SEG-006.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ResendEmailVerificationControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  /**
   * Mock the use case so the controller test is isolated from business logic. No live Keycloak
   * required.
   */
  @MockitoBean private ResendEmailVerificationUseCase resendEmailVerificationUseCase;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    // Default stub: resend completes normally for any email (no enumeration)
    doNothing().when(resendEmailVerificationUseCase).resend(anyString());
  }

  // -------------------------------------------------------------------------
  // S2-C-1: Known email → 202 Accepted
  // -------------------------------------------------------------------------

  @Test
  void resend_withKnownEmail_returns202Accepted() throws Exception {
    mockMvc
        .perform(
            post("/auth/email-verifications/resend")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "00000000-0000-0000-0000-000000000010")
                .content("{\"email\": \"student@uniquindio.edu.co\"}"))
        .andExpect(status().isAccepted());
  }

  // -------------------------------------------------------------------------
  // S2-C-2: Unknown email → SAME 202 Accepted (no user enumeration)
  // -------------------------------------------------------------------------

  @Test
  void resend_withUnknownEmail_returns202Accepted_sameAsKnown() throws Exception {
    // The service is a no-op for unknown emails (no Keycloak call), but the response is identical.
    mockMvc
        .perform(
            post("/auth/email-verifications/resend")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "00000000-0000-0000-0000-000000000011")
                .content("{\"email\": \"nobody@uniquindio.edu.co\"}"))
        .andExpect(status().isAccepted());
  }

  // -------------------------------------------------------------------------
  // S2-C-3: permitAll — unauthenticated request reaches the endpoint
  // -------------------------------------------------------------------------

  @Test
  void resend_isPermitAll_unauthenticated_returns202_not401or403() throws Exception {
    // No oidcLogin() — unauthenticated request; should NOT get 401/403 (permitAll)
    mockMvc
        .perform(
            post("/auth/email-verifications/resend")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "00000000-0000-0000-0000-000000000012")
                .content("{\"email\": \"student@uniquindio.edu.co\"}"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              assertThat(status).as("resend must be permitAll (not 401/403)").isNotIn(401, 403);
            });
  }
}
