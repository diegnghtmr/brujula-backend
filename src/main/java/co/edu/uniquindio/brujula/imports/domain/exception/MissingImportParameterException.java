package co.edu.uniquindio.brujula.imports.domain.exception;

/**
 * Thrown when a required parameter is missing for the requested import kind.
 *
 * <p>For example: kind=COURSES without planId, or kind=OFFERINGS without periodId. Maps to HTTP 400
 * Bad Request at the controller layer (WU4). Covers: SC-001-G, SC-001-H.
 */
public class MissingImportParameterException extends RuntimeException {

  private final String parameterName;

  public MissingImportParameterException(String parameterName, String reason) {
    super("Missing required parameter '%s': %s".formatted(parameterName, reason));
    this.parameterName = parameterName;
  }

  public String getParameterName() {
    return parameterName;
  }
}
