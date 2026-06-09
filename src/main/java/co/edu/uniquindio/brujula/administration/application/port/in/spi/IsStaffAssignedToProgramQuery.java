package co.edu.uniquindio.brujula.administration.application.port.in.spi;

import java.util.Set;
import java.util.UUID;

/**
 * Published SPI input port — checks whether a staff member is assigned to a program.
 *
 * <p>Published via {@code @NamedInterface("spi")} in this package's {@code package-info.java}.
 * Cross-module consumers (e.g. {@code imports}) must access this port ONLY through
 * {@code administration::spi}.
 *
 * <p>ARQ-001: method signatures use only UUID/primitive types — no domain entities cross the
 * module boundary.
 *
 * <p>Only <em>active</em> assignments ({@code active = true}) are considered for the check.
 */
public interface IsStaffAssignedToProgramQuery {

  /**
   * Check whether the given staff member has an active assignment to the given program.
   *
   * @param staffId the staff account UUID
   * @param programId the program UUID
   * @return {@code true} if an active assignment row exists for (staffId, programId)
   */
  boolean isAssigned(UUID staffId, UUID programId);

  /**
   * Return all program UUIDs for which the given staff member has active assignments.
   *
   * @param staffId the staff account UUID
   * @return set of program UUIDs; empty if none
   */
  Set<UUID> assignedPrograms(UUID staffId);
}
