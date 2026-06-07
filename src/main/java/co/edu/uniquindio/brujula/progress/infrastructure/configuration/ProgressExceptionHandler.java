package co.edu.uniquindio.brujula.progress.infrastructure.configuration;

import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.progress.domain.DuplicateProgramSubscriptionException;
import co.edu.uniquindio.brujula.progress.domain.HistoryImpactNotAcknowledgedException;
import co.edu.uniquindio.brujula.progress.domain.InactivePlanException;
import co.edu.uniquindio.brujula.progress.domain.LastActiveSubscriptionException;
import co.edu.uniquindio.brujula.progress.domain.MaxSubscriptionsExceededException;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps progress-module application and domain exceptions to HTTP problem responses (RFC 9457).
 *
 * <p>Lives in {@code infrastructure.configuration} (not {@code adapter.in.rest}) so that it may
 * reference both application-layer and domain exceptions without violating ARQ-001. Infrastructure
 * may depend on both application and domain layers.
 *
 * <p>Exception → HTTP mapping:
 *
 * <ul>
 *   <li>{@link SubscriptionNotFoundException} → 404 Not Found (BOLA-safe: also used for
 *       ownership violations to avoid resource enumeration).
 *   <li>{@link MaxSubscriptionsExceededException} → 409 Conflict (cap=2).
 *   <li>{@link DuplicateProgramSubscriptionException} → 409 Conflict.
 *   <li>{@link HistoryImpactNotAcknowledgedException} → 422 Unprocessable Entity (spec SUB-3-PLAN-NO-ACK).
 *   <li>{@link LastActiveSubscriptionException} → 409 Conflict (must retain one subscription).
 *   <li>{@link co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException} → 409 Conflict (key reuse with different body).
 *   <li>{@link OptimisticLockingFailureException} → 409 Conflict (version conflict, CROSS-3).
 * </ul>
 *
 * <p>Satisfies: SUB-1..4, CROSS-3. ADR-F3. FIX-W2: 422 for history-impact ack (spec SUB-3-PLAN-NO-ACK).
 */
@RestControllerAdvice
class ProgressExceptionHandler {

  @ExceptionHandler(SubscriptionNotFoundException.class)
  ProblemDetail handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
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
    // FIX-W2: spec SUB-3-PLAN-NO-ACK requires 422 Unprocessable Entity, not 409.
    // 409 is reserved for version (optimistic-lock) conflicts; 422 signals a semantic validation
    // failure (missing acknowledgment field) which is distinct from a version collision.
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  @ExceptionHandler(InactivePlanException.class)
  ProblemDetail handleInactivePlan(InactivePlanException ex) {
    // FIX-W3: spec SUB-1-INACTIVE-PLAN requires 422 for ARCHIVED/non-ACTIVE plan.
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  @ExceptionHandler(LastActiveSubscriptionException.class)
  ProblemDetail handleLastActiveSubscription(LastActiveSubscriptionException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  ProblemDetail handleIdempotencyConflict(IdempotencyKeyConflictException ex) {
    // FIX-S1: sharedkernel IdempotencyKeyConflictException consolidates progress and administration exceptions.
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "Version conflict — reload and retry.");
  }
}
