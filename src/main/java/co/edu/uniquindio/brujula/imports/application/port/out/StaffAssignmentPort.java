package co.edu.uniquindio.brujula.imports.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: staff assignment check for the imports module.
 *
 * <p>Named by need (ARQ-001). Implemented by {@code StaffAssignmentAdapter} in
 * {@code imports.infrastructure.adapter.out.administration}, which delegates to
 * {@code administration::spi.IsStaffAssignedToProgramQuery} and
 * {@code administration::spi.ResolveStaffByEmailQuery}.
 *
 * <p>This port insulates the imports application layer from the {@code administration} module
 * internals (anti-corruption adapter pattern, mirroring {@code CreditLimitPort} in simulation).
 *
 * <p>The upload/confirm services (WU3/WU6) will depend on this port; not on administration
 * directly.
 */
public interface StaffAssignmentPort {

  /**
   * Check whether the given staff member has an active assignment to the given program.
   *
   * @param staffId the staff UUID
   * @param programId the program UUID
   * @return {@code true} if an active assignment exists
   */
  boolean isAssigned(UUID staffId, UUID programId);

  /**
   * Resolve the staff UUID from the actor's institutional email address.
   *
   * <p>Used to translate the OIDC principal email claim into a staffId before calling
   * {@link #isAssigned(UUID, UUID)}. Resolves BOTH active and inactive staff accounts; callers
   * decide whether inactive = 403.
   *
   * @param email institutional email address
   * @return the staff UUID if found; empty if no matching StaffAccount exists
   */
  Optional<UUID> resolveStaffId(String email);

  /**
   * Resolve the staff UUID for an ACTIVE staff account only.
   *
   * <p>WARNING-1 closure: returns empty if the matching StaffAccount is INACTIVE, preventing
   * a deactivated staff member with an un-revoked active assignment from passing the 403 guard.
   *
   * <p>This is the method {@link co.edu.uniquindio.brujula.imports.application.service.UploadImportService}
   * MUST use for the STAFF role authorization check.
   *
   * @param email institutional email address
   * @return the staff UUID if an ACTIVE StaffAccount with this email exists; empty otherwise
   */
  Optional<UUID> resolveActiveStaffId(String email);

  /**
   * Return all program UUIDs for which the given staff member has active assignments.
   *
   * <p>Used by ListImportsService (IMP-003) to scope the list query to assigned programs.
   *
   * @param staffId the staff account UUID
   * @return list of program UUIDs; empty if none
   */
  List<UUID> getAssignedPrograms(UUID staffId);
}
