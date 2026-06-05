package co.edu.uniquindio.brujula.identity.application.dto;

import java.util.Set;

/**
 * Command carrying the claims extracted from the Keycloak OIDC token.
 *
 * <p>Lives in {@code application.dto} (not {@code application.port.in}) to satisfy ARQ-003, which
 * requires all classes in {@code port.in} to be interfaces.
 *
 * @param oidcSubject the {@code sub} claim (stable, unique per user)
 * @param email the {@code email} claim
 * @param fullName the {@code name} or {@code preferred_username} claim
 * @param keycloakRoles the set of realm roles from {@code realm_access.roles}
 */
public record ProvisionCommand(
    String oidcSubject, String email, String fullName, Set<String> keycloakRoles) {}
