package co.edu.uniquindio.brujula.identity.application.service;

import co.edu.uniquindio.brujula.identity.application.port.in.ResendEmailVerificationUseCase;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case service: resend the email verification message (S2-C).
 *
 * <p>Security invariant — NO USER ENUMERATION:
 * This service completes silently for BOTH known and unknown emails. The caller (controller)
 * always returns 202 Accepted, making it impossible to determine account existence.
 *
 * <p>Orchestration:
 * <ol>
 *   <li>Look up AppUser by email in the local database.
 *   <li>If found, call {@link KeycloakUserProvisioningPort#resendEmailVerification} to re-trigger
 *       the verification email via Keycloak Admin API.
 *   <li>If NOT found, return silently (no Keycloak call, no exception).
 * </ol>
 *
 * <p>Framework-free (ARQ-002). Wired by
 * {@link co.edu.uniquindio.brujula.identity.infrastructure.configuration.IdentityConfiguration}.
 */
public class ResendEmailVerificationService implements ResendEmailVerificationUseCase {

  private static final Logger log = LoggerFactory.getLogger(ResendEmailVerificationService.class);

  private final LoadAppUserPort loadAppUserPort;
  private final KeycloakUserProvisioningPort keycloakPort;
  private final String verifyRedirectUri;

  public ResendEmailVerificationService(
      LoadAppUserPort loadAppUserPort,
      KeycloakUserProvisioningPort keycloakPort,
      String verifyRedirectUri) {
    this.loadAppUserPort = loadAppUserPort;
    this.keycloakPort = keycloakPort;
    this.verifyRedirectUri = verifyRedirectUri;
  }

  @Override
  public void resend(String email) {
    // Look up user locally — if not found, return silently (no user enumeration).
    var maybeUser = loadAppUserPort.findByEmail(email);
    if (maybeUser.isEmpty()) {
      // Do NOT log at INFO — logging would create a timing/log side-channel.
      log.debug("resendEmailVerification: email not found locally, returning silently (no enumeration)");
      return;
    }

    // User exists — trigger email verification via Keycloak (resilient: swallow Keycloak errors).
    // W-003: identical resilience pattern to InitiatePasswordRecoveryService — Keycloak failures
    // must not propagate, keeping the 202 response uniform for known and unknown emails alike.
    try {
      keycloakPort.resendEmailVerification(email, verifyRedirectUri);
      log.debug("Email verification resend triggered for user: {}", email);
    } catch (KeycloakUserProvisioningPort.KeycloakProvisioningException e) {
      // Resilience decision: Keycloak failures must not propagate to the client.
      // The caller always gets 202. Log at WARN so ops can investigate.
      log.warn(
          "Email verification resend Keycloak call failed for user {} — returning 202 anyway. Error: {}",
          email,
          e.getMessage());
    }
  }
}
