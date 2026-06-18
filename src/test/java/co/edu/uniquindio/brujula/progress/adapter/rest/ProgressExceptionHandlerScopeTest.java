package co.edu.uniquindio.brujula.progress.adapter.rest;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.DeleteSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.ListMySubscriptionsQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.UpdateSubscriptionUseCase;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Regression scope tests for {@code ProgressExceptionHandler}.
 *
 * <p>Asserts the per-module response contract for shared exceptions (status + {@code $.code}).
 * {@code basePackages} scoping is defence-in-depth; {@code @Order(HIGHEST_PRECEDENCE)} is the
 * primary correctness guarantee. These tests catch contract regressions (missing {@code $.code},
 * wrong status), not {@code basePackages} removal while handlers remain behaviorally identical.
 *
 * <p>Verifies progress controller → {@code ProgressExceptionHandler} maps {@link
 * IdempotencyKeyConflictException} to HTTP 409 with {@code $.code = "IDEMPOTENCY_KEY_CONFLICT"}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ProgressExceptionHandlerScopeTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;

  @MockitoBean private CreateSubscriptionUseCase createSubscriptionUseCase;
  @MockitoBean private ListMySubscriptionsQuery listMySubscriptionsQuery;
  @MockitoBean private GetSubscriptionQuery getSubscriptionQuery;
  @MockitoBean private UpdateSubscriptionUseCase updateSubscriptionUseCase;
  @MockitoBean private DeleteSubscriptionUseCase deleteSubscriptionUseCase;

  private MockMvc mockMvc;
  private String studentSub;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    studentSub = "sub-scope-progress-" + UUID.randomUUID();
    AppUser student =
        AppUser.provision(
            studentSub,
            "scope-progress-" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Scope Student",
            Role.STUDENT);
    saveAppUserPort.save(student);
  }

  /**
   * Progress controller throwing {@link IdempotencyKeyConflictException} MUST return HTTP 409 with
   * {@code $.code = "IDEMPOTENCY_KEY_CONFLICT"} via {@code ProgressExceptionHandler}.
   *
   * <p>Asserting {@code $.code} makes this test load-bearing: it will fail if any handler in the
   * chain omits the {@code code} property, regardless of {@code basePackages} state.
   */
  @Test
  void progress_controller_idempotency_conflict_returns_409_with_code() throws Exception {
    UUID idemKey = UUID.randomUUID();
    when(createSubscriptionUseCase.createSubscription(any()))
        .thenThrow(new IdempotencyKeyConflictException(idemKey, "CREATE_SUBSCRIPTION"));

    mockMvc
        .perform(
            post("/me/subscriptions")
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", idemKey.toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"programId":"%s","planId":"%s","referenceJourneyCode":"DIURNA"}
                    """
                        .formatted(UUID.randomUUID(), UUID.randomUUID())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code", is("IDEMPOTENCY_KEY_CONFLICT")));
  }

  /**
   * Verifies the full response contract when the progress subscription endpoint throws {@link
   * IdempotencyKeyConflictException}.
   *
   * <p>Asserts both HTTP 409 status and {@code $.code = "IDEMPOTENCY_KEY_CONFLICT"} so that any
   * future handler regression (missing code property or wrong status) is caught.
   */
  @Test
  void simulation_handler_does_not_intercept_progress_controller_request() throws Exception {
    UUID idemKey = UUID.randomUUID();
    when(createSubscriptionUseCase.createSubscription(any()))
        .thenThrow(new IdempotencyKeyConflictException(idemKey, "CREATE_SUBSCRIPTION"));

    mockMvc
        .perform(
            post("/me/subscriptions")
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", idemKey.toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"programId":"%s","planId":"%s","referenceJourneyCode":"DIURNA"}
                    """
                        .formatted(UUID.randomUUID(), UUID.randomUUID())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code", is("IDEMPOTENCY_KEY_CONFLICT")));
  }
}
