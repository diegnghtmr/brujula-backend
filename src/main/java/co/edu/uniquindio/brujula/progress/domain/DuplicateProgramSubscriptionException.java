package co.edu.uniquindio.brujula.progress.domain;

import java.util.UUID;

/**
 * Thrown when a student attempts to subscribe to a program they are already subscribed to.
 *
 * <p>Maps to HTTP 409 Conflict (SUB-1-DUP scenario).
 */
public class DuplicateProgramSubscriptionException extends RuntimeException {

  private final UUID userId;
  private final UUID programId;

  public DuplicateProgramSubscriptionException(UUID userId, UUID programId) {
    super("Student %s is already subscribed to program %s.".formatted(userId, programId));
    this.userId = userId;
    this.programId = programId;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getProgramId() {
    return programId;
  }
}
