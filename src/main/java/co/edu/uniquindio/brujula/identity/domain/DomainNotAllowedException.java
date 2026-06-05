package co.edu.uniquindio.brujula.identity.domain;

/**
 * Thrown when an email domain is not in the allowed-domain list (RN-007).
 *
 * <p>Maps to HTTP 422 at the REST adapter layer.
 */
public class DomainNotAllowedException extends DomainException {

  private final String email;

  public DomainNotAllowedException(String email) {
    super("Email domain not allowed: " + email);
    this.email = email;
  }

  public String getEmail() {
    return email;
  }
}
