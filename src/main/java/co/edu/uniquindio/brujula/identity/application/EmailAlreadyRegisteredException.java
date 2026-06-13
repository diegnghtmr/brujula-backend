package co.edu.uniquindio.brujula.identity.application;

/**
 * Thrown when a student attempts to register with an email address that already has an AppUser
 * record.
 *
 * <p>The REST adapter maps this to 409 Conflict per the OpenAPI contract for {@code POST
 * /auth/registration} (status code 409: "Correo ya registrado").
 *
 * <p>Framework-free: no Spring imports.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

  private final String email;

  public EmailAlreadyRegisteredException(String email) {
    super("A user with email '%s' is already registered".formatted(email));
    this.email = email;
  }

  public String getEmail() {
    return email;
  }
}
