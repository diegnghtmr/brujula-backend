package co.edu.uniquindio.brujula.identity.application.dto;

import java.util.Set;

/**
 * Command carrying the claims extracted from the Keycloak OIDC token.
 *
 * <p>Lives in {@code application.dto} (not {@code application.port.in}) because port.in must
 * contain interfaces only.
 *
 * @param oidcSubject the {@code sub} claim (stable, unique per user)
 * @param email the {@code email} claim
 * @param fullName the {@code name} or {@code preferred_username} claim
 * @param keycloakRoles the set of realm roles from {@code realm_access.roles}
 * @param emailVerified the {@code email_verified} claim from the OIDC token (null treated as
 *     false). Used by {@code ProvisionUserOnLoginService} to sync onboardingState
 *     PENDING_VERIFICATION → ACTIVE when Keycloak has verified the email (obs #1574 sync-on-login).
 */
public record ProvisionCommand(
    String oidcSubject,
    String email,
    String fullName,
    Set<String> keycloakRoles,
    Boolean emailVerified) {

  /** Backward-compat factory — creates a command with emailVerified=null (treated as false). */
  public ProvisionCommand(
      String oidcSubject, String email, String fullName, Set<String> keycloakRoles) {
    this(oidcSubject, email, fullName, keycloakRoles, null);
  }
}
