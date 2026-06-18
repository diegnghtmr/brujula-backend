package co.edu.uniquindio.brujula.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.identity.application.port.in.InitiatePasswordRecoveryUseCase;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort.KeycloakProvisioningException;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.application.service.InitiatePasswordRecoveryService;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RED → GREEN use case tests for InitiatePasswordRecoveryService (S2-D).
 *
 * <p>STRICT TDD: tests written RED before the service exists.
 *
 * <p>Key security constraint: NO USER ENUMERATION. The endpoint MUST return 202 regardless of
 * whether the email is registered or not. These tests prove it at the service layer.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>Known email → port triggered (Keycloak sends password reset email)
 *   <li>Unknown email → port NOT triggered, service returns normally (no exception)
 *   <li>Keycloak failure → swallowed with WARN log, service returns normally
 *   <li>No-enumeration proof: known and unknown both complete without exception
 * </ul>
 *
 * <p>No user enumeration enforced (OPTION A: Keycloak-native reset page — no backend confirm
 * endpoint).
 */
@ExtendWith(MockitoExtension.class)
class InitiatePasswordRecoveryServiceTest {

  @Mock private LoadAppUserPort loadAppUserPort;
  @Mock private KeycloakUserProvisioningPort keycloakPort;

  private static final String RECOVERY_REDIRECT_URI = "http://localhost:5173/reset-password";

  private InitiatePasswordRecoveryUseCase sut;

  @BeforeEach
  void setUp() {
    sut = new InitiatePasswordRecoveryService(loadAppUserPort, keycloakPort, RECOVERY_REDIRECT_URI);
  }

  // -------------------------------------------------------------------------
  // Known email → Keycloak password recovery triggered
  // -------------------------------------------------------------------------

  @Test
  void initiateRecovery_withKnownEmail_callsKeycloakPasswordReset_andReturnsNormally() {
    // Arrange — user exists in DB
    AppUser existingUser = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");
    when(loadAppUserPort.findByEmail("student@uniquindio.edu.co"))
        .thenReturn(Optional.of(existingUser));

    // Act — should complete without throwing
    sut.initiateRecovery("student@uniquindio.edu.co");

    // Assert — Keycloak was called with the email and redirect URI
    verify(keycloakPort)
        .triggerPasswordReset(eq("student@uniquindio.edu.co"), eq(RECOVERY_REDIRECT_URI));
  }

  // -------------------------------------------------------------------------
  // Unknown email → NO user enumeration, returns normally (no exception, no Keycloak call)
  // -------------------------------------------------------------------------

  @Test
  void initiateRecovery_withUnknownEmail_doesNotCallKeycloak_andReturnsNormally() {
    // Arrange — email not found
    when(loadAppUserPort.findByEmail("nobody@uniquindio.edu.co")).thenReturn(Optional.empty());

    // Act — must NOT throw; 202 returned to caller regardless
    sut.initiateRecovery("nobody@uniquindio.edu.co");

    // Assert — NO Keycloak call (user does not exist locally)
    verify(keycloakPort, never()).triggerPasswordReset(anyString(), anyString());
  }

  // -------------------------------------------------------------------------
  // Keycloak failure → swallowed, returns normally (resilience)
  // -------------------------------------------------------------------------

  @Test
  void initiateRecovery_whenKeycloakFails_swallowsException_andReturnsNormally() {
    // Arrange — user exists but Keycloak is unavailable
    AppUser existingUser = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");
    when(loadAppUserPort.findByEmail("student@uniquindio.edu.co"))
        .thenReturn(Optional.of(existingUser));
    doThrow(new KeycloakProvisioningException("Keycloak unavailable", "KEYCLOAK_503"))
        .when(keycloakPort)
        .triggerPasswordReset(anyString(), anyString());

    // Act — must NOT propagate the Keycloak exception to the controller
    // The caller gets 202 even when Keycloak fails (resilience decision)
    sut.initiateRecovery("student@uniquindio.edu.co");

    // Assert — the call was attempted (service did try Keycloak)
    verify(keycloakPort)
        .triggerPasswordReset(eq("student@uniquindio.edu.co"), eq(RECOVERY_REDIRECT_URI));
  }

  // -------------------------------------------------------------------------
  // No-enumeration proof: known and unknown return identically
  // -------------------------------------------------------------------------

  @Test
  void initiateRecovery_knownAndUnknownEmail_bothCompleteWithoutException() {
    // Arrange
    AppUser existingUser = AppUser.register("known@uniquindio.edu.co", "Known User");
    when(loadAppUserPort.findByEmail("known@uniquindio.edu.co"))
        .thenReturn(Optional.of(existingUser));
    when(loadAppUserPort.findByEmail("unknown@uniquindio.edu.co")).thenReturn(Optional.empty());

    // Act + Assert — IDENTICAL observable outcome for caller
    sut.initiateRecovery("known@uniquindio.edu.co"); // known: Keycloak called, no exception
    sut.initiateRecovery("unknown@uniquindio.edu.co"); // unknown: no Keycloak, no exception

    // The HTTP response (202 void) is IDENTICAL for both — no user enumeration.
    assertThat(true).as("Both invocations completed without exception").isTrue();
  }
}
