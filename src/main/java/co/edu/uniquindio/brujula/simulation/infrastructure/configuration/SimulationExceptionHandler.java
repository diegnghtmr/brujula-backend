package co.edu.uniquindio.brujula.simulation.infrastructure.configuration;

import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationStillBlockedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationValidationBlockedException;
import co.edu.uniquindio.brujula.simulation.domain.ValidationNotice;
import java.net.URI;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
 *   <li>{@link SimulationNotFoundException} → 404 Not Found
 *   <li>{@link SimulationSelectionNotFoundException} → 404 Not Found
 *   <li>{@link SimulationAccessDeniedException} → 403 Forbidden (X-003 spec WINS over design 404)
 *   <li>{@link SimulationValidationBlockedException} → 409 Conflict (VAL-001: blocking validation)
 *       — renders {@code application/problem+json} with {@code code}, {@code title}, {@code detail},
 *       and {@code metadata} matching the spec VAL-002..005 schema.
 *   <li>{@link IdempotencyKeyConflictException} → 409 Conflict (X-001: key reuse with diff body)
 *   <li>{@link OptimisticLockingFailureException} → 409 Conflict (X-002: stale version)
 * </ul>
 *
 * <p>QAL-ADV-001 (WU1): scoped to the simulation REST adapter package so that shared exceptions
 * (e.g. {@link IdempotencyKeyConflictException}) handled by multiple module advices are always
 * resolved by the owning module's handler. Controllers are package-private; {@code basePackages}
 * string is used instead of {@code assignableTypes}. Template from {@code ImportsExceptionHandler}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "co.edu.uniquindio.brujula.simulation.infrastructure.adapter.in.rest")
class SimulationExceptionHandler {

  @ExceptionHandler(SimulationNotFoundException.class)
  ProblemDetail handleSimulationNotFound(SimulationNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(SimulationSelectionNotFoundException.class)
  ProblemDetail handleSelectionNotFound(SimulationSelectionNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(SimulationAccessDeniedException.class)
  ProblemDetail handleSimulationAccessDenied(SimulationAccessDeniedException ex) {
    // X-003: non-owner gets 403 (spec wins over design's 404 suggestion)
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(SimulationValidationBlockedException.class)
  ProblemDetail handleValidationBlocked(SimulationValidationBlockedException ex) {
    // VAL-001: blocking validation → 409 application/problem+json
    // Shape mirrors Fase-3 ProgressExceptionHandler problem+json pattern.
    ValidationNotice primary = ex.primaryNotice();
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setType(URI.create("https://api.brujula.edu.co/errors/simulation-validation-blocked"));
    problem.setTitle("Simulation validation blocked");
    problem.setDetail(primary.message());
    problem.setProperty("code", primary.code());
    if (primary.metadata() != null && !primary.metadata().isEmpty()) {
      problem.setProperty("metadata", primary.metadata());
    }
    if (ex.getBlocking().size() > 1) {
      // Surface all blocking codes when multiple rules fired simultaneously
      problem.setProperty("allBlockingCodes",
          ex.getBlocking().stream().map(ValidationNotice::code).toList());
    }
    return problem;
  }

  @ExceptionHandler(SimulationStillBlockedException.class)
  ProblemDetail handleSimulationStillBlocked(SimulationStillBlockedException ex) {
    // STALE-002: review failed due to persistent blockers → 409 problem+json
    ValidationNotice primary = ex.primaryNotice();
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setType(URI.create("https://api.brujula.edu.co/errors/simulation-still-blocked"));
    problem.setTitle("Simulation still blocked after review");
    problem.setDetail(primary.message());
    problem.setProperty("code", primary.code());
    if (primary.metadata() != null && !primary.metadata().isEmpty()) {
      problem.setProperty("metadata", primary.metadata());
    }
    if (ex.getBlocking().size() > 1) {
      problem.setProperty("allBlockingCodes",
          ex.getBlocking().stream().map(ValidationNotice::code).toList());
    }
    return problem;
  }

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  ProblemDetail handleIdempotencyConflict(IdempotencyKeyConflictException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setProperty("code", "IDEMPOTENCY_KEY_CONFLICT");
    return problem;
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "Version conflict — reload and retry.");
    problem.setProperty("code", "OPTIMISTIC_LOCK_CONFLICT");
    return problem;
  }
}
