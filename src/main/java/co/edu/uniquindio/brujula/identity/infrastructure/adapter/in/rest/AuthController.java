package co.edu.uniquindio.brujula.identity.infrastructure.adapter.in.rest;

import co.edu.uniquindio.brujula.generated.api.AuthApi;
import co.edu.uniquindio.brujula.generated.model.AccountStatus;
import co.edu.uniquindio.brujula.generated.model.RegistrationAccepted;
import co.edu.uniquindio.brujula.generated.model.RequestPasswordRecoveryRequest;
import co.edu.uniquindio.brujula.generated.model.ResendEmailVerificationRequest;
import co.edu.uniquindio.brujula.generated.model.StudentRegistrationRequest;
import co.edu.uniquindio.brujula.identity.application.dto.RegisterStudentCommand;
import co.edu.uniquindio.brujula.identity.application.dto.RegisterStudentResult;
import co.edu.uniquindio.brujula.identity.application.port.in.RegisterStudentUseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter implementing the generated {@link AuthApi} interface.
 *
 * <p>G2 RESOLUTION: {@code POST /auth/logout} is handled by Spring Security's {@code logout()}
 * filter BEFORE the DispatcherServlet. This shim returns 204 in the unlikely event the request
 * reaches this controller (Spring Security's filter runs first). No domain logic here.
 *
 * <p>S2-A-3 CONTRACT UPDATE: confirmEmailVerification and confirmPasswordRecovery have been removed
 * from the API contract. Keycloak handles email verification and password reset natively on its own
 * pages. No backend token-confirm endpoints are needed. See architecture decision #1574.
 *
 * <p>S2-B-5: registerStudent delegates to {@link RegisterStudentUseCase}. The controller maps
 * the HTTP request to a {@link RegisterStudentCommand} and maps the result back to
 * {@link RegistrationAccepted} (202 Accepted). ARQ-001: no domain types are referenced directly —
 * only application DTOs and generated model types.
 *
 * <p>Satisfies: S1-B-5 (G2 resolve), Design §2.1 AuthApi shim, S2-A-3 contract audit, S2-B-5.
 */
@RestController
class AuthController implements AuthApi {

  private final RegisterStudentUseCase registerStudentUseCase;

  AuthController(RegisterStudentUseCase registerStudentUseCase) {
    this.registerStudentUseCase = registerStudentUseCase;
  }

  /** Shim — Spring Security handles POST /auth/logout before DispatcherServlet. */
  @Override
  public ResponseEntity<Void> logout(String xXsrfToken) {
    // Spring Security's logout filter always processes /auth/logout first.
    // If this method is ever reached, the session was already invalidated by Spring.
    return ResponseEntity.noContent().build();
  }

  /**
   * S2-B-5: Self-registration for students.
   *
   * <p>Maps the HTTP request fields to a {@link RegisterStudentCommand}. The termsVersion and
   * privacyVersion are NOT sourced from the request body — the backend reads them from
   * configuration (G6 RESOLVED). The request carries only {@code acceptedTerms=true} and
   * {@code acceptedDataProcessing=true} booleans as acknowledgements.
   *
   * <p>Returns 202 Accepted with a {@link RegistrationAccepted} body. Exceptions are mapped by
   * {@code IdentityExceptionHandler}: DomainNotAllowedException → 422,
   * EmailAlreadyRegisteredException → 409.
   */
  @Override
  public ResponseEntity<RegistrationAccepted> registerStudent(
      UUID idempotencyKey, StudentRegistrationRequest body) {
    // Terms/privacy versions come from application config, NOT from the request body.
    // The command carries empty string placeholders — RegisterStudentService overrides them with
    // the configured current versions before persisting consent evidence (G6 RESOLVED, obs #1574).
    // OPTION A (DT-011): no password field on the registration contract.
    // Keycloak sends UPDATE_PASSWORD via execute-actions-email so the user sets their credential
    // on Keycloak's own page. The backend never receives or stores a password.
    var command =
        new RegisterStudentCommand(
            body.getEmail(),
            body.getFullName(),
            // Versions sourced from config in RegisterStudentService; placeholders here.
            "",
            "");

    RegisterStudentResult result = registerStudentUseCase.register(command);

    // Map onboardingState string to AccountStatus enum (ARQ-001: no domain type imported directly).
    // RegisterStudentResult.onboardingState() is a String (e.g. "PENDING_VERIFICATION", "ACTIVE").
    // AccountStatus.fromValue() performs safe lookup and throws IllegalArgumentException on mismatch.
    AccountStatus accountStatus = AccountStatus.fromValue(result.onboardingState());
    var response = new RegistrationAccepted(result.userId(), accountStatus);

    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
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
