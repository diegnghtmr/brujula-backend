package co.edu.uniquindio.brujula.administration.application.service;

import co.edu.uniquindio.brujula.administration.application.port.in.spi.ResolveStaffByEmailQuery;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service: resolves a staff UUID from an institutional email address.
 *
 * <p>Implements {@link ResolveStaffByEmailQuery} published via {@code administration::spi}.
 * Delegates to the existing {@link LoadStaffPort#findByEmail(String)} which performs a lookup
 * against {@code administration.staff_account.email} (UNIQUE constraint, V003).
 *
 * <p>Framework-free: no Spring or JPA dependencies (ARQ-002). Wired via {@code @Bean} in
 * {@link co.edu.uniquindio.brujula.administration.infrastructure.configuration.AdministrationConfiguration}.
 *
 * <p>Actor→StaffAccount seam: enables consuming modules to translate an OIDC email claim into
 * a staffId for subsequent assignment checks (IMP-010, SC-010-D..E).
 */
public class ResolveStaffByEmailService implements ResolveStaffByEmailQuery {

  private final LoadStaffPort loadStaffPort;

  public ResolveStaffByEmailService(LoadStaffPort loadStaffPort) {
    this.loadStaffPort = loadStaffPort;
  }

  @Override
  public Optional<UUID> resolveStaffId(String email) {
    return loadStaffPort.findByEmail(email).map(account -> account.getId());
  }
}
