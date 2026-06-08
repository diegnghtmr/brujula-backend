package co.edu.uniquindio.brujula.offering.infrastructure.configuration;

import co.edu.uniquindio.brujula.offering.domain.IllegalOfferingTransitionException;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotPublishableException;
import co.edu.uniquindio.brujula.offering.domain.PeriodNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionNotFoundException;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps offering-module domain exceptions to HTTP problem responses (RFC 9457).
 *
 * <p>Lives in {@code infrastructure.configuration} to reference both application and domain layers.
 *
 * <p>Exception → HTTP mapping (OFFW-008):
 * <ul>
 *   <li>{@link PeriodNotFoundException} → 404 Not Found
 *   <li>{@link OfferingNotFoundException} → 404 Not Found
 *   <li>{@link ScheduleOptionNotFoundException} → 404 Not Found
 *   <li>{@link IllegalOfferingTransitionException} → 409 Conflict
 *   <li>{@link OfferingNotPublishableException} → 422 Unprocessable Entity
 *   <li>{@link OptimisticLockingFailureException} → 409 Conflict (OPTIMISTIC_LOCK_CONFLICT)
 *   <li>{@link IdempotencyKeyConflictException} → 409 Conflict
 *   <li>{@link DataIntegrityViolationException} → 409 Conflict
 * </ul>
 *
 * <p>Satisfies: OFF-001, OFF-002, OFF-003, OFFW-008.
 */
@RestControllerAdvice
class OfferingExceptionHandler {

  @ExceptionHandler(PeriodNotFoundException.class)
  ProblemDetail handlePeriodNotFound(PeriodNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(OfferingNotFoundException.class)
  ProblemDetail handleOfferingNotFound(OfferingNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ScheduleOptionNotFoundException.class)
  ProblemDetail handleScheduleOptionNotFound(ScheduleOptionNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(IllegalOfferingTransitionException.class)
  ProblemDetail handleIllegalOfferingTransition(IllegalOfferingTransitionException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setProperty("code", "ILLEGAL_OFFERING_TRANSITION");
    return problem;
  }

  @ExceptionHandler(OfferingNotPublishableException.class)
  ProblemDetail handleOfferingNotPublishable(OfferingNotPublishableException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "Version conflict: the resource was modified by another request.");
    problem.setProperty("code", "OPTIMISTIC_LOCK_CONFLICT");
    return problem;
  }

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  ProblemDetail handleIdempotencyConflict(IdempotencyKeyConflictException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setProperty("code", "IDEMPOTENCY_KEY_CONFLICT");
    return problem;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "Data integrity constraint violated.");
    problem.setProperty("code", "DATA_INTEGRITY_VIOLATION");
    return problem;
  }
}
