package co.edu.uniquindio.brujula.identity.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.AuthApi;
import co.edu.uniquindio.brujula.generated.model.RegistrationAccepted;
import co.edu.uniquindio.brujula.generated.model.RequestPasswordRecoveryRequest;
import co.edu.uniquindio.brujula.generated.model.ResendEmailVerificationRequest;
import co.edu.uniquindio.brujula.generated.model.StudentRegistrationRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin shim implementing the generated {@link AuthApi} interface.
 *
 * <p>G2 RESOLUTION: {@code POST /auth/logout} is handled by Spring Security's {@code logout()}
 * filter BEFORE the DispatcherServlet. This shim returns 204 in the unlikely event the request
 * reaches this controller (Spring Security's filter runs first). No domain logic here.
 *
 * <p>S2-A-3 CONTRACT UPDATE: confirmEmailVerification and confirmPasswordRecovery have been removed
 * from the API contract. Keycloak handles email verification and password reset natively on its own
 * pages. No backend token-confirm endpoints are needed. See architecture decision #1574.
 *
 * <p>All other auth operations (registration, verification, recovery) are Slice 2 — they return 501
 * Not Implemented here. They will be fully implemented in Slice 2 apply batches S2-B..D.
 *
 * <p>Satisfies: S1-B-5 (G2 resolve), Design §2.1 AuthApi shim, S2-A-3 contract audit.
 */
@RestController
class AuthController implements AuthApi {

  /** Shim — Spring Security handles POST /auth/logout before DispatcherServlet. */
  @Override
  public ResponseEntity<Void> logout(String xXsrfToken) {
    // Spring Security's logout filter always processes /auth/logout first.
    // If this method is ever reached, the session was already invalidated by Spring.
    return ResponseEntity.noContent().build();
  }

  /** Slice 2 — not implemented until WU-E-B. */
  @Override
  public ResponseEntity<RegistrationAccepted> registerStudent(
      UUID idempotencyKey, StudentRegistrationRequest body) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  /** Slice 2 — resend email verification trigger. Not fully implemented until WU-E-C. */
  @Override
  public ResponseEntity<Void> resendEmailVerification(
      UUID idempotencyKey, ResendEmailVerificationRequest body) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  /** Slice 2 — not implemented until WU-E-D. */
  @Override
  public ResponseEntity<Void> requestPasswordRecovery(RequestPasswordRecoveryRequest body) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
