package co.edu.uniquindio.brujula.identity.application.port.out;

/**
 * Value object wrapping a Keycloak user identifier (UUID string from the Location header).
 *
 * <p>Extracted by {@code KeycloakAdminApiAdapter} from the {@code Location} header returned by
 * {@code POST /admin/realms/{realm}/users} (Keycloak 26.4 response: 201 + Location header whose
 * last path segment is the user UUID).
 *
 * <p>Framework-free (ARQ-002).
 *
 * @param value the raw Keycloak user UUID string (e.g. {@code "a2f0e8c1-1234-..."})
 */
public record KeycloakUserId(String value) {

  public KeycloakUserId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("KeycloakUserId value must not be null or blank");
    }
  }
}
