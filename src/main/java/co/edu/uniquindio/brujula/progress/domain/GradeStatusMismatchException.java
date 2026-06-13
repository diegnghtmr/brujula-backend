package co.edu.uniquindio.brujula.progress.domain;

/**
 * Thrown when the combination of {@link AttemptStatus} and {@link Grade} violates the
 * grade-coherence rule:
 *
 * <ul>
 *   <li>PASSED requires a non-null grade in [3.0, 5.0].
 *   <li>FAILED requires a non-null grade in [0.0, 2.9].
 *   <li>CANCELLED requires a null grade.
 * </ul>
 *
 * <p>Maps to HTTP 422 Unprocessable Entity.
 */
public class GradeStatusMismatchException extends RuntimeException {

  private final AttemptStatus status;

  public GradeStatusMismatchException(AttemptStatus status, String detail) {
    super("Grade-status mismatch for status " + status + ": " + detail);
    this.status = status;
  }

  public AttemptStatus getStatus() {
    return status;
  }
}
