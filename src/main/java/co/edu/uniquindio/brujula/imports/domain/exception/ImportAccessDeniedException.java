package co.edu.uniquindio.brujula.imports.domain.exception;

import java.util.UUID;

/**
 * Thrown when a STAFF actor attempts to access an import operation for a program they are not
 * assigned to, or when the actor's StaffAccount is INACTIVE.
 *
 * <p>Maps to HTTP 403 Forbidden at the controller layer (WU4). Covers: SC-001-D, INV-06, IMP-010.
 */
public class ImportAccessDeniedException extends RuntimeException {

  private final String reason;

  public ImportAccessDeniedException(String reason) {
    super("Import access denied: " + reason);
    this.reason = reason;
  }

  /** Factory: actor email is not associated with an ACTIVE StaffAccount. */
  public static ImportAccessDeniedException inactiveOrUnknownActor(String email) {
    return new ImportAccessDeniedException(
        "No active StaffAccount found for email '%s'".formatted(email));
  }

  /** Factory: staff is not assigned to the target program. */
  public static ImportAccessDeniedException notAssignedToProgram(UUID programId) {
    return new ImportAccessDeniedException(
        "Staff is not assigned to program '%s'".formatted(programId));
  }

  public String getReason() {
    return reason;
  }
}
