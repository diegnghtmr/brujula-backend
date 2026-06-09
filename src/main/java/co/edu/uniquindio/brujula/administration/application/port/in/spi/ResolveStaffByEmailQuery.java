package co.edu.uniquindio.brujula.administration.application.port.in.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Published SPI input port — resolves a staff UUID from an institutional email address.
 *
 * <p>This is the actor→StaffAccount seam used by consuming modules to translate an OIDC email
 * claim into the administration module's internal staff identifier, enabling the assignment
 * check via {@link IsStaffAssignedToProgramQuery}.
 *
 * <p>Published via {@code @NamedInterface("spi")} in this package's {@code package-info.java}.
 * Cross-module consumers must access this port ONLY through {@code administration::spi}.
 *
 * <p>ARQ-001: returns {@link java.util.Optional} of UUID — no domain types cross the boundary.
 */
public interface ResolveStaffByEmailQuery {

  /**
   * Resolve the staff UUID for the given institutional email address.
   *
   * <p>The lookup is performed against {@code administration.staff_account.email} (UNIQUE
   * constraint). Both ACTIVE and INACTIVE staff accounts are resolved; callers are responsible
   * for deciding whether an inactive account should be rejected.
   *
   * @param email the institutional email address of the actor
   * @return the staff UUID if a matching {@code StaffAccount} exists; empty otherwise
   */
  Optional<UUID> resolveStaffId(String email);

  /**
   * Resolve the staff UUID for an ACTIVE staff account with the given email.
   *
   * <p>Returns empty if: (a) no matching StaffAccount exists, OR (b) the matching StaffAccount
   * has status INACTIVE. This is the preferred method for authorization checks (WARNING-1 closure
   * from WU2 verify report): callers that need to enforce "INACTIVE staff → 403" should use this
   * method instead of {@link #resolveStaffId(String)}.
   *
   * <p>ARQ-001: returns UUID only — no domain types cross the spi boundary.
   *
   * @param email the institutional email address (normalized to lowercase before lookup)
   * @return the staff UUID if an ACTIVE matching StaffAccount exists; empty otherwise
   */
  Optional<UUID> resolveActiveStaffId(String email);
}
