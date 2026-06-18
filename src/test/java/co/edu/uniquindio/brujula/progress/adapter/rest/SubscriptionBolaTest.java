package co.edu.uniquindio.brujula.progress.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
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
 * BOLA HTTP-layer ownership tests for subscription resources.
 *
 * <p>Uses {@code @SpringBootTest} + Testcontainers PostgreSQL (via {@link
 * AbstractPostgresIntegrationTest}) + {@code spring-security-test} {@code oidcLogin()} with TWO
 * distinct OIDC subjects.
 *
 * <p>Principal-B owns subscription SUB-1 (seeded via real persistence port). Principal-A sends GET
 * on SUB-1's ID and expects HTTP 404 — the deliberate enumeration-safe response (existing domain
 * contract: ownership violation is surfaced as SubscriptionNotFoundException → 404, to avoid
 * revealing resource existence to unauthorised callers).
 *
 * <p>The test asserts the CURRENT domain behavior and must NOT change it to 403. Per design:
 * simulation uses 403, subscriptions use 404 (enumeration-safety deliberate design).
 *
 * <p>ANTI-FAKE-GREEN: NO {@code @MockitoBean} on GetSubscriptionQuery. The real service + real
 * persistence exercises the ownership guard end-to-end through the HTTP adapter.
 *
 * <p>Scenarios covered:
 *
 * <ul>
 *   <li>Cross-owner GET → 404 (enumeration-safe); no subscription data from B leaked.
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SubscriptionBolaTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;
  @Autowired private SaveSubscriptionPort saveSubscriptionPort;

  private MockMvc mockMvc;

  /** Principal-A: the attacker — attempts cross-owner access. */
  private String principalASubject;

  /** Principal-B: the legitimate owner of the subscription seeded in @BeforeEach. */
  private String principalBSubject;

  private UUID principalBUserId;

  /** The subscription owned by principal-B. */
  private UUID subOwnedByBId;

  /**
   * Domain identifiers seeded on principal-B's subscription — must not appear in error responses.
   */
  private UUID subProgramId;

  private UUID subPlanId;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    // Principal-A: attacker (distinct sub + userId)
    principalASubject = "bola-sub-prog-attacker-" + UUID.randomUUID();
    AppUser principalA =
        AppUser.provision(
            principalASubject,
            "prog-attacker-" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Principal A Attacker Progress",
            Role.STUDENT);
    saveAppUserPort.save(principalA);

    // Principal-B: legitimate owner
    principalBSubject = "bola-sub-prog-owner-" + UUID.randomUUID();
    AppUser principalB =
        AppUser.provision(
            principalBSubject,
            "prog-owner-" + UUID.randomUUID() + "@uniquindio.edu.co",
            "Principal B Owner Progress",
            Role.STUDENT);
    saveAppUserPort.save(principalB);
    principalBUserId = principalB.id();

    // Seed: create a subscription owned by principal-B using the real persistence port.
    // Real ownership check in GetSubscriptionService fires on the HTTP request from principal-A.
    subProgramId = UUID.randomUUID();
    subPlanId = UUID.randomUUID();
    Subscription subOwnedByB =
        Subscription.create(
            SubscriptionId.generate(),
            principalBUserId,
            subProgramId, // programId — arbitrary, not validated by ownership check
            subPlanId, // planId — arbitrary
            "DIURNA");
    Subscription saved = saveSubscriptionPort.save(subOwnedByB);
    subOwnedByBId = saved.getId().value();
  }

  // -------------------------------------------------------------------------
  // Cross-owner GET → 404 (enumeration-safe)
  // -------------------------------------------------------------------------

  /**
   * Principal-A sends GET on principal-B's subscription.
   *
   * <p>Real GetSubscriptionService checks userId equality. When the IDs differ it throws {@link
   * co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException}, which {@code
   * ProgressExceptionHandler} maps to HTTP 404.
   *
   * <p>This is the deliberate BOLA-safe response: 404 does not confirm resource existence,
   * preventing enumeration attacks.
   *
   * <p>No subscription data from principal-B must appear in the response: the 404 body contains
   * only an error description, never subscription fields (id, programId, planId, etc.).
   */
  @Test
  void cross_owner_get_subscription_returns_404_no_data_leaked() throws Exception {
    mockMvc
        .perform(
            get("/me/subscriptions/{id}", subOwnedByBId)
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(principalASubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isNotFound())
        // Assert no subscription data from principal-B leaks into the response.
        // A 404 ProblemDetail body must not contain a programId or userId field.
        .andExpect(
            result -> {
              String body = result.getResponse().getContentAsString();
              // The 404 error body must not expose any of principal-B's subscription domain data.
              // A plain ProblemDetail (status/type/title/detail) is the only acceptable shape.
              assertThat(body)
                  .as("Response must not leak principal-B's userId")
                  .doesNotContain(principalBUserId.toString());
              assertThat(body)
                  .as("Response must not leak principal-B's programId")
                  .doesNotContain(subProgramId.toString());
              assertThat(body)
                  .as("Response must not leak principal-B's planId")
                  .doesNotContain(subPlanId.toString());
            });
  }

  // -------------------------------------------------------------------------
  // Bonus: owner can still access their own subscription (sanity check)
  // -------------------------------------------------------------------------

  /**
   * Sanity: principal-B (the owner) can GET their own subscription → 200 OK. Confirms the seeding
   * worked and the real ownership check passes for the legitimate owner.
   */
  @Test
  void owner_can_get_own_subscription_returns_200() throws Exception {
    mockMvc
        .perform(
            get("/me/subscriptions/{id}", subOwnedByBId)
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject(principalBSubject))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk());
  }
}
