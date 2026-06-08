package co.edu.uniquindio.brujula.administration.infrastructure.configuration;

import co.edu.uniquindio.brujula.administration.application.service.StaffNotFoundException;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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
 *   <li>{@link OptimisticLockingFailureException} → 409 Conflict (OFFW-002 period update)
 *   <li>{@link DataIntegrityViolationException} → 409 Conflict (period name duplicate,
 *       unique constraint {@code academic_period_name_uq})
 * </ul>
 *
 * <p>Exception visibility note (ADR-0 task 6.3): {@code PeriodNotFoundException} (offering domain)
 * is handled by {@code OfferingExceptionHandler} (global {@code @RestControllerAdvice}) which
 * already maps it to 404 — no duplication needed here. Spring/DAL exceptions
 * ({@link OptimisticLockingFailureException}, {@link DataIntegrityViolationException}) are
 * framework types already visible in the infrastructure layer of any module.
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

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    pd.setTitle("Version Conflict");
    pd.setProperty("code", "OPTIMISTIC_LOCK_CONFLICT");
    return pd;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
        "A resource with the same unique identifier already exists");
    pd.setTitle("Conflict");
    return pd;
  }
}
