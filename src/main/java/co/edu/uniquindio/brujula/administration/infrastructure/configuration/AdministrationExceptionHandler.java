package co.edu.uniquindio.brujula.administration.infrastructure.configuration;

import co.edu.uniquindio.brujula.administration.application.service.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.administration.application.service.StaffNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps administration-module application exceptions to HTTP problem responses (RFC 9457).
 *
 * <p>Lives in {@code infrastructure.configuration} (not {@code adapter.in.rest}) so that it may
 * reference application exception types without violating ARQ-001. Infrastructure may depend on
 * application layer (ARQ-008).
 *
 * <ul>
 *   <li>{@link StaffNotFoundException} → 404 Not Found
 *   <li>{@link IdempotencyKeyConflictException} → 409 Conflict
 * </ul>
 */
@RestControllerAdvice
class AdministrationExceptionHandler {

  @ExceptionHandler(StaffNotFoundException.class)
  ProblemDetail handleStaffNotFound(StaffNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  ProblemDetail handleIdempotencyConflict(IdempotencyKeyConflictException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }
}
