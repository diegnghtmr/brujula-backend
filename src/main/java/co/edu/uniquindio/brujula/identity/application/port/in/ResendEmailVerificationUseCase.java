package co.edu.uniquindio.brujula.identity.application.port.in;

/**
 * Input port: resend the email verification message to a given email address (S2-C).
 *
 * <p>Security: NO USER ENUMERATION. The operation completes silently regardless of whether the
 * email is registered or not. The caller always receives a 202 Accepted response — the existence
 * (or absence) of an account is never revealed.
 *
 * <p>Framework-free (ARQ-002, ARQ-003).
 */
public interface ResendEmailVerificationUseCase {

  /**
   * Resend the email verification link for the given email.
   *
   * <p>If the email is registered and unverified, the verification email is resent via Keycloak. If
   * the email is not registered, the method returns silently (no exception, no Keycloak call).
   *
   * @param email the institutional email address from the request
   */
  void resend(String email);
}
