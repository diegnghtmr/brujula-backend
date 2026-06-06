package co.edu.uniquindio.brujula.identity.application.port.out;

/**
 * Output port: provisions and manages users in Keycloak via the Admin REST API.
 *
 * <p>Port placement decision: identity module. Student registration creates a Keycloak user tightly
 * coupled to {@code identity.domain.AppUser}. The {@code RegisterStudentService} (S2-B) lives in
 * {@code identity.application.service} and is the primary consumer of this port. Keeping the port
 * in {@code identity} avoids a new cross-module dependency from {@code identity} → {@code
 * administration.port.out}. The adapter implementing this port lives in {@code
 * identity.infrastructure.adapter.out.client}.
 *
 * <p>Keycloak-native verification flow (architecture decision, obs #1574):
 *
 * <ul>
 *   <li>Registration: {@link #createUser} → emailVerified=false, then {@link
 *       #triggerEmailVerification} triggers execute-actions-email[VERIFY_EMAIL].
 *   <li>Keycloak handles verification on its own page; redirects back to SPA via redirectUri.
 *   <li>Backend syncs onboardingState PENDING_VERIFICATION→ACTIVE on next OIDC login (claim
 *       email_verified=true). NO backend token-confirm endpoint.
 *   <li>Password recovery: {@link #triggerPasswordReset} triggers execute-actions-email
 *       [UPDATE_PASSWORD]; Keycloak handles the reset page natively.
 * </ul>
 *
 * <p>Framework-free (ARQ-002): no Spring/Jakarta imports allowed at this layer.
 */
public interface KeycloakUserProvisioningPort {

  /**
   * Create a new user in Keycloak.
   *
   * <p>Maps to {@code POST /admin/realms/{realm}/users} (Keycloak 26.4). Response 201 with {@code
   * Location} header; adapter extracts the user ID from the Location URI.
   *
   * @param command user creation data (email, fullName)
   * @return the Keycloak user ID extracted from the Location header
   * @throws KeycloakProvisioningException if Keycloak returns an error (e.g. 409 duplicate)
   */
  KeycloakUserId createUser(ProvisionUserCommand command);

  /**
   * Trigger an email verification action for an existing Keycloak user.
   *
   * <p>Maps to {@code PUT /admin/realms/{realm}/users/{id}/execute-actions-email} with body {@code
   * ["VERIFY_EMAIL"]}, query params {@code redirectUri} and {@code clientId}. Response 204. The
   * {@code redirectUri} must be registered as a valid redirect URI for the SPA client in the realm
   * or Keycloak will reject it.
   *
   * @param userId the Keycloak user ID (from {@link #createUser})
   * @param redirectUri the SPA landing URI to redirect after verification (e.g. {@code
   *     http://localhost:5173/verify-email})
   * @throws KeycloakProvisioningException if Keycloak returns a non-2xx error
   */
  void triggerEmailVerification(KeycloakUserId userId, String redirectUri);

  /**
   * Trigger a password reset action for a user identified by email.
   *
   * <p>Maps to {@code PUT /admin/realms/{realm}/users/{id}/execute-actions-email} with body {@code
   * ["UPDATE_PASSWORD"]}. Requires a prior user lookup by email to obtain the ID. Response 204.
   * Keycloak sends the reset link; the SPA/backend do NOT handle the reset form.
   *
   * @param email the institutional email of the user (used to look up the Keycloak user ID)
   * @param redirectUri the SPA landing URI to redirect after the password reset
   * @throws KeycloakProvisioningException if Keycloak returns a non-2xx error
   */
  void triggerPasswordReset(String email, String redirectUri);

  /**
   * Resend the email verification action for a user identified by email.
   *
   * <p>Used by the email-verification resend endpoint (S2-C). Looks up the Keycloak user by email,
   * then triggers {@code execute-actions-email ["VERIFY_EMAIL", "UPDATE_PASSWORD"]} (OPTION A).
   * If no user is found in Keycloak, returns silently (no user enumeration).
   *
   * @param email the institutional email of the user to look up in Keycloak
   * @param redirectUri the SPA landing URI to redirect after verification
   * @throws KeycloakProvisioningException if Keycloak returns a non-2xx error (caller decides
   *     whether to swallow or propagate)
   */
  void resendEmailVerification(String email, String redirectUri);

  // -------------------------------------------------------------------------
  // Value objects (nested in the port — framework-free, application-layer VO)
  // -------------------------------------------------------------------------

  /**
   * Command for creating a user in Keycloak.
   *
   * @param email the institutional email address
   * @param fullName the user's full name (firstName + lastName combined)
   */
  record ProvisionUserCommand(String email, String fullName) {

    public ProvisionUserCommand {
      if (email == null || email.isBlank()) {
        throw new IllegalArgumentException("email must not be null or blank");
      }
      if (fullName == null || fullName.isBlank()) {
        throw new IllegalArgumentException("fullName must not be null or blank");
      }
    }
  }

  // -------------------------------------------------------------------------
  // Exception hierarchy
  // -------------------------------------------------------------------------

  /**
   * Thrown when Keycloak returns a non-2xx response or an unexpected error occurs.
   *
   * <p>Carries a machine-readable {@link #getCode()} string for downstream error mapping. Examples:
   * {@code KEYCLOAK_DUPLICATE_USER} (409), {@code KEYCLOAK_USER_NOT_FOUND} (404), {@code
   * KEYCLOAK_SERVER_ERROR} (5xx).
   */
  class KeycloakProvisioningException extends RuntimeException {

    private final String code;

    public KeycloakProvisioningException(String message, String code) {
      super(message);
      this.code = code;
    }

    public KeycloakProvisioningException(String message, String code, Throwable cause) {
      super(message, cause);
      this.code = code;
    }

    public String getCode() {
      return code;
    }
  }
}
