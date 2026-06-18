package co.edu.uniquindio.brujula.administration.adapter.rest;

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
import co.edu.uniquindio.brujula.administration.application.port.in.CreateStaffUseCase;
import co.edu.uniquindio.brujula.administration.application.port.in.ListStaffQuery;
import co.edu.uniquindio.brujula.administration.application.port.in.ManageAllowedEmailDomainsUseCase;
import co.edu.uniquindio.brujula.administration.application.port.in.UpdateStaffUseCase;
import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.CreatePeriodUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdatePeriodUseCase;
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
 * Regression scope tests for {@code AdministrationExceptionHandler}.
 *
 * <p>Asserts the per-module response contract for shared exceptions (status + {@code $.code}).
 * {@code basePackages} scoping is defence-in-depth; {@code @Order(HIGHEST_PRECEDENCE)} is the
 * primary correctness guarantee. These tests catch contract regressions (missing {@code $.code},
 * wrong status), not {@code basePackages} removal while handlers remain behaviorally identical.
 *
 * <p>Verifies: administration controller → {@code AdministrationExceptionHandler} maps {@link
 * IdempotencyKeyConflictException} to HTTP 409 with {@code $.code = "IDEMPOTENCY_KEY_CONFLICT"}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AdministrationExceptionHandlerScopeTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  @MockitoBean private CreateStaffUseCase createStaffUseCase;
  @MockitoBean private ListStaffQuery listStaffQuery;
  @MockitoBean private UpdateStaffUseCase updateStaffUseCase;
  @MockitoBean private ManageAllowedEmailDomainsUseCase manageAllowedEmailDomainsUseCase;
  @MockitoBean private GetCurrentUserQuery getCurrentUserQuery;
  @MockitoBean private CreatePeriodUseCase createPeriodUseCase;
  @MockitoBean private UpdatePeriodUseCase updatePeriodUseCase;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // AdministrationController.resolveActorId() calls getCurrentUserBySubject before delegating
    // to the use case. Stub it so the request reaches createStaff (where our mock throws).
    UUID actorId = UUID.randomUUID();
    when(getCurrentUserQuery.getCurrentUserBySubject(any(String.class)))
        .thenReturn(
            new UserProfile(
                actorId,
                "admin-scope-sub",
                "admin-scope@uniquindio.edu.co",
                "Scope Admin",
                "ADMIN",
                "ACTIVE",
                true));
  }

  /**
   * Administration controller throwing {@link IdempotencyKeyConflictException} MUST return HTTP 409
   * with {@code $.code = "IDEMPOTENCY_KEY_CONFLICT"} from {@code AdministrationExceptionHandler}.
   *
   * <p>Asserting {@code $.code} makes this test load-bearing: it will fail if any handler in the
   * chain omits the {@code code} property, regardless of {@code basePackages} state.
   */
  @Test
  void administration_controller_idempotency_conflict_returns_409_with_code() throws Exception {
    UUID idemKey = UUID.randomUUID();
    when(createStaffUseCase.createStaff(any()))
        .thenThrow(new IdempotencyKeyConflictException(idemKey, "CREATE_STAFF"));

    mockMvc
        .perform(
            post("/admin/staff")
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject("admin-scope-sub"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .header("Idempotency-Key", idemKey.toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"newstaff@uniquindio.edu.co","fullName":"New Staff"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code", is("IDEMPOTENCY_KEY_CONFLICT")));
  }
}
