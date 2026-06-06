package co.edu.uniquindio.brujula.identity.infrastructure.configuration;

import co.edu.uniquindio.brujula.identity.application.EmailAlreadyRegisteredException;
import co.edu.uniquindio.brujula.identity.domain.DomainNotAllowedException;
import co.edu.uniquindio.brujula.identity.domain.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps identity-module domain exceptions to HTTP problem responses (RFC 9457).
 *
 * <p>Lives in the infrastructure.configuration package (not adapter.in.rest) so that it may
 * reference domain exception types without violating ARQ-001, while still being picked up by
 * Spring's {@code @ControllerAdvice} scan. Infrastructure is permitted to depend on domain
 * (ARQ-008).
 *
 * <p>Handles exceptions thrown by identity REST adapters and converts them to appropriate HTTP
 * status codes:
 *
 * <ul>
 *   <li>{@link UserNotFoundException} → 404 Not Found
 *   <li>{@link DomainNotAllowedException} → 422 Unprocessable Entity
 *   <li>{@link EmailAlreadyRegisteredException} → 409 Conflict (S2-B-3)
 * </ul>
 */
@RestControllerAdvice
class IdentityExceptionHandler {

  @ExceptionHandler(UserNotFoundException.class)
  ProblemDetail handleUserNotFound(UserNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(DomainNotAllowedException.class)
  ProblemDetail handleDomainNotAllowed(DomainNotAllowedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  /** S2-B-3: email already registered → 409 Conflict per OpenAPI contract. */
  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }
}
