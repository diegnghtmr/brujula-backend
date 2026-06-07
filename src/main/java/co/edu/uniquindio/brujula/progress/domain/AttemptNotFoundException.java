package co.edu.uniquindio.brujula.progress.domain;

import java.util.UUID;

/**
 * Thrown when a requested {@link CourseAttempt} does not exist or does not belong to the
 * authenticated user (BOLA ownership guard).
 *
 * <p>Maps to HTTP 404 Not Found (PRG-2-BOLA, ownership not revealed).
 */
public class AttemptNotFoundException extends RuntimeException {

  private final UUID attemptId;

  public AttemptNotFoundException(UUID attemptId) {
    super("Course attempt not found: " + attemptId);
    this.attemptId = attemptId;
  }

  public UUID getAttemptId() {
    return attemptId;
  }
}
