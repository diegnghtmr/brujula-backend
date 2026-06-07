package co.edu.uniquindio.brujula.curriculum.infrastructure.configuration;

import co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps curriculum domain exceptions to HTTP problem responses (RFC 9457).
 *
 * <p>Lives in infrastructure.configuration (not adapter.in.rest) so that it may reference
 * domain exception types without violating ARQ-001, while still being picked up by Spring's
 * {@code @ControllerAdvice} scan. Infrastructure is permitted to depend on domain (ARQ-008).
 *
 * <p>Handled exceptions:
 * <ul>
 *   <li>{@link CourseNotFoundException} → 404 Not Found (CUR-001)
 * </ul>
 */
@RestControllerAdvice
class CurriculumExceptionHandler {

  @ExceptionHandler(CourseNotFoundException.class)
  ProblemDetail handleCourseNotFound(CourseNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }
}
