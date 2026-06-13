package co.edu.uniquindio.brujula.imports.infrastructure.configuration;

import co.edu.uniquindio.brujula.imports.domain.exception.FileSizeExceededException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportAccessDeniedException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotConfirmableException;
import co.edu.uniquindio.brujula.imports.domain.exception.ImportBatchNotFoundException;
import co.edu.uniquindio.brujula.imports.domain.exception.MissingImportParameterException;
import co.edu.uniquindio.brujula.imports.domain.exception.SchemaVersionMismatchException;
import co.edu.uniquindio.brujula.imports.domain.exception.UnsupportedFileTypeException;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Maps imports-module domain exceptions to RFC 9457 {@code application/problem+json} responses.
 *
 * <p>Lives in {@code infrastructure.configuration} (same pattern as OfferingExceptionHandler) so
 * that it may reference domain exception types. Infrastructure may depend on domain.
 *
 * <p>Exception → HTTP mapping:
 *
 * <ul>
 *   <li>{@link UnsupportedFileTypeException} → 415 UNSUPPORTED_FILE_TYPE
 *   <li>{@link FileSizeExceededException} → 413 FILE_TOO_LARGE
 *   <li>{@link MaxUploadSizeExceededException} → 413 FILE_TOO_LARGE (container-level guard)
 *   <li>{@link ImportAccessDeniedException} → 403 IMPORT_ACCESS_DENIED
 *   <li>{@link MissingImportParameterException} → 400 MISSING_PARAMETER
 *   <li>{@link IdempotencyKeyConflictException} → 409 IDEMPOTENCY_KEY_CONFLICT
 *   <li>{@link ImportBatchNotConfirmableException} → 409 IMPORT_NOT_CONFIRMABLE
 *   <li>{@link ImportBatchNotFoundException} → 404 IMPORT_NOT_FOUND
 *   <li>{@link SchemaVersionMismatchException} → 409 SCHEMA_VERSION_MISMATCH
 * </ul>
 *
 * <p>The {@code code} property is set on the ProblemDetail so it appears at the top-level {@code
 * $.code} field (matching OfferingExceptionHandler convention).
 *
 * <p>Scoped to the imports REST package with {@link Ordered#HIGHEST_PRECEDENCE}. The shared {@link
 * IdempotencyKeyConflictException} (sharedkernel) is handled by several module advices, all
 * registered as unscoped global {@code @RestControllerAdvice}. Without scoping + precedence here,
 * another module's advice (resolved by bean order) could intercept this controller's idempotency
 * conflict and emit a response without the expected {@code $.code}. Scoping this advice to its own
 * controller package and giving it top precedence guarantees imports requests are mapped by this
 * handler without altering any other module. (The broader pre-existing issue of overlapping global
 * advices for shared exceptions is tracked as separate technical debt.)
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(
    basePackages = "co.edu.uniquindio.brujula.imports.infrastructure.adapter.in.rest")
class ImportsExceptionHandler {

  // -------------------------------------------------------------------------
  // 400 Bad Request
  // -------------------------------------------------------------------------

  @ExceptionHandler(MissingImportParameterException.class)
  ProblemDetail handleMissingParameter(MissingImportParameterException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setProperty("code", "MISSING_PARAMETER");
    return problem;
  }

  // -------------------------------------------------------------------------
  // 403 Forbidden
  // -------------------------------------------------------------------------

  @ExceptionHandler(ImportAccessDeniedException.class)
  ProblemDetail handleAccessDenied(ImportAccessDeniedException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    problem.setProperty("code", "IMPORT_ACCESS_DENIED");
    return problem;
  }

  // -------------------------------------------------------------------------
  // 404 Not Found
  // -------------------------------------------------------------------------

  @ExceptionHandler(ImportBatchNotFoundException.class)
  ProblemDetail handleImportNotFound(ImportBatchNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setProperty("code", "IMPORT_NOT_FOUND");
    return problem;
  }

  // -------------------------------------------------------------------------
  // 409 Conflict
  // -------------------------------------------------------------------------

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  ProblemDetail handleIdempotencyConflict(IdempotencyKeyConflictException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setProperty("code", "IDEMPOTENCY_KEY_CONFLICT");
    return problem;
  }

  @ExceptionHandler(ImportBatchNotConfirmableException.class)
  ProblemDetail handleImportNotConfirmable(ImportBatchNotConfirmableException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setProperty("code", "IMPORT_NOT_CONFIRMABLE");
    return problem;
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setProperty("code", "OPTIMISTIC_LOCK_CONFLICT");
    return problem;
  }

  @ExceptionHandler(SchemaVersionMismatchException.class)
  ProblemDetail handleSchemaVersionMismatch(SchemaVersionMismatchException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setProperty("code", SchemaVersionMismatchException.ERROR_CODE);
    return problem;
  }

  // -------------------------------------------------------------------------
  // 413 Content Too Large — both container-level and app-level paths
  // -------------------------------------------------------------------------

  /**
   * Application-level file size guard (thrown by UploadImportService when the parsed bytes exceed
   * the configured limit).
   */
  @ExceptionHandler(FileSizeExceededException.class)
  ProblemDetail handleFileSizeExceeded(FileSizeExceededException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
    problem.setProperty("code", "FILE_TOO_LARGE");
    return problem;
  }

  /**
   * Container-level multipart size guard (thrown by Spring's multipart resolver BEFORE the
   * controller method is entered when {@code spring.servlet.multipart.max-request-size} is
   * exceeded). Must be mapped here so oversize files ALWAYS return 413.
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ProblemDetail handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds the configured maximum upload size.");
    problem.setProperty("code", "FILE_TOO_LARGE");
    return problem;
  }

  // -------------------------------------------------------------------------
  // 415 Unsupported Media Type
  // -------------------------------------------------------------------------

  @ExceptionHandler(UnsupportedFileTypeException.class)
  ProblemDetail handleUnsupportedFileType(UnsupportedFileTypeException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    problem.setProperty("code", "UNSUPPORTED_FILE_TYPE");
    return problem;
  }

  // -------------------------------------------------------------------------
  // 501 Not Implemented — defensive guard
  // -------------------------------------------------------------------------

  /**
   * Maps {@link UnsupportedOperationException} to HTTP 501 Not Implemented.
   *
   * <p>Kept as a defensive handler. No supported import kind (COURSES, REQUIREMENTS, OFFERINGS)
   * throws this exception from {@code ConfirmImportService.replayRows()}. If a future kind is added
   * and not yet implemented, this handler prevents leaking a raw 500 stack trace at the API
   * boundary.
   */
  @ExceptionHandler(UnsupportedOperationException.class)
  ProblemDetail handleNotImplemented(UnsupportedOperationException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, ex.getMessage());
    problem.setProperty("code", "NOT_IMPLEMENTED");
    return problem;
  }
}
