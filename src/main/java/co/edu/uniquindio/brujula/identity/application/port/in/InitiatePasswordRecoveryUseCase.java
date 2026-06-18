package co.edu.uniquindio.brujula.identity.application.port.in;

/**
 * Input port: initiate the password recovery flow for a given email address.
 *
 * <p>Security: NO USER ENUMERATION. The operation completes silently regardless of whether the
 * email is registered or not. The caller always receives a 202 Accepted response — the existence
 * (or absence) of an account is never revealed.
 *
 * <p>Architecture note: Keycloak handles the password reset natively on its own pages. The backend
 * triggers {@code execute-actions-email [UPDATE_PASSWORD]} via the Keycloak Admin API; Keycloak
 * sends the reset link. The backend NEVER receives or stores passwords.
 *
 * <p>Framework-free.
 */
public interface InitiatePasswordRecoveryUseCase {

  /**
   * Initiate the password recovery flow for the given email.
   *
   * <p>If the email is registered, Keycloak sends a password reset link to the address. If the
   * email is not registered, the method returns silently (no exception, no Keycloak call). If
   * Keycloak is unavailable, the exception is swallowed and the method returns silently (resilience
   * decision: the caller always gets 202).
   *
   * @param email the email address from the recovery request
   */
  void initiateRecovery(String email);
}
