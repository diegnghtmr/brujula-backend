package co.edu.uniquindio.brujula.identity.domain;

import java.util.UUID;

/**
 * Thrown when an AppUser is not found by id or OIDC subject.
 *
 * <p>Maps to HTTP 404 at the REST adapter layer.
 */
public class UserNotFoundException extends DomainException {

  public UserNotFoundException(UUID id) {
    super("User not found: " + id);
  }

  public UserNotFoundException(String oidcSubject) {
    super("User not found for OIDC subject: " + oidcSubject);
  }
}
