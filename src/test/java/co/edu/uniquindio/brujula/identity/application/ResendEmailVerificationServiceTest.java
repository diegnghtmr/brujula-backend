package co.edu.uniquindio.brujula.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.identity.application.port.in.ResendEmailVerificationUseCase;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.application.service.ResendEmailVerificationService;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RED → GREEN use case tests for ResendEmailVerificationService (S2-C).
 *
 * <p>STRICT TDD: tests written RED before the service exists.
 *
 * <p>Key security constraint: NO USER ENUMERATION. The endpoint MUST return the same 202 response
 * regardless of whether the email is registered or not. These tests prove it at the service layer.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>Known email → port triggered with correct KeycloakUserId
 *   <li>Unknown email → port NOT triggered, service returns normally (no exception)
 *   <li>Both paths return void (no exception) — identical behavior proves no enumeration
 * </ul>
 *
 * <p>Satisfies: S2-C, Spec §Email verification resend, RNF-SEG-006 no user enumeration.
 */
@ExtendWith(MockitoExtension.class)
class ResendEmailVerificationServiceTest {

  @Mock private LoadAppUserPort loadAppUserPort;
  @Mock private KeycloakUserProvisioningPort keycloakPort;

  private static final String VERIFY_REDIRECT_URI = "http://localhost:5173/verify-email";

  private ResendEmailVerificationUseCase sut;

  @BeforeEach
  void setUp() {
    sut = new ResendEmailVerificationService(loadAppUserPort, keycloakPort, VERIFY_REDIRECT_URI);
  }

  // -------------------------------------------------------------------------
  // Known email → Keycloak triggered
  // -------------------------------------------------------------------------

  @Test
  void resend_withKnownEmail_triggersEmailVerification_returnsNormally() {
    // Arrange — user exists in the DB
    AppUser existingUser = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");
    when(loadAppUserPort.findByEmail("student@uniquindio.edu.co"))
        .thenReturn(Optional.of(existingUser));

    // The adapter looks up by email in Keycloak and triggers the action — mock port returns a
    // KeycloakUserId (the adapter finds it by email internally; port contract uses email for
    // resend)
    // The service calls triggerEmailVerificationByEmail which maps to the existing port method
    // For resend we use triggerEmailVerification with a by-email approach: port looks up user.
    // Design decision: add resendEmailVerification(email, redirectUri) to the port — this avoids
    // the need for a separate DB-to-Keycloak ID lookup in the service (the adapter already has the
    // by-email logic in triggerPasswordReset). We reuse the same Keycloak user-lookup pattern.

    // Act — should return without throwing (202 behavior)
    sut.resend("student@uniquindio.edu.co");

    // Assert — Keycloak was triggered
    verify(keycloakPort)
        .resendEmailVerification(eq("student@uniquindio.edu.co"), eq(VERIFY_REDIRECT_URI));
  }

  // -------------------------------------------------------------------------
  // Unknown email → NO user enumeration, returns normally (no exception, no Keycloak call)
  // -------------------------------------------------------------------------

  @Test
  void resend_withUnknownEmail_doesNotTriggerKeycloak_andReturnsNormally() {
    // Arrange — email not found in DB
    when(loadAppUserPort.findByEmail("nobody@uniquindio.edu.co")).thenReturn(Optional.empty());

    // Act — must NOT throw; caller still receives 202
    sut.resend("nobody@uniquindio.edu.co");

    // Assert — NO Keycloak call (user does not exist locally)
    verify(keycloakPort, never()).resendEmailVerification(anyString(), anyString());
  }

  // -------------------------------------------------------------------------
  // No-enumeration proof: known and unknown return identically (both complete without exception)
  // -------------------------------------------------------------------------

  @Test
  void resend_knownAndUnknownEmail_bothCompleteWithoutException() {
    // Arrange
    AppUser existingUser = AppUser.register("known@uniquindio.edu.co", "Known User");
    when(loadAppUserPort.findByEmail("known@uniquindio.edu.co"))
        .thenReturn(Optional.of(existingUser));
    when(loadAppUserPort.findByEmail("unknown@uniquindio.edu.co")).thenReturn(Optional.empty());

    // Act + Assert — both calls complete without throwing (identical observable outcome)
    // This is the no-enumeration proof: callers cannot distinguish known from unknown.
    sut.resend("known@uniquindio.edu.co"); // known: completes normally
    sut.resend("unknown@uniquindio.edu.co"); // unknown: also completes normally, no exception

    // The difference is only in side-effects the caller cannot observe:
    // known → Keycloak call made; unknown → Keycloak call NOT made.
    // The HTTP response (202 void) is identical.
    assertThat(true).as("Both invocations completed without exception").isTrue();
  }

  // -------------------------------------------------------------------------
  // W-003: Keycloak failure → swallowed, returns normally (no 500 leak)
  // -------------------------------------------------------------------------

  /**
   * W-003 (no-enumeration consistency): when Keycloak throws {@code KeycloakProvisioningException}
   * for a KNOWN email, the service must swallow it and return normally — identical to {@code
   * InitiatePasswordRecoveryService} (which already swallows). Without this fix a Keycloak 5xx for
   * a known email would produce a 500 while an unknown email produces 202, creating a
   * status-channel asymmetry that partially breaks no-enumeration.
   */
  @Test
  void resend_whenKeycloakFails_swallowsException_andReturnsNormally() {
    // Arrange — user exists in the DB
    AppUser existingUser = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");
    when(loadAppUserPort.findByEmail("student@uniquindio.edu.co"))
        .thenReturn(Optional.of(existingUser));

    // Port throws KeycloakProvisioningException (simulates Keycloak 5xx)
    doThrow(
            new KeycloakUserProvisioningPort.KeycloakProvisioningException(
                "Keycloak unavailable", "KEYCLOAK_SERVER_ERROR"))
        .when(keycloakPort)
        .resendEmailVerification(anyString(), anyString());

    // Act + Assert — must NOT propagate; service returns normally (endpoint stays 202)
    assertThatCode(() -> sut.resend("student@uniquindio.edu.co"))
        .as("KeycloakProvisioningException must be swallowed — not propagated to the caller")
        .doesNotThrowAnyException();
  }
}
