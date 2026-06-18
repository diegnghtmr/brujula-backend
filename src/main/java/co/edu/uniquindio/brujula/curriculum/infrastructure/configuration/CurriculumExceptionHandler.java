package co.edu.uniquindio.brujula.curriculum.infrastructure.configuration;

import co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteCycleException;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps curriculum domain exceptions to HTTP problem responses (RFC 9457).
 *
 * <p>Lives in infrastructure.configuration (not adapter.in.rest) so that it may reference domain
 * exception types while still being picked up by Spring's {@code @ControllerAdvice} scan.
 * Infrastructure is permitted to depend on domain.
 *
 * <p>Scoped to the curriculum REST package with {@link Ordered#HIGHEST_PRECEDENCE} to prevent
 * cross-module handler hijacking for shared exceptions like {@link
 * IdempotencyKeyConflictException}.
 *
 * <p>Exception → HTTP mapping:
 *
 * <ul>
 *   <li>{@link CourseNotFoundException} → 404 Not Found (CUR-001)
 *   <li>{@link CourseRequirementNotFoundException} → 404 Not Found (CUR-003)
 *   <li>{@link PrerequisiteCycleException} → 422 Unprocessable Entity (CUR-003)
 *   <li>{@link OptimisticLockingFailureException} → 409 Conflict (CUR-002 / CUR-003)
 *   <li>{@link IdempotencyKeyConflictException} → 409 Conflict (CUR-001 / CUR-003)
 *   <li>{@link DataIntegrityViolationException} → 409 Conflict
 * </ul>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(
    basePackages = "co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.in.rest")
class CurriculumExceptionHandler {

  @ExceptionHandler(CourseNotFoundException.class)
  ProblemDetail handleCourseNotFound(CourseNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(CourseRequirementNotFoundException.class)
  ProblemDetail handleCourseRequirementNotFound(CourseRequirementNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(PrerequisiteCycleException.class)
  ProblemDetail handlePrerequisiteCycle(PrerequisiteCycleException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    problem.setProperty("code", "PREREQUISITE_CYCLE");
    return problem;
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
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
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "Data integrity constraint violated.");
    problem.setProperty("code", "DATA_INTEGRITY_VIOLATION");
    return problem;
  }
}
