package co.edu.uniquindio.brujula.administration.application.service;

import java.util.UUID;

/** Thrown when a staff account is not found by id. */
public class StaffNotFoundException extends RuntimeException {

  private final UUID staffId;

  public StaffNotFoundException(UUID staffId) {
    super("Staff account not found: " + staffId);
    this.staffId = staffId;
  }

  public UUID getStaffId() {
    return staffId;
  }
}
