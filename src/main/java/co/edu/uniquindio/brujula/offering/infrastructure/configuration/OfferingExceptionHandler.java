package co.edu.uniquindio.brujula.offering.infrastructure.configuration;

import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.PeriodNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps offering-module domain exceptions to HTTP problem responses (RFC 9457).
 *
 * <p>Lives in {@code infrastructure.configuration} to reference both application and domain layers.
 *
 * <p>Exception → HTTP mapping:
 * <ul>
 *   <li>{@link PeriodNotFoundException} → 404 Not Found
 *   <li>{@link OfferingNotFoundException} → 404 Not Found
 *   <li>{@link ScheduleOptionNotFoundException} → 404 Not Found
 * </ul>
 *
 * <p>Satisfies: OFF-001, OFF-002, OFF-003.
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
}
