package co.edu.uniquindio.brujula.administration.application.port.out;

import java.util.Set;
import java.util.UUID;

/**
 * Output port: load active staff-program assignments.
 *
 * <p>Returns the set of program UUIDs to which the given staff member has an <em>active</em>
 * assignment ({@code active = true} in {@code administration.staff_program_assignment}).
 *
 * <p>Named by need (ARQ-001). Implemented by the persistence adapter in {@code
 * administration.infrastructure.adapter.out.persistence}.
 */
public interface LoadStaffProgramAssignmentsPort {

  /**
   * Return the set of program UUIDs for which the given staff member has an active assignment.
   *
   * @param staffId the staff account UUID
   * @return set of program UUIDs with {@code active = true}; empty if none
   */
  Set<UUID> findActiveAssignmentsByStaffId(UUID staffId);
}
