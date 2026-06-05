package co.edu.uniquindio.brujula.identity.application.service;

import co.edu.uniquindio.brujula.identity.application.dto.ProvisionCommand;
import co.edu.uniquindio.brujula.identity.application.port.in.ProvisionUserOnLoginUseCase;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAllowedDomainsPort;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AllowedEmailDomainPolicy;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.DomainNotAllowedException;
import co.edu.uniquindio.brujula.identity.domain.Role;
import java.util.Optional;
import java.util.Set;

/**
 * Use case service: provision (find-or-create) an AppUser on OIDC login.
 *
 * <p>Idempotent: first login creates the record, subsequent logins load it. Keycloak realm roles
 * ({@code realm_access.roles}) are mapped to {@link Role}: ADMIN→ADMIN, STAFF→STAFF,
 * STUDENT→STUDENT (highest privilege wins). Unknown roles default to STUDENT.
 *
 * <p>Enforces allowed-email-domain policy (RN-007, RNF-SEG-006 dual enforcement).
 *
 * <p>No Spring annotations here — wired by the infrastructure configuration. Satisfies S1-B-3.
 */
public class ProvisionUserOnLoginService implements ProvisionUserOnLoginUseCase {

  private final LoadAppUserPort loadAppUserPort;
  private final SaveAppUserPort saveAppUserPort;
  private final LoadAllowedDomainsPort loadAllowedDomainsPort;

  public ProvisionUserOnLoginService(
      LoadAppUserPort loadAppUserPort,
      SaveAppUserPort saveAppUserPort,
      LoadAllowedDomainsPort loadAllowedDomainsPort) {
    this.loadAppUserPort = loadAppUserPort;
    this.saveAppUserPort = saveAppUserPort;
    this.loadAllowedDomainsPort = loadAllowedDomainsPort;
  }

  @Override
  public AppUser provision(ProvisionCommand command) {
    // 1. Enforce allowed-domain policy (RN-007)
    Set<String> allowed = loadAllowedDomainsPort.loadAllowedDomains();
    var policy = new AllowedEmailDomainPolicy(allowed);
    if (!policy.isAllowed(command.email())) {
      throw new DomainNotAllowedException(command.email());
    }

    // 2. Find existing user (idempotency)
    Optional<AppUser> existing = loadAppUserPort.findByOidcSubject(command.oidcSubject());
    if (existing.isPresent()) {
      return existing.get();
    }

    // 3. First login: determine role from Keycloak claims (highest privilege)
    Role role = mapRole(command.keycloakRoles());

    // 4. Provision and persist
    AppUser newUser =
        AppUser.provision(command.oidcSubject(), command.email(), command.fullName(), role);
    return saveAppUserPort.save(newUser);
  }

  /**
   * Map Keycloak realm roles to domain Role. Highest privilege wins: ADMIN > STAFF > STUDENT.
   * Unknown roles default to STUDENT.
   */
  private Role mapRole(Set<String> keycloakRoles) {
    if (keycloakRoles != null) {
      if (keycloakRoles.contains("ADMIN")) return Role.ADMIN;
      if (keycloakRoles.contains("STAFF")) return Role.STAFF;
      if (keycloakRoles.contains("STUDENT")) return Role.STUDENT;
    }
    return Role.STUDENT;
  }
}
