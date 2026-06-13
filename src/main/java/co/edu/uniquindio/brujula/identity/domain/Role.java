package co.edu.uniquindio.brujula.identity.domain;

/**
 * Role value object — maps to Keycloak realm roles and to the {@code role} column of {@code
 * identity.app_user}.
 *
 * <p>No Spring/JPA imports. Matches {@code UserRole} enum in the OpenAPI contract.
 */
public enum Role {
  STUDENT,
  STAFF,
  ADMIN
}
