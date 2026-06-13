package co.edu.uniquindio.brujula.progress.infrastructure.configuration;

import co.edu.uniquindio.brujula.progress.domain.AttemptNotFoundException;
import co.edu.uniquindio.brujula.progress.domain.DuplicateProgramSubscriptionException;
import co.edu.uniquindio.brujula.progress.domain.GradeStatusMismatchException;
import co.edu.uniquindio.brujula.progress.domain.HistoryImpactNotAcknowledgedException;
import co.edu.uniquindio.brujula.progress.domain.InactivePlanException;
import co.edu.uniquindio.brujula.progress.domain.LastActiveSubscriptionException;
import co.edu.uniquindio.brujula.progress.domain.MaxSubscriptionsExceededException;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps progress-module application and domain exceptions to HTTP problem responses (RFC 9457).
 *
 * <p>Lives in {@code infrastructure.configuration} (not {@code adapter.in.rest}) so that it may
 * reference both application-layer and domain exceptions. Infrastructure may depend on both
 * application and domain layers.
 *
 * <p>Exception → HTTP mapping:
 *
 * <ul>
 *   <li>{@link SubscriptionNotFoundException} → 404 Not Found (BOLA-safe: also used for ownership
 *       violations to avoid resource enumeration).
 *   <li>{@link AttemptNotFoundException} → 404 Not Found (BOLA-safe for course attempt ownership).
 *   <li>{@link GradeStatusMismatchException} → 422 Unprocessable Entity (grade-status coherence).
 *   <li>{@link MaxSubscriptionsExceededException} → 409 Conflict (cap=2).
 *   <li>{@link DuplicateProgramSubscriptionException} → 409 Conflict.
 *   <li>{@link HistoryImpactNotAcknowledgedException} → 422 Unprocessable Entity (missing
 *       acknowledgment on plan change with existing attempts).
 *   <li>{@link LastActiveSubscriptionException} → 409 Conflict (must retain one subscription).
 *   <li>{@link
 *       co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException}
 *       → 409 Conflict (key reuse with different body).
 *   <li>{@link OptimisticLockingFailureException} → 409 Conflict (version conflict).
 * </ul>
 *
 * <p>Scoped to the progress REST adapter package so that shared exceptions (e.g. {@link
 * co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException})
 * handled by multiple module advices are always resolved by the owning module's handler and never
 * intercepted by a foreign module advice. Controllers are package-private, so {@code basePackages}
 * (string) is used instead of {@code assignableTypes} (which does not compile).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(
    basePackages = "co.edu.uniquindio.brujula.progress.infrastructure.adapter.in.rest")
class ProgressExceptionHandler {

  @ExceptionHandler(SubscriptionNotFoundException.class)
  ProblemDetail handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(AttemptNotFoundException.class)
  ProblemDetail handleAttemptNotFound(AttemptNotFoundException ex) {
    // BOLA-safe: also surfaces when the caller does not own the attempt.
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(GradeStatusMismatchException.class)
  ProblemDetail handleGradeStatusMismatch(GradeStatusMismatchException ex) {
    // Grade-status coherence failure: PASSED requires grade ≥ 3.0,
    // FAILED requires grade < 3.0, CANCELLED must not have a grade.
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  @ExceptionHandler(MaxSubscriptionsExceededException.class)
  ProblemDetail handleMaxSubscriptionsExceeded(MaxSubscriptionsExceededException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(DuplicateProgramSubscriptionException.class)
  ProblemDetail handleDuplicateProgram(DuplicateProgramSubscriptionException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(HistoryImpactNotAcknowledgedException.class)
  ProblemDetail handleHistoryImpactNotAcknowledged(HistoryImpactNotAcknowledgedException ex) {
    // 422 Unprocessable Entity, not 409: 409 is reserved for version (optimistic-lock) conflicts;
    // 422 signals a semantic validation failure (missing acknowledgment field).
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  @ExceptionHandler(InactivePlanException.class)
  ProblemDetail handleInactivePlan(InactivePlanException ex) {
    // 422 for ARCHIVED/non-ACTIVE plan — same semantic: unprocessable due to plan state.
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  @ExceptionHandler(LastActiveSubscriptionException.class)
  ProblemDetail handleLastActiveSubscription(LastActiveSubscriptionException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  ProblemDetail handleIdempotencyConflict(IdempotencyKeyConflictException ex) {
    // Sharedkernel IdempotencyKeyConflictException consolidates progress and administration
    // exceptions.
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setProperty("code", "IDEMPOTENCY_KEY_CONFLICT");
    return problem;
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "Version conflict — reload and retry.");
    problem.setProperty("code", "OPTIMISTIC_LOCK_CONFLICT");
    return problem;
  }
}
