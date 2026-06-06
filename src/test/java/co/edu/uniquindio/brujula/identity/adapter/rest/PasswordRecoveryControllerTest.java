package co.edu.uniquindio.brujula.identity.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.in.InitiatePasswordRecoveryUseCase;
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
 * RED → GREEN integration tests for POST /auth/password-recovery (S2-D).
 *
 * <p>STRICT TDD: tests written RED before the controller implementation exists.
 *
 * <p>Key scenarios:
 * <ul>
 *   <li>Known email → 202 Accepted</li>
 *   <li>Unknown email → SAME 202 Accepted (no user enumeration)</li>
 *   <li>Endpoint is permitAll: unauthenticated request succeeds</li>
 * </ul>
 *
 * <p>Satisfies: S2-D, Design §1.3 permitAll for /auth/password-recovery/**, RNF-SEG-006.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class PasswordRecoveryControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  /**
   * Mock the use case so the controller test is isolated from business logic.
   * No live Keycloak required.
   */
  @MockitoBean private InitiatePasswordRecoveryUseCase initiatePasswordRecoveryUseCase;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    // Default stub: recovery completes normally for any email (no enumeration in service)
    doNothing().when(initiatePasswordRecoveryUseCase).initiateRecovery(anyString());
  }

  // -------------------------------------------------------------------------
  // S2-D-1: Known email → 202 Accepted
  // -------------------------------------------------------------------------

  @Test
  void initiateRecovery_withKnownEmail_returns202Accepted() throws Exception {
    mockMvc
        .perform(
            post("/auth/password-recovery")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"student@uniquindio.edu.co\"}"))
        .andExpect(status().isAccepted());
  }

  // -------------------------------------------------------------------------
  // S2-D-2: Unknown email → SAME 202 Accepted (no user enumeration)
  // -------------------------------------------------------------------------

  @Test
  void initiateRecovery_withUnknownEmail_returns202Accepted_sameAsKnown() throws Exception {
    // Service is a no-op for unknown, but HTTP response is IDENTICAL — no user enumeration.
    mockMvc
        .perform(
            post("/auth/password-recovery")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"nobody@uniquindio.edu.co\"}"))
        .andExpect(status().isAccepted());
  }

  // -------------------------------------------------------------------------
  // S2-D-3: permitAll — unauthenticated request reaches the endpoint
  // -------------------------------------------------------------------------

  @Test
  void initiateRecovery_isPermitAll_unauthenticated_returns202_not401or403() throws Exception {
    // No oidcLogin() — unauthenticated request; should NOT get 401/403 (permitAll)
    mockMvc
        .perform(
            post("/auth/password-recovery")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"student@uniquindio.edu.co\"}"))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              assertThat(status)
                  .as("password-recovery must be permitAll (not 401/403)")
                  .isNotIn(401, 403);
            });
  }
}
