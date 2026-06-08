package co.edu.uniquindio.brujula.simulation.infrastructure.configuration;

import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps simulation-module exceptions to HTTP problem responses (RFC 9457).
 *
 * <p>Exception → HTTP mapping:
 *
 * <ul>
 *   <li>{@link SimulationNotFoundException} → 404 Not Found (also BOLA enumeration-safe)
 *   <li>{@link SimulationAccessDeniedException} → 403 Forbidden (X-003 spec WINS over design 404)
 *   <li>{@link IdempotencyKeyConflictException} → 409 Conflict (X-001: key reuse with diff body)
 *   <li>{@link OptimisticLockingFailureException} → 409 Conflict (X-002: stale version)
 * </ul>
 *
 * <p>NOTE: {@link SimulationAccessDeniedException} maps to 403 per spec X-003
 * ("non-owner gets 403"). This deviates from the design's 404 suggestion (which would have been
 * BOLA-safer). The spec/contract wins over the design note.
 */
@RestControllerAdvice
class SimulationExceptionHandler {

  @ExceptionHandler(SimulationNotFoundException.class)
  ProblemDetail handleSimulationNotFound(SimulationNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(SimulationAccessDeniedException.class)
  ProblemDetail handleSimulationAccessDenied(SimulationAccessDeniedException ex) {
    // X-003: non-owner gets 403 (spec wins over design's 404 suggestion)
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  ProblemDetail handleIdempotencyConflict(IdempotencyKeyConflictException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "Version conflict — reload and retry.");
  }
}
