package co.edu.uniquindio.brujula.identity.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.AuthApi;
import co.edu.uniquindio.brujula.generated.model.ConfirmEmailVerificationRequest;
import co.edu.uniquindio.brujula.generated.model.ConfirmPasswordRecoveryRequest;
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
 * <p>All other auth operations (registration, verification, recovery) are Slice 2 — they return 501
 * Not Implemented in WU-B. They will be implemented in the Slice 2 apply batch.
 *
 * <p>Satisfies: S1-B-5 (G2 resolve), Design §2.1 AuthApi shim.
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

  /** Slice 2 — not implemented in WU-B. */
  @Override
  public ResponseEntity<RegistrationAccepted> registerStudent(
      UUID idempotencyKey, StudentRegistrationRequest body) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  /** Slice 2 — not implemented in WU-B. */
  @Override
  public ResponseEntity<Void> confirmEmailVerification(ConfirmEmailVerificationRequest body) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  /** Slice 2 — not implemented in WU-B. */
  @Override
  public ResponseEntity<Void> resendEmailVerification(
      UUID idempotencyKey, ResendEmailVerificationRequest body) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  /** Slice 2 — not implemented in WU-B. */
  @Override
  public ResponseEntity<Void> requestPasswordRecovery(RequestPasswordRecoveryRequest body) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  /** Slice 2 — not implemented in WU-B. */
  @Override
  public ResponseEntity<Void> confirmPasswordRecovery(ConfirmPasswordRecoveryRequest body) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
