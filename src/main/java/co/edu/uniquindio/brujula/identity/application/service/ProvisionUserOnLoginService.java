package co.edu.uniquindio.brujula.identity.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.identity.application.dto.ProvisionCommand;
import co.edu.uniquindio.brujula.identity.application.port.in.ProvisionUserOnLoginUseCase;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAllowedDomainsPort;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AllowedEmailDomainPolicy;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.DomainNotAllowedException;
import co.edu.uniquindio.brujula.identity.domain.OnboardingState;
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
 * <p>Records a {@code USER_PROVISIONED} audit event on first login via {@link AuditRecorder}
 * (RNF-SEG-007 synchronous, same transaction). Satisfies S1-C wiring requirement.
 *
 * <p>Sync-on-login (S2-B-4, obs #1574): when an existing AppUser is in PENDING_VERIFICATION state
 * and the OIDC claims contain {@code email_verified=true}, transitions the user to ACTIVE and
 * persists. This is the Keycloak-native email verification sync mechanism — no backend
 * token-confirm endpoint needed.
 *
 * <p>No Spring annotations here — wired by the infrastructure configuration. Satisfies S1-B-3.
 */
public class ProvisionUserOnLoginService implements ProvisionUserOnLoginUseCase {

  private final LoadAppUserPort loadAppUserPort;
  private final SaveAppUserPort saveAppUserPort;
  private final LoadAllowedDomainsPort loadAllowedDomainsPort;
  private final AuditRecorder auditRecorder;

  public ProvisionUserOnLoginService(
      LoadAppUserPort loadAppUserPort,
      SaveAppUserPort saveAppUserPort,
      LoadAllowedDomainsPort loadAllowedDomainsPort,
      AuditRecorder auditRecorder) {
    this.loadAppUserPort = loadAppUserPort;
    this.saveAppUserPort = saveAppUserPort;
    this.loadAllowedDomainsPort = loadAllowedDomainsPort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public AppUser provision(ProvisionCommand command) {
    // 1. Enforce allowed-domain policy (RN-007)
    Set<String> allowed = loadAllowedDomainsPort.loadAllowedDomains();
    var policy = new AllowedEmailDomainPolicy(allowed);
    if (!policy.isAllowed(command.email())) {
      throw new DomainNotAllowedException(command.email());
    }

    // 2. Find existing user by OIDC subject (idempotency — handles subsequent logins)
    Optional<AppUser> existing = loadAppUserPort.findByOidcSubject(command.oidcSubject());
    if (existing.isPresent()) {
      return syncOnLogin(existing.get(), command);
    }

    // 2b. C-002: fall back to email lookup for self-registered users whose oidcSubject is null.
    //     AppUser.register() creates a row with oidcSubject=null. On the user's first OIDC login
    //     findByOidcSubject misses (the row has null, not the real sub). findByEmail finds it.
    //     We link the sub and sync onboardingState in place — no duplicate row created.
    Optional<AppUser> existingByEmail = loadAppUserPort.findByEmail(command.email());
    if (existingByEmail.isPresent() && existingByEmail.get().oidcSubject() == null) {
      AppUser toLink = existingByEmail.get();
      toLink.linkOidcSubject(command.oidcSubject());
      // Apply email_verified sync (same logic as syncOnLogin)
      boolean emailVerified = Boolean.TRUE.equals(command.emailVerified());
      if (toLink.onboardingState() == OnboardingState.PENDING_VERIFICATION && emailVerified) {
        toLink.activate();
      }
      return saveAppUserPort.save(toLink);
    }

    // 3. First login: determine role from Keycloak claims (highest privilege)
    Role role = mapRole(command.keycloakRoles());

    // 4. Provision and persist
    AppUser newUser =
        AppUser.provision(command.oidcSubject(), command.email(), command.fullName(), role);
    AppUser saved = saveAppUserPort.save(newUser);

    // 5. Record audit event for first-time provisioning (RNF-SEG-007)
    auditRecorder.record(
        new RecordAuditEventCommand(
            saved.id(), role.name(), "USER_PROVISIONED", "app_user", saved.id(), null, null, null));

    return saved;
  }

  /**
   * Sync-on-login: if the user is PENDING_VERIFICATION and the OIDC token carries
   * email_verified=true, transition to ACTIVE and persist (obs #1574).
   */
  private AppUser syncOnLogin(AppUser user, ProvisionCommand command) {
    boolean emailVerified = Boolean.TRUE.equals(command.emailVerified());
    if (user.onboardingState() == OnboardingState.PENDING_VERIFICATION && emailVerified) {
      user.activate();
      return saveAppUserPort.save(user);
    }
    return user;
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
