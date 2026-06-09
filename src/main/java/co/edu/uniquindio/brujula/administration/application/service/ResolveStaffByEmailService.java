package co.edu.uniquindio.brujula.administration.application.service;

import co.edu.uniquindio.brujula.administration.application.port.in.spi.ResolveStaffByEmailQuery;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.domain.StaffAccountStatus;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service: resolves a staff UUID from an institutional email address.
 *
 * <p>Implements {@link ResolveStaffByEmailQuery} published via {@code administration::spi}.
 * Delegates to the existing {@link LoadStaffPort#findByEmail(String)} which performs a lookup
 * against {@code administration.staff_account.email} (UNIQUE constraint, V003).
 *
 * <p>Framework-free: no Spring or JPA dependencies (ARQ-002). Wired via {@code @Bean} in {@link
 * co.edu.uniquindio.brujula.administration.infrastructure.configuration.AdministrationConfiguration}.
 *
 * <p>Actor→StaffAccount seam: enables consuming modules to translate an OIDC email claim into a
 * staffId for subsequent assignment checks (IMP-010, SC-010-D..E).
 *
 * <p>WARNING-1 closure (WU3): {@link #resolveActiveStaffId(String)} returns empty for INACTIVE
 * accounts, preventing deactivated staff from bypassing the 403 guard even when they have an
 * un-revoked active assignment row.
 */
public class ResolveStaffByEmailService implements ResolveStaffByEmailQuery {

  private final LoadStaffPort loadStaffPort;

  public ResolveStaffByEmailService(LoadStaffPort loadStaffPort) {
    this.loadStaffPort = loadStaffPort;
  }

  /** Resolves both ACTIVE and INACTIVE accounts. Callers decide whether inactive = 403. */
  @Override
  public Optional<UUID> resolveStaffId(String email) {
    return loadStaffPort.findByEmail(normalizeEmail(email)).map(account -> account.getId());
  }

  /**
   * Resolves only ACTIVE accounts. Returns empty for INACTIVE accounts or unknown emails.
   *
   * <p>This is the load-bearing WARNING-1 closure: an INACTIVE StaffAccount with an ACTIVE
   * assignment row must NOT pass the upload/confirm access guard.
   */
  @Override
  public Optional<UUID> resolveActiveStaffId(String email) {
    return loadStaffPort
        .findByEmail(normalizeEmail(email))
        .filter(account -> account.getStatus() == StaffAccountStatus.ACTIVE)
        .map(account -> account.getId());
  }

  /**
   * Normalizes an email for case-insensitive lookup (WARNING-2 partial closure).
   *
   * <p>Trims whitespace and converts to lowercase before querying the DB. This handles the common
   * case where an OIDC provider returns a mixed-case email while StaffAccount records are stored in
   * lowercase. Note: the underlying DB column (V003 TEXT) is case-sensitive; this normalization
   * happens at the service layer.
   */
  private String normalizeEmail(String email) {
    if (email == null) return null;
    return email.trim().toLowerCase();
  }
}
