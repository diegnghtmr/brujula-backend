package co.edu.uniquindio.brujula.identity.application.service;

import co.edu.uniquindio.brujula.identity.application.port.in.InitiatePasswordRecoveryUseCase;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case service: initiate the password recovery flow (S2-D).
 *
 * <p>Security invariant — NO USER ENUMERATION: This service completes silently for BOTH known and
 * unknown emails. The caller (controller) always returns 202 Accepted, making it impossible to
 * determine account existence.
 *
 * <p>OPTION A / DT-011: The backend triggers {@code execute-actions-email [UPDATE_PASSWORD]} via
 * Keycloak Admin API. Keycloak sends the reset link and handles the reset page natively. The
 * backend NEVER receives, stores, or validates passwords.
 *
 * <p>Orchestration:
 *
 * <ol>
 *   <li>Look up AppUser by email in the local database.
 *   <li>If found, call {@link KeycloakUserProvisioningPort#triggerPasswordReset} (resilient:
 *       Keycloak exceptions are swallowed and logged at WARN).
 *   <li>If NOT found, return silently (no Keycloak call, no exception).
 * </ol>
 *
 * <p>Framework-free (ARQ-002). Wired by {@link
 * co.edu.uniquindio.brujula.identity.infrastructure.configuration.IdentityConfiguration}.
 */
public class InitiatePasswordRecoveryService implements InitiatePasswordRecoveryUseCase {

  private static final Logger log = LoggerFactory.getLogger(InitiatePasswordRecoveryService.class);

  private final LoadAppUserPort loadAppUserPort;
  private final KeycloakUserProvisioningPort keycloakPort;
  private final String recoveryRedirectUri;

  public InitiatePasswordRecoveryService(
      LoadAppUserPort loadAppUserPort,
      KeycloakUserProvisioningPort keycloakPort,
      String recoveryRedirectUri) {
    this.loadAppUserPort = loadAppUserPort;
    this.keycloakPort = keycloakPort;
    this.recoveryRedirectUri = recoveryRedirectUri;
  }

  @Override
  public void initiateRecovery(String email) {
    // Look up user locally — if not found, return silently (no user enumeration).
    var maybeUser = loadAppUserPort.findByEmail(email);
    if (maybeUser.isEmpty()) {
      log.debug(
          "initiatePasswordRecovery: email not found locally, returning silently (no enumeration)");
      return;
    }

    // User exists — trigger Keycloak password reset (resilient: swallow Keycloak errors).
    try {
      keycloakPort.triggerPasswordReset(email, recoveryRedirectUri);
      log.debug("Password recovery triggered for user: {}", email);
    } catch (KeycloakUserProvisioningPort.KeycloakProvisioningException e) {
      // Resilience decision: Keycloak failures must not propagate to the client.
      // The caller always gets 202. Log at WARN so ops can investigate.
      log.warn(
          "Password recovery Keycloak call failed for user {} — returning 202 anyway. Error: {}",
          email,
          e.getMessage());
    }
  }
}
