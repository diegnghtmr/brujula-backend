package co.edu.uniquindio.brujula.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.identity.application.dto.ProvisionCommand;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAllowedDomainsPort;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.application.service.ProvisionUserOnLoginService;
import co.edu.uniquindio.brujula.identity.domain.AccountStatus;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.DomainNotAllowedException;
import co.edu.uniquindio.brujula.identity.domain.OnboardingState;
import co.edu.uniquindio.brujula.identity.domain.Role;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RED → GREEN use case tests for ProvisionUserOnLoginService.
 *
 * <p>Uses Mockito in-memory stubs — zero Spring context (ARQ-002).
 *
 * <p>Scenarios: first login creates user, subsequent login loads existing, allowed-domain policy
 * enforced, Keycloak role claims mapped to Role VO, default role STUDENT when no recognized role.
 *
 * <p>Satisfies: S1-B-3, Spec §Identity Provisioning on First Login.
 */
@ExtendWith(MockitoExtension.class)
class ProvisionUserOnLoginServiceTest {

  @Mock private LoadAppUserPort loadAppUserPort;
  @Mock private SaveAppUserPort saveAppUserPort;
  @Mock private LoadAllowedDomainsPort loadAllowedDomainsPort;
  @Mock private AuditRecorder auditRecorder;

  private ProvisionUserOnLoginService service;

  @BeforeEach
  void setUp() {
    service =
        new ProvisionUserOnLoginService(
            loadAppUserPort, saveAppUserPort, loadAllowedDomainsPort, auditRecorder);
  }

  // -------------------------------------------------------------------------
  // First login — creates new user
  // -------------------------------------------------------------------------

  @Test
  void first_login_creates_new_app_user() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadAppUserPort.findByOidcSubject("sub-new")).thenReturn(Optional.empty());

    AppUser saved = AppUser.provision("sub-new", "alice@uniquindio.edu.co", "Alice", Role.STUDENT);
    when(saveAppUserPort.save(any())).thenReturn(saved);

    var command =
        new ProvisionCommand("sub-new", "alice@uniquindio.edu.co", "Alice", Set.of("STUDENT"));

    AppUser result = service.provision(command);

    assertThat(result.oidcSubject()).isEqualTo("sub-new");
    assertThat(result.email()).isEqualTo("alice@uniquindio.edu.co");
    verify(saveAppUserPort).save(any());
    // Spec §Audit Log: first login MUST record USER_PROVISIONED with the new user as both
    // actor and entityId (the user is provisioning themselves via OIDC).
    verify(auditRecorder)
        .record(
            argThat(
                cmd ->
                    "USER_PROVISIONED".equals(cmd.action())
                        && "app_user".equals(cmd.entityType())
                        && saved.id().equals(cmd.actorId())
                        && saved.id().equals(cmd.entityId())));
  }

  @Test
  void first_login_creates_user_with_student_role_when_student_claim_present() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadAppUserPort.findByOidcSubject(any())).thenReturn(Optional.empty());

    AppUser saved = AppUser.provision("sub-s", "s@uniquindio.edu.co", "S", Role.STUDENT);
    when(saveAppUserPort.save(any())).thenReturn(saved);

    var result =
        service.provision(
            new ProvisionCommand("sub-s", "s@uniquindio.edu.co", "S", Set.of("STUDENT")));

    assertThat(result.role()).isEqualTo(Role.STUDENT);
  }

  @Test
  void first_login_maps_admin_role_from_keycloak_claims() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadAppUserPort.findByOidcSubject(any())).thenReturn(Optional.empty());

    AppUser saved = AppUser.provision("sub-a", "a@uniquindio.edu.co", "Admin", Role.ADMIN);
    when(saveAppUserPort.save(any())).thenReturn(saved);

    var result =
        service.provision(
            new ProvisionCommand("sub-a", "a@uniquindio.edu.co", "Admin", Set.of("ADMIN")));

    assertThat(result.role()).isEqualTo(Role.ADMIN);
  }

  @Test
  void first_login_defaults_to_student_when_no_recognized_role_claim() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadAppUserPort.findByOidcSubject(any())).thenReturn(Optional.empty());

    AppUser saved = AppUser.provision("sub-u", "u@uniquindio.edu.co", "Unknown", Role.STUDENT);
    when(saveAppUserPort.save(any())).thenReturn(saved);

    var result =
        service.provision(
            new ProvisionCommand(
                "sub-u", "u@uniquindio.edu.co", "Unknown", Set.of("SOME_OTHER_ROLE")));

    assertThat(result.role()).isEqualTo(Role.STUDENT);
  }

  @Test
  void first_login_sets_status_active_and_onboarding_pending() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadAppUserPort.findByOidcSubject(any())).thenReturn(Optional.empty());

    AppUser saved = AppUser.provision("sub-x", "x@uniquindio.edu.co", "X", Role.STUDENT);
    when(saveAppUserPort.save(any())).thenReturn(saved);

    var result =
        service.provision(
            new ProvisionCommand("sub-x", "x@uniquindio.edu.co", "X", Set.of("STUDENT")));

    assertThat(result.status()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(result.onboardingCompleted()).isFalse();
  }

  // -------------------------------------------------------------------------
  // Subsequent login — loads existing user (idempotent)
  // -------------------------------------------------------------------------

  @Test
  void subsequent_login_returns_existing_user_without_creating_new() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));

    AppUser existing =
        AppUser.reconstitute(
            UUID.randomUUID(),
            "sub-existing",
            "e@uniquindio.edu.co",
            "Existing",
            Role.STUDENT,
            AccountStatus.ACTIVE,
            false,
            1L);
    when(loadAppUserPort.findByOidcSubject("sub-existing")).thenReturn(Optional.of(existing));

    var command =
        new ProvisionCommand("sub-existing", "e@uniquindio.edu.co", "Existing", Set.of("STUDENT"));
    AppUser result = service.provision(command);

    assertThat(result.id()).isEqualTo(existing.id());
    assertThat(result.version()).isEqualTo(1L);
    verify(saveAppUserPort, never()).save(any());
    // Subsequent login must NOT record a USER_PROVISIONED event (already provisioned)
    verify(auditRecorder, never()).record(any());
  }

  // -------------------------------------------------------------------------
  // C-002: self-registered user + first OIDC login → link, no duplicate
  // -------------------------------------------------------------------------

  /**
   * C-002 happy path: a self-registered app_user row with oidcSubject=null and email
   * x@uniquindio.edu.co exists. When OIDC login completes for that email with a real sub claim,
   * the EXISTING row must be found (by email), linked (oidcSubject set to the real sub,
   * onboardingState synced from email_verified), and saved — NO new row created.
   */
  @Test
  void selfRegisteredUser_firstOidcLogin_linksExistingRowByEmail_notDuplicate() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));

    // Pre-registered null-subject row (AppUser.register() creates PENDING_VERIFICATION, null subject)
    AppUser preRegistered =
        AppUser.reconstitute(
            UUID.randomUUID(),
            null, // oidcSubject = null
            "x@uniquindio.edu.co",
            "X Student",
            Role.STUDENT,
            AccountStatus.ACTIVE,
            false,
            0L,
            OnboardingState.PENDING_VERIFICATION,
            null,
            null,
            null);

    // findByOidcSubject misses (sub is new), findByEmail finds the null-subject row
    when(loadAppUserPort.findByOidcSubject("real-sub-abc")).thenReturn(Optional.empty());
    when(loadAppUserPort.findByEmail("x@uniquindio.edu.co")).thenReturn(Optional.of(preRegistered));

    // save() returns whatever we pass in (simulate persistence)
    when(saveAppUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var command =
        new ProvisionCommand(
            "real-sub-abc",
            "x@uniquindio.edu.co",
            "X Student",
            Set.of("STUDENT"),
            true); // email_verified=true from Keycloak

    AppUser result = service.provision(command);

    // MUST be the same row (same id), not a new one
    assertThat(result.id()).isEqualTo(preRegistered.id());
    // MUST have the OIDC subject linked
    assertThat(result.oidcSubject()).isEqualTo("real-sub-abc");
    // MUST be ACTIVE (email_verified=true triggered the transition)
    assertThat(result.onboardingState()).isEqualTo(OnboardingState.ACTIVE);

    // MUST save the linked user (not create a new one)
    verify(saveAppUserPort).save(any());
    // Must NOT record USER_PROVISIONED because we're linking an existing self-registered row
    verify(auditRecorder, never()).record(any());
  }

  /**
   * C-002 regression guard: no pre-registered row for the email → first OIDC login creates a new
   * user as before. No regression.
   */
  @Test
  void firstOidcLogin_noPreRegisteredRow_createsNewUser() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadAppUserPort.findByOidcSubject("brand-new-sub")).thenReturn(Optional.empty());
    when(loadAppUserPort.findByEmail("fresh@uniquindio.edu.co")).thenReturn(Optional.empty());

    AppUser saved =
        AppUser.provision("brand-new-sub", "fresh@uniquindio.edu.co", "Fresh User", Role.STUDENT);
    when(saveAppUserPort.save(any())).thenReturn(saved);

    var command =
        new ProvisionCommand(
            "brand-new-sub", "fresh@uniquindio.edu.co", "Fresh User", Set.of("STUDENT"));

    AppUser result = service.provision(command);

    assertThat(result.oidcSubject()).isEqualTo("brand-new-sub");
    verify(saveAppUserPort).save(any());
    verify(auditRecorder).record(any()); // USER_PROVISIONED for genuinely new users
  }

  /**
   * C-002 idempotency: subject already set on subsequent OIDC logins — hits findByOidcSubject
   * directly, no findByEmail call needed.
   */
  @Test
  void subsequentOidcLogin_subjectAlreadyLinked_usesSubjectLookup_notEmailFallback() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));

    AppUser linked =
        AppUser.reconstitute(
            UUID.randomUUID(),
            "real-sub-abc",
            "x@uniquindio.edu.co",
            "X Student",
            Role.STUDENT,
            AccountStatus.ACTIVE,
            false,
            1L,
            OnboardingState.ACTIVE,
            null,
            null,
            null);

    when(loadAppUserPort.findByOidcSubject("real-sub-abc")).thenReturn(Optional.of(linked));

    var command =
        new ProvisionCommand(
            "real-sub-abc", "x@uniquindio.edu.co", "X Student", Set.of("STUDENT"), true);

    AppUser result = service.provision(command);

    assertThat(result.id()).isEqualTo(linked.id());
    // findByEmail must NOT be called — subject lookup was sufficient
    verify(loadAppUserPort, never()).findByEmail(any());
    verify(saveAppUserPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // Domain invariant: allowed-email-domain enforced
  // -------------------------------------------------------------------------

  @Test
  void provision_rejects_email_from_disallowed_domain() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));

    var command = new ProvisionCommand("sub-bad", "user@gmail.com", "Hacker", Set.of("STUDENT"));

    assertThatThrownBy(() -> service.provision(command))
        .isInstanceOf(DomainNotAllowedException.class)
        .hasMessageContaining("gmail.com");

    verify(saveAppUserPort, never()).save(any());
  }

  @Test
  void provision_rejects_when_allowed_domain_list_is_empty() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of());

    var command = new ProvisionCommand("sub-1", "a@uniquindio.edu.co", "A", Set.of("STUDENT"));

    // Empty allowed domains is an invalid state — domain policy throws
    assertThatThrownBy(() -> service.provision(command))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
