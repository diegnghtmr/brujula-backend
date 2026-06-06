package co.edu.uniquindio.brujula.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
 * RED → GREEN sync-on-login tests for ProvisionUserOnLoginService.
 *
 * <p>When an existing AppUser in PENDING_VERIFICATION state logs in via OIDC with
 * email_verified=true in claims, the service MUST transition onboardingState to ACTIVE and persist.
 *
 * <p>Scenarios: (1) PENDING_VERIFICATION + email_verified=true → ACTIVE + save called. (2)
 * PENDING_VERIFICATION + email_verified=false → stays PENDING_VERIFICATION + save not called. (3)
 * ACTIVE user → unchanged regardless of email_verified. (4) email_verified field absent (null) →
 * treated as false (no transition).
 *
 * <p>Satisfies: S2-B-4, Spec §Email verification confirm endpoint triggers Keycloak action,
 * Architecture decision: sync-on-login (obs #1574).
 */
@ExtendWith(MockitoExtension.class)
class SyncOnLoginTest {

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
  // PENDING_VERIFICATION + email_verified=true → transition to ACTIVE + save
  // -------------------------------------------------------------------------

  @Test
  void pendingVerification_withEmailVerifiedTrue_transitionsToActive_andSaves() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));

    AppUser existing =
        AppUser.reconstitute(
            UUID.randomUUID(),
            "sub-pending",
            "pending@uniquindio.edu.co",
            "Pending Student",
            Role.STUDENT,
            AccountStatus.ACTIVE,
            false,
            1L,
            OnboardingState.PENDING_VERIFICATION,
            null,
            null,
            null);
    when(loadAppUserPort.findByOidcSubject("sub-pending")).thenReturn(Optional.of(existing));
    when(saveAppUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var command =
        new ProvisionCommand("sub-pending", "pending@uniquindio.edu.co", "Pending Student",
            Set.of("STUDENT"), true);

    AppUser result = service.provision(command);

    assertThat(result.onboardingState())
        .as("Should be ACTIVE after email_verified=true sync-on-login")
        .isEqualTo(OnboardingState.ACTIVE);

    // Verify save was called with ACTIVE state
    verify(saveAppUserPort)
        .save(argThat(u -> u.onboardingState() == OnboardingState.ACTIVE));
  }

  // -------------------------------------------------------------------------
  // PENDING_VERIFICATION + email_verified=false → stays PENDING, no extra save
  // -------------------------------------------------------------------------

  @Test
  void pendingVerification_withEmailVerifiedFalse_staysPendingVerification() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));

    AppUser existing =
        AppUser.reconstitute(
            UUID.randomUUID(),
            "sub-pending2",
            "pending2@uniquindio.edu.co",
            "Pending2",
            Role.STUDENT,
            AccountStatus.ACTIVE,
            false,
            1L,
            OnboardingState.PENDING_VERIFICATION,
            null,
            null,
            null);
    when(loadAppUserPort.findByOidcSubject("sub-pending2")).thenReturn(Optional.of(existing));

    var command =
        new ProvisionCommand("sub-pending2", "pending2@uniquindio.edu.co", "Pending2",
            Set.of("STUDENT"), false);

    AppUser result = service.provision(command);

    assertThat(result.onboardingState())
        .as("Should remain PENDING_VERIFICATION when email_verified=false")
        .isEqualTo(OnboardingState.PENDING_VERIFICATION);
  }

  // -------------------------------------------------------------------------
  // ACTIVE user — no state change regardless of email_verified
  // -------------------------------------------------------------------------

  @Test
  void activeUser_withEmailVerifiedTrue_remainsActive_noExtraTransition() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));

    AppUser existing =
        AppUser.reconstitute(
            UUID.randomUUID(),
            "sub-active",
            "active@uniquindio.edu.co",
            "Active User",
            Role.STUDENT,
            AccountStatus.ACTIVE,
            false,
            2L,
            OnboardingState.ACTIVE,
            null,
            null,
            null);
    when(loadAppUserPort.findByOidcSubject("sub-active")).thenReturn(Optional.of(existing));

    var command =
        new ProvisionCommand("sub-active", "active@uniquindio.edu.co", "Active User",
            Set.of("STUDENT"), true);

    AppUser result = service.provision(command);

    assertThat(result.onboardingState())
        .as("ACTIVE user should remain ACTIVE")
        .isEqualTo(OnboardingState.ACTIVE);
  }

  // -------------------------------------------------------------------------
  // email_verified absent (null) — treated as false
  // -------------------------------------------------------------------------

  @Test
  void pendingVerification_withEmailVerifiedNull_staysPendingVerification() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));

    AppUser existing =
        AppUser.reconstitute(
            UUID.randomUUID(),
            "sub-pending3",
            "pending3@uniquindio.edu.co",
            "Pending3",
            Role.STUDENT,
            AccountStatus.ACTIVE,
            false,
            1L,
            OnboardingState.PENDING_VERIFICATION,
            null,
            null,
            null);
    when(loadAppUserPort.findByOidcSubject("sub-pending3")).thenReturn(Optional.of(existing));

    // null emailVerified — treated as false
    var command =
        new ProvisionCommand("sub-pending3", "pending3@uniquindio.edu.co", "Pending3",
            Set.of("STUDENT"), null);

    AppUser result = service.provision(command);

    assertThat(result.onboardingState())
        .as("null email_verified should not trigger transition")
        .isEqualTo(OnboardingState.PENDING_VERIFICATION);
  }
}
