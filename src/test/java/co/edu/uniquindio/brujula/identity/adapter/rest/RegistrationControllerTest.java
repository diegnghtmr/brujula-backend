package co.edu.uniquindio.brujula.identity.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserId;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for POST /auth/registration (S2-B-5, RegistrationController).
 *
 * <p>Tests are RED before the controller routes to {@link RegisterStudentUseCase}. They turn GREEN
 * after the AuthController.registerStudent shim is replaced with the real implementation.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>S2-B-5-1: POST without CSRF token → 403 (SPA CSRF enforced on public POST endpoints).
 *   <li>S2-B-5-2: Non-institutional email (blocked domain) → 422 with ProblemDetail.
 *   <li>S2-B-5-3: Valid institutional email + consent → 202 Accepted with RegistrationAccepted body.
 *   <li>S2-B-5-4: POST /auth/registration is reachable without authentication (permitAll).
 *   <li>S2-B-5-5: Duplicate email → 409 Conflict.
 * </ul>
 *
 * <p>CSRF: {@code /auth/registration} is in the {@code permitAll} matcher but is NOT excluded from
 * CSRF (no {@code csrf().ignoringRequestMatchers(...)}). SPA clients read the XSRF-TOKEN cookie from
 * a prior request and send it as X-XSRF-TOKEN. Tests supply it via {@code csrf()} post-processor.
 *
 * <p>Satisfies: S2-B-5, OpenAPI contract POST /auth/registration → 202 RegistrationAccepted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RegistrationControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  /**
   * Mock the Keycloak outbound port so integration tests do not require a running Keycloak server.
   * The real adapter ({@code KeycloakAdminApiAdapter}) connects to {@code localhost:9090} which is
   * unavailable in the test environment.
   */
  @MockitoBean private KeycloakUserProvisioningPort keycloakUserProvisioningPort;

  private MockMvc mockMvc;

  private static final String VALID_FULL_NAME = "Test Student";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    // Stub createUser to return a synthetic Keycloak user ID.
    when(keycloakUserProvisioningPort.createUser(any()))
        .thenReturn(new KeycloakUserId(UUID.randomUUID().toString()));
    // Stub triggerEmailVerification to be a no-op (email service not needed in tests).
    doNothing().when(keycloakUserProvisioningPort).triggerEmailVerification(any(), anyString());
  }

  // ---------------------------------------------------------------------------
  // S2-B-5-1: CSRF enforcement — public POST still requires CSRF token
  // ---------------------------------------------------------------------------

  @Test
  void post_registration_without_csrf_returns_403() throws Exception {
    // No csrf() post-processor → CSRF filter rejects the request.
    mockMvc
        .perform(
            post("/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "00000000-0000-0000-0000-000000000001")
                .content(validRegistrationBody("no-csrf-" + System.nanoTime() + "@uniquindio.edu.co")))
        .andExpect(status().isForbidden());
  }

  // ---------------------------------------------------------------------------
  // S2-B-5-4: Endpoint is permitAll — reachable unauthenticated (with CSRF)
  // A 422 (blocked domain) proves the request reached the controller, not Spring Security.
  // ---------------------------------------------------------------------------

  @Test
  void post_registration_is_permit_all_no_authentication_required() throws Exception {
    // Non-institutional domain → 422, NOT 401/403. This proves permitAll works.
    mockMvc
        .perform(
            post("/auth/registration")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "00000000-0000-0000-0000-000000000002")
                .content(validRegistrationBody("attacker@gmail.com")))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              assertThat(status)
                  .as("Non-institutional email should return 422, not an auth error")
                  .isNotIn(401, 403);
            });
  }

  // ---------------------------------------------------------------------------
  // S2-B-5-2: Non-institutional email → 422 Unprocessable Entity
  // ---------------------------------------------------------------------------

  @Test
  void post_registration_with_non_institutional_email_returns_422() throws Exception {
    mockMvc
        .perform(
            post("/auth/registration")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "00000000-0000-0000-0000-000000000003")
                .content(validRegistrationBody("student@gmail.com")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422));
  }

  // ---------------------------------------------------------------------------
  // S2-B-5-3: Valid institutional email + consent → 202 Accepted
  // ---------------------------------------------------------------------------

  @Test
  void post_registration_with_valid_body_returns_202_accepted() throws Exception {
    // Each test run uses a unique email to avoid DB constraint conflicts.
    String uniqueEmail = "student-reg-" + System.nanoTime() + "@uniquindio.edu.co";
    mockMvc
        .perform(
            post("/auth/registration")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "00000000-0000-0000-0000-000000000004")
                .content(validRegistrationBody(uniqueEmail)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.userId").isNotEmpty())
        .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));
  }

  // ---------------------------------------------------------------------------
  // S2-B-5-5: Duplicate email → 409 Conflict
  // ---------------------------------------------------------------------------

  @Test
  void post_registration_with_duplicate_email_returns_409() throws Exception {
    String duplicateEmail = "dup-reg-" + System.nanoTime() + "@uniquindio.edu.co";

    // First registration — should succeed (202)
    mockMvc
        .perform(
            post("/auth/registration")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "00000000-0000-0000-0000-000000000005")
                .content(validRegistrationBody(duplicateEmail)))
        .andExpect(status().isAccepted());

    // Second registration with the same email — should conflict (409)
    mockMvc
        .perform(
            post("/auth/registration")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "00000000-0000-0000-0000-000000000006")
                .content(validRegistrationBody(duplicateEmail)))
        .andExpect(status().isConflict());
  }

  // ---------------------------------------------------------------------------
  // Helper
  // ---------------------------------------------------------------------------

  private String validRegistrationBody(String email) {
    // W-001/OPTION A: no password field — Keycloak sets password via UPDATE_PASSWORD action-email
    return """
        {
          "fullName": "%s",
          "email": "%s",
          "acceptedTerms": true,
          "acceptedDataProcessing": true
        }
        """.formatted(VALID_FULL_NAME, email);
  }
}
