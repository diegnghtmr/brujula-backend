package co.edu.uniquindio.brujula.imports.application.port.out;

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
   * {@link #isAssigned(UUID, UUID)}.
   *
   * @param email institutional email address
   * @return the staff UUID if found; empty if no matching StaffAccount exists
   */
  Optional<UUID> resolveStaffId(String email);
}
