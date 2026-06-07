package co.edu.uniquindio.brujula.programcatalog.infrastructure.configuration;

import co.edu.uniquindio.brujula.programcatalog.domain.ProgramNotFoundException;
import co.edu.uniquindio.brujula.programcatalog.domain.StudyPlanNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps programcatalog domain exceptions to HTTP problem responses (RFC 9457).
 *
 * <p>Lives in infrastructure.configuration (not adapter.in.rest) so that it may reference
 * domain exception types without violating ARQ-001, while still being picked up by Spring's
 * {@code @ControllerAdvice} scan. Infrastructure is permitted to depend on domain (ARQ-008).
 *
 * <p>Handled exceptions:
 * <ul>
 *   <li>{@link ProgramNotFoundException} → 404 Not Found (CAT-001)
 *   <li>{@link StudyPlanNotFoundException} → 404 Not Found (CAT-002)
 * </ul>
 */
@RestControllerAdvice
class ProgramCatalogExceptionHandler {

  @ExceptionHandler(ProgramNotFoundException.class)
  ProblemDetail handleProgramNotFound(ProgramNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(StudyPlanNotFoundException.class)
  ProblemDetail handleStudyPlanNotFound(StudyPlanNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }
}
