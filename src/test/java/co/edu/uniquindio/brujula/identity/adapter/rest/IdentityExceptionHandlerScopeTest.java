package co.edu.uniquindio.brujula.identity.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.EmailAlreadyRegisteredException;
import co.edu.uniquindio.brujula.identity.application.port.in.InitiatePasswordRecoveryUseCase;
import co.edu.uniquindio.brujula.identity.application.port.in.RegisterStudentUseCase;
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
 * Regression scope tests for {@code IdentityExceptionHandler}.
 *
 * <p>Asserts the per-module response contract for module-specific exceptions (status code). {@code
 * basePackages} scoping is defence-in-depth; {@code @Order(HIGHEST_PRECEDENCE)} is the primary
 * correctness guarantee. These tests catch contract regressions (wrong status or handler not
 * firing), not {@code basePackages} removal while handlers remain behaviorally identical.
 *
 * <p>Note: the identity module does not handle any sharedkernel shared exceptions (idempotency,
 * optimistic-lock); its 409 path uses the module-specific {@link EmailAlreadyRegisteredException}.
 * There is no {@code $.code} to assert here per spec, so status assertion is the load-bearing
 * check.
 *
 * <p>Verifies identity controller → {@code IdentityExceptionHandler} maps {@link
 * EmailAlreadyRegisteredException} to HTTP 409.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class IdentityExceptionHandlerScopeTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  @MockitoBean private RegisterStudentUseCase registerStudentUseCase;
  @MockitoBean private ResendEmailVerificationUseCase resendEmailVerificationUseCase;
  @MockitoBean private InitiatePasswordRecoveryUseCase initiatePasswordRecoveryUseCase;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  /**
   * Identity controller throwing {@link EmailAlreadyRegisteredException} MUST return HTTP 409 from
   * {@code IdentityExceptionHandler} (scoped to the identity REST package).
   *
   * <p>This is a status-level contract test. {@link EmailAlreadyRegisteredException} is
   * module-specific (not sharedkernel), so there is no {@code $.code} to assert per spec. If a
   * handler regression changes the 409 mapping, this test catches it.
   */
  @Test
  void identity_controller_email_already_registered_returns_409() throws Exception {
    when(registerStudentUseCase.register(any()))
        .thenThrow(new EmailAlreadyRegisteredException("scope-test@uniquindio.edu.co"));

    mockMvc
        .perform(
            post("/auth/registration")
                .with(csrf())
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "scope-test@uniquindio.edu.co",
                      "password": "Passw0rd!",
                      "fullName": "Scope Test",
                      "acceptedTerms": true,
                      "acceptedDataProcessing": true
                    }
                    """))
        .andExpect(status().isConflict());
  }
}
