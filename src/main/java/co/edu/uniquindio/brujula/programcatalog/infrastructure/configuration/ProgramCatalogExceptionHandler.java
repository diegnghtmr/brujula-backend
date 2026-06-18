package co.edu.uniquindio.brujula.programcatalog.infrastructure.configuration;

import co.edu.uniquindio.brujula.programcatalog.domain.ProgramNotFoundException;
import co.edu.uniquindio.brujula.programcatalog.domain.StudyPlanNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps programcatalog domain exceptions to HTTP problem responses (RFC 9457).
 *
 * <p>Lives in infrastructure.configuration (not adapter.in.rest) so that it may reference domain
 * exception types while still being picked up by Spring's {@code @ControllerAdvice} scan.
 * Infrastructure is permitted to depend on domain.
 *
 * <p>Handled exceptions:
 *
 * <ul>
 *   <li>{@link ProgramNotFoundException} → 404 Not Found
 *   <li>{@link StudyPlanNotFoundException} → 404 Not Found
 * </ul>
 *
 * <p>Scoped to the programcatalog REST adapter package so that domain exceptions are always
 * resolved by this handler and cannot be intercepted by foreign module advices. Controllers are
 * package-private; {@code basePackages} string is used instead of {@code assignableTypes}. Template
 * from {@code ImportsExceptionHandler}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(
    basePackages = "co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.in.rest")
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
