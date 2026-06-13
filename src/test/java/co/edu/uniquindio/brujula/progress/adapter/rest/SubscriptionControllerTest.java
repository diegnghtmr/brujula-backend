package co.edu.uniquindio.brujula.progress.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.DeleteSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.ListMySubscriptionsQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.UpdateSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.domain.HistoryImpactNotAcknowledgedException;
import co.edu.uniquindio.brujula.progress.domain.LastActiveSubscriptionException;
import co.edu.uniquindio.brujula.progress.domain.MaxSubscriptionsExceededException;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * REST integration tests for the Subscription endpoints.
 *
 * <p>Uses {@code @SpringBootTest} + real security chain (same pattern as {@code
 * ProfileApiSecurityTest}). Application-layer ports are mocked via {@code @MockitoBean} so no
 * database writes are needed for business-logic scenarios.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>Unauthenticated → 401/redirect.
 *   <li>STAFF role on /me/** → 403 (students-only resource).
 *   <li>STUDENT POST create → 201.
 *   <li>POST without Idempotency-Key → 400.
 *   <li>Cap=2 exceeded → 409.
 *   <li>Optimistic-lock conflict → 409.
 *   <li>GET subscription belonging to another user → 404.
 *   <li>Delete last active subscription → 409.
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SubscriptionControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;

  @MockitoBean private CreateSubscriptionUseCase createSubscriptionUseCase;
  @MockitoBean private ListMySubscriptionsQuery listMySubscriptionsQuery;
  @MockitoBean private GetSubscriptionQuery getSubscriptionQuery;
  @MockitoBean private UpdateSubscriptionUseCase updateSubscriptionUseCase;
  @MockitoBean private DeleteSubscriptionUseCase deleteSubscriptionUseCase;

  private MockMvc mockMvc;
  private String studentSub;
  private UUID studentUserId;
  private String staffSub;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Provision a STUDENT user
    studentSub = "sub-student-" + UUID.randomUUID();
    AppUser student =
        AppUser.provision(
            studentSub,
            "student-" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Test Student",
            Role.STUDENT);
    saveAppUserPort.save(student);
    studentUserId = student.id();

    // Provision a STAFF user
    staffSub = "sub-staff-" + UUID.randomUUID();
    AppUser staff =
        AppUser.provision(
            staffSub,
            "staff-" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Test Staff",
            Role.STAFF);
    saveAppUserPort.save(staff);
  }

  // -------------------------------------------------------------------------
  // Unauthenticated requests are rejected
  // -------------------------------------------------------------------------

  @Test
  void unauthenticated_get_subscriptions_returns_4xx() throws Exception {
    mockMvc
        .perform(get("/me/subscriptions"))
        .andExpect(
            result -> {
              int s = result.getResponse().getStatus();
              if (s >= 200 && s < 300) {
                throw new AssertionError("Expected 4xx but got " + s);
              }
            });
  }

  @Test
  void unauthenticated_post_subscription_returns_4xx() throws Exception {
    mockMvc
        .perform(
            post("/me/subscriptions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestBody()))
        .andExpect(
            result -> {
              int s = result.getResponse().getStatus();
              if (s >= 200 && s < 300) {
                throw new AssertionError("Expected 4xx but got " + s);
              }
            });
  }

  // -------------------------------------------------------------------------
  // STAFF role cannot access /me/subscriptions (student resource)
  // -------------------------------------------------------------------------

  @Test
  void staff_get_subscriptions_returns_403() throws Exception {
    mockMvc
        .perform(
            get("/me/subscriptions")
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(staffSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void staff_post_subscription_returns_403() throws Exception {
    mockMvc
        .perform(
            post("/me/subscriptions")
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(staffSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STAFF")))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestBody()))
        .andExpect(status().isForbidden());
  }

  // -------------------------------------------------------------------------
  // POST without Idempotency-Key header → 400
  // -------------------------------------------------------------------------

  @Test
  void post_subscription_without_idempotency_key_returns_400() throws Exception {
    mockMvc
        .perform(
            post("/me/subscriptions")
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                // No Idempotency-Key header
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestBody()))
        .andExpect(status().isBadRequest());
  }

  // -------------------------------------------------------------------------
  // STUDENT creates subscription → 201
  // -------------------------------------------------------------------------

  @Test
  void student_creates_subscription_returns_201() throws Exception {
    UUID subId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();

    SubscriptionView view =
        new SubscriptionView(
            subId,
            programId,
            "Engineering",
            planId,
            "Plan 2020",
            "DIURNA",
            "Diurna",
            0L,
            Instant.now());

    when(createSubscriptionUseCase.createSubscription(any())).thenReturn(view);

    mockMvc
        .perform(
            post("/me/subscriptions")
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"programId":"%s","planId":"%s","referenceJourneyCode":"DIURNA"}
                    """
                        .formatted(programId, planId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(subId.toString()));
  }

  // -------------------------------------------------------------------------
  // Cap=2 exceeded → 409
  // -------------------------------------------------------------------------

  @Test
  void post_subscription_cap_exceeded_returns_409() throws Exception {
    when(createSubscriptionUseCase.createSubscription(any()))
        .thenThrow(new MaxSubscriptionsExceededException(studentUserId, 2));

    mockMvc
        .perform(
            post("/me/subscriptions")
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestBody()))
        .andExpect(status().isConflict());
  }

  // -------------------------------------------------------------------------
  // Idempotency-key conflict → 409
  // -------------------------------------------------------------------------

  @Test
  void post_subscription_idempotency_conflict_returns_409() throws Exception {
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
                .content(createRequestBody()))
        .andExpect(status().isConflict());
  }

  // -------------------------------------------------------------------------
  // GET subscription belonging to another user → 404
  // -------------------------------------------------------------------------

  @Test
  void get_subscription_bola_returns_404() throws Exception {
    UUID subId = UUID.randomUUID();
    when(getSubscriptionQuery.getSubscription(eq(subId), any()))
        .thenThrow(new SubscriptionNotFoundException(subId));

    mockMvc
        .perform(
            get("/me/subscriptions/" + subId)
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isNotFound());
  }

  // -------------------------------------------------------------------------
  // Delete last active subscription → 409
  // -------------------------------------------------------------------------

  @Test
  void delete_last_subscription_returns_409() throws Exception {
    UUID subId = UUID.randomUUID();
    org.mockito.Mockito.doThrow(new LastActiveSubscriptionException(studentUserId))
        .when(deleteSubscriptionUseCase)
        .deleteSubscription(eq(subId), any());

    mockMvc
        .perform(
            delete("/me/subscriptions/" + subId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token"))
        .andExpect(status().isConflict());
  }

  // -------------------------------------------------------------------------
  // GET /me/subscriptions list — returns 200 for authenticated student
  // -------------------------------------------------------------------------

  @Test
  void list_subscriptions_returns_200_for_student() throws Exception {
    when(listMySubscriptionsQuery.listMySubscriptions(any())).thenReturn(List.of());

    mockMvc
        .perform(
            get("/me/subscriptions")
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // PATCH /me/subscriptions/{subscriptionId} REST tests
  // -------------------------------------------------------------------------

  /** Successful PATCH returns 200 with incremented version in response. */
  @Test
  void patch_subscription_journey_change_returns_200_with_incremented_version() throws Exception {
    UUID subId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();

    // View returned by service carries version=1 (incremented)
    SubscriptionView updatedView =
        new SubscriptionView(
            subId,
            programId,
            "Engineering",
            planId,
            "Plan 2020",
            "NOCTURNA",
            "Nocturna",
            1L,
            java.time.Instant.now());

    when(updateSubscriptionUseCase.updateSubscription(any())).thenReturn(updatedView);

    mockMvc
        .perform(
            patch("/me/subscriptions/" + subId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"referenceJourneyCode":"NOCTURNA","version":0}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1L));
  }

  /** PATCH without acknowledgeHistoryImpact when plan change has attempts → 422. */
  @Test
  void patch_subscription_plan_change_without_ack_returns_422() throws Exception {
    UUID subId = UUID.randomUUID();
    when(updateSubscriptionUseCase.updateSubscription(any()))
        .thenThrow(new HistoryImpactNotAcknowledgedException(SubscriptionId.of(subId)));

    mockMvc
        .perform(
            patch("/me/subscriptions/" + subId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"planId":"%s","version":0}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isUnprocessableEntity());
  }

  /** PATCH with stale version → 409. */
  @Test
  void patch_subscription_stale_version_returns_409() throws Exception {
    UUID subId = UUID.randomUUID();
    when(updateSubscriptionUseCase.updateSubscription(any()))
        .thenThrow(new OptimisticLockingFailureException("Version conflict"));

    mockMvc
        .perform(
            patch("/me/subscriptions/" + subId)
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(studentSub))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .header("X-XSRF-TOKEN", "csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"referenceJourneyCode":"DIURNA","version":0}
                    """))
        .andExpect(status().isConflict());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private String createRequestBody() {
    return """
        {"programId":"%s","planId":"%s","referenceJourneyCode":"DIURNA"}
        """
        .formatted(UUID.randomUUID(), UUID.randomUUID());
  }
}
