package co.edu.uniquindio.brujula.identity.infrastructure.configuration;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAllowedDomainsPort;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.application.port.out.ReadActivityPort;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.application.service.GetCurrentUserService;
import co.edu.uniquindio.brujula.identity.application.service.InitiatePasswordRecoveryService;
import co.edu.uniquindio.brujula.identity.application.service.ListMyActivityService;
import co.edu.uniquindio.brujula.identity.application.service.ProvisionUserOnLoginService;
import co.edu.uniquindio.brujula.identity.application.service.RegisterStudentService;
import co.edu.uniquindio.brujula.identity.application.service.ResendEmailVerificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the identity module's application services.
 *
 * <p>Bridges domain/application (framework-free) into the Spring container. Infrastructure adapters
 * are auto-detected via {@code @Component}; services are registered here to maintain explicit
 * wiring.
 *
 * <p>Injects {@link AuditRecorder} (cross-module SPI from the audit module) into {@link
 * ProvisionUserOnLoginService} to satisfy S1-C wiring requirement (RNF-SEG-007).
 *
 * <p>S2-B additions: {@link RegisterStudentService} wired with Keycloak port + config properties
 * (G6 RESOLVED: consent terms/version source = configuration properties).
 */
@Configuration
class IdentityConfiguration {

  @Bean
  ProvisionUserOnLoginService provisionUserOnLoginService(
      LoadAppUserPort loadAppUserPort,
      SaveAppUserPort saveAppUserPort,
      LoadAllowedDomainsPort loadAllowedDomainsPort,
      AuditRecorder auditRecorder) {
    return new ProvisionUserOnLoginService(
        loadAppUserPort, saveAppUserPort, loadAllowedDomainsPort, auditRecorder);
  }

  @Bean
  GetCurrentUserService getCurrentUserService(LoadAppUserPort loadAppUserPort) {
    return new GetCurrentUserService(loadAppUserPort);
  }

  @Bean
  ListMyActivityService listMyActivityService(ReadActivityPort readActivityPort) {
    return new ListMyActivityService(readActivityPort);
  }

  /**
   * Resend email verification use case service (S2-C).
   *
   * <p>Reuses the same verify-redirect-uri as registration — same SPA landing page for the
   * verification flow. No user enumeration: service returns silently for unknown emails.
   */
  @Bean
  ResendEmailVerificationService resendEmailVerificationService(
      LoadAppUserPort loadAppUserPort,
      KeycloakUserProvisioningPort keycloakUserProvisioningPort,
      @Value("${brujula.registration.verify-redirect-uri:http://localhost:5173/verify-email}")
          String verifyRedirectUri) {
    return new ResendEmailVerificationService(
        loadAppUserPort, keycloakUserProvisioningPort, verifyRedirectUri);
  }

  /**
   * Initiate password recovery use case service (S2-D).
   *
   * <p>OPTION A / DT-011: triggers Keycloak execute-actions-email [UPDATE_PASSWORD]. Backend never
   * stores passwords. No user enumeration: service returns silently for unknown emails.
   */
  @Bean
  InitiatePasswordRecoveryService initiatePasswordRecoveryService(
      LoadAppUserPort loadAppUserPort,
      KeycloakUserProvisioningPort keycloakUserProvisioningPort,
      @Value("${brujula.registration.recovery-redirect-uri:http://localhost:5173/reset-password}")
          String recoveryRedirectUri) {
    return new InitiatePasswordRecoveryService(
        loadAppUserPort, keycloakUserProvisioningPort, recoveryRedirectUri);
  }

  /**
   * Register student use case service (S2-B).
   *
   * <p>G6 RESOLVED: consent terms/version source = config properties {@code
   * brujula.registration.current-terms-version} and {@code current-privacy-version}. The verify
   * redirect URI is the SPA landing page for the email verification flow (obs #1574).
   */
  @Bean
  RegisterStudentService registerStudentService(
      LoadAllowedDomainsPort loadAllowedDomainsPort,
      LoadAppUserPort loadAppUserPort,
      SaveAppUserPort saveAppUserPort,
      KeycloakUserProvisioningPort keycloakUserProvisioningPort,
      AuditRecorder auditRecorder,
      @Value("${brujula.registration.current-terms-version:1.0}") String currentTermsVersion,
      @Value("${brujula.registration.current-privacy-version:1.0}") String currentPrivacyVersion,
      @Value("${brujula.registration.verify-redirect-uri:http://localhost:5173/verify-email}")
          String verifyRedirectUri) {
    return new RegisterStudentService(
        loadAllowedDomainsPort,
        loadAppUserPort,
        saveAppUserPort,
        keycloakUserProvisioningPort,
        auditRecorder,
        currentTermsVersion,
        currentPrivacyVersion,
        verifyRedirectUri);
  }
}
