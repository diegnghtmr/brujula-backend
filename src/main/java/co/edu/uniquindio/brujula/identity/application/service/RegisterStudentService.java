package co.edu.uniquindio.brujula.identity.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.identity.application.EmailAlreadyRegisteredException;
import co.edu.uniquindio.brujula.identity.application.dto.RegisterStudentCommand;
import co.edu.uniquindio.brujula.identity.application.dto.RegisterStudentResult;
import co.edu.uniquindio.brujula.identity.application.port.in.RegisterStudentUseCase;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort.ProvisionUserCommand;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAllowedDomainsPort;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AllowedEmailDomainPolicy;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.DomainNotAllowedException;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case service: register a new student via self-registration.
 *
 * <p>Orchestration (Slice 2, S2-B-3):
 *
 * <ol>
 *   <li>Validate email domain against allowed-domain list (RN-007, RNF-SEG-006 dual enforcement).
 *       This is the FIRST gate — Keycloak is NEVER called for rejected domains.
 *   <li>Check email is not already registered (pre-condition for idempotency).
 *   <li>Create user in Keycloak via {@link KeycloakUserProvisioningPort#createUser}. If Keycloak
 *       fails, the method throws and no AppUser is persisted.
 *   <li>Register {@link AppUser} with consent evidence captured, onboardingState=PENDING_VERIFICATION.
 *   <li>Persist the AppUser.
 *   <li>Trigger email verification via {@link
 *       KeycloakUserProvisioningPort#triggerEmailVerification}. If this fails, log a warning and
 *       continue (email can be resent; the AppUser is already persisted — resilience decision).
 *   <li>Record {@code STUDENT_REGISTERED} audit event (RNF-SEG-007).
 * </ol>
 *
 * <p>Framework-free (ARQ-002). Wired by {@link
 * co.edu.uniquindio.brujula.identity.infrastructure.configuration.IdentityConfiguration}.
 *
 * <p>Gap G6 RESOLVED: consent terms/version source = configuration properties {@code
 * brujula.registration.current-terms-version} and {@code current-privacy-version}.
 */
public class RegisterStudentService implements RegisterStudentUseCase {

  private static final Logger log = LoggerFactory.getLogger(RegisterStudentService.class);

  private final LoadAllowedDomainsPort loadAllowedDomainsPort;
  private final LoadAppUserPort loadAppUserPort;
  private final SaveAppUserPort saveAppUserPort;
  private final KeycloakUserProvisioningPort keycloakPort;
  private final AuditRecorder auditRecorder;
  private final String currentTermsVersion;
  private final String currentPrivacyVersion;
  private final String verifyRedirectUri;

  public RegisterStudentService(
      LoadAllowedDomainsPort loadAllowedDomainsPort,
      LoadAppUserPort loadAppUserPort,
      SaveAppUserPort saveAppUserPort,
      KeycloakUserProvisioningPort keycloakPort,
      AuditRecorder auditRecorder,
      String currentTermsVersion,
      String currentPrivacyVersion,
      String verifyRedirectUri) {
    this.loadAllowedDomainsPort = loadAllowedDomainsPort;
    this.loadAppUserPort = loadAppUserPort;
    this.saveAppUserPort = saveAppUserPort;
    this.keycloakPort = keycloakPort;
    this.auditRecorder = auditRecorder;
    this.currentTermsVersion = currentTermsVersion;
    this.currentPrivacyVersion = currentPrivacyVersion;
    this.verifyRedirectUri = verifyRedirectUri;
  }

  @Override
  public RegisterStudentResult register(RegisterStudentCommand command) {
    // 1. Enforce allowed-domain policy — FIRST gate (RN-007, RNF-SEG-006 dual enforcement).
    //    Keycloak is NEVER called if the domain is rejected.
    Set<String> allowedDomains = loadAllowedDomainsPort.loadAllowedDomains();
    var policy = new AllowedEmailDomainPolicy(allowedDomains);
    if (!policy.isAllowed(command.email())) {
      throw new DomainNotAllowedException(command.email());
    }

    // 2. Check email is not already registered.
    if (loadAppUserPort.existsByEmail(command.email())) {
      throw new EmailAlreadyRegisteredException(command.email());
    }

    // 3. Create user in Keycloak (throws KeycloakProvisioningException on failure).
    //    OPTION A (DT-011): no password here. The backend creates the Keycloak user without
    //    a credential; the UPDATE_PASSWORD action in triggerEmailVerification (step 6) prompts
    //    the user to set their password on Keycloak's own page after email verification.
    var keycloakUserId =
        keycloakPort.createUser(new ProvisionUserCommand(command.email(), command.fullName()));

    // 4. Build AppUser with consent evidence and PENDING_VERIFICATION state.
    AppUser newUser = AppUser.register(command.email(), command.fullName());
    newUser.captureConsent(
        currentTermsVersion, currentPrivacyVersion, Instant.now());

    // 5. Persist the AppUser.
    AppUser saved = saveAppUserPort.save(newUser);

    // 6. Trigger email verification (resilience: failure does not roll back AppUser).
    try {
      keycloakPort.triggerEmailVerification(keycloakUserId, verifyRedirectUri);
    } catch (KeycloakUserProvisioningPort.KeycloakProvisioningException e) {
      log.warn(
          "Failed to trigger email verification for user {} (Keycloak userId: {}). "
              + "User can request a resend. Error: {}",
          saved.email(),
          keycloakUserId.value(),
          e.getMessage());
    }

    // 7. Record audit event (RNF-SEG-007: synchronous, same logical unit).
    auditRecorder.record(
        new RecordAuditEventCommand(
            saved.id(),
            "STUDENT",
            "STUDENT_REGISTERED",
            "app_user",
            saved.id(),
            null,
            null,
            null));

    // ARQ-001: onboardingState passed as String to avoid REST adapter importing domain enum.
    return new RegisterStudentResult(saved.id(), saved.email(), saved.onboardingState().name());
  }
}
