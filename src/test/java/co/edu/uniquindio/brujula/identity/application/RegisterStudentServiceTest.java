package co.edu.uniquindio.brujula.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.identity.application.dto.RegisterStudentCommand;
import co.edu.uniquindio.brujula.identity.application.dto.RegisterStudentResult;
import co.edu.uniquindio.brujula.identity.application.port.in.RegisterStudentUseCase;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserId;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort;
import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort.KeycloakProvisioningException;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAllowedDomainsPort;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.application.service.RegisterStudentService;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.DomainNotAllowedException;
import co.edu.uniquindio.brujula.identity.domain.OnboardingState;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RED → GREEN use case tests for RegisterStudentService.
 *
 * <p>Uses Mockito mocks — zero Spring context (ARQ-002).
 *
 * <p>Scenarios: happy path, non-institutional email rejected BEFORE Keycloak, duplicate email
 * handling, Keycloak failure → no AppUser persisted, email verification triggered.
 *
 * <p>Satisfies: S2-B-3, Spec §Successful registration stores consent evidence, §Registration
 * rejected for non-institutional email (RN-007, RNF-SEG-006 dual enforcement).
 */
@ExtendWith(MockitoExtension.class)
class RegisterStudentServiceTest {

  @Mock private LoadAllowedDomainsPort loadAllowedDomainsPort;
  @Mock private LoadAppUserPort loadAppUserPort;
  @Mock private SaveAppUserPort saveAppUserPort;
  @Mock private KeycloakUserProvisioningPort keycloakPort;
  @Mock private AuditRecorder auditRecorder;

  private static final String CURRENT_TERMS_VERSION = "1.0";
  private static final String CURRENT_PRIVACY_VERSION = "1.0";
  private static final String VERIFY_REDIRECT_URI = "http://localhost:5173/verify-email";

  private RegisterStudentUseCase sut;

  @BeforeEach
  void setUp() {
    sut =
        new RegisterStudentService(
            loadAllowedDomainsPort,
            loadAppUserPort,
            saveAppUserPort,
            keycloakPort,
            auditRecorder,
            CURRENT_TERMS_VERSION,
            CURRENT_PRIVACY_VERSION,
            VERIFY_REDIRECT_URI);
  }

  // -------------------------------------------------------------------------
  // Happy path
  // -------------------------------------------------------------------------

  @Test
  void register_happyPath_provisionsKeycloakUser_andPersistsAppUser_andTriggersVerification() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadAppUserPort.existsByEmail("student@uniquindio.edu.co")).thenReturn(false);

    var keycloakUserId = new KeycloakUserId(UUID.randomUUID().toString());
    when(keycloakPort.createUser(any())).thenReturn(keycloakUserId);

    var savedUser = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");
    when(saveAppUserPort.save(any())).thenReturn(savedUser);

    var command =
        new RegisterStudentCommand("student@uniquindio.edu.co", "Juan Pérez", "1.0", "1.0");

    RegisterStudentResult result = sut.register(command);

    assertThat(result).isNotNull();
    assertThat(result.email()).isEqualTo("student@uniquindio.edu.co");
    // RegisterStudentResult.onboardingState is now a String (ARQ-001 compliance)
    assertThat(result.onboardingState()).isEqualTo("PENDING_VERIFICATION");

    // Keycloak user created
    verify(keycloakPort)
        .createUser(argThat(cmd -> "student@uniquindio.edu.co".equals(cmd.email())));

    // AppUser persisted with PENDING_VERIFICATION
    verify(saveAppUserPort)
        .save(
            argThat(
                user ->
                    user.onboardingState() == OnboardingState.PENDING_VERIFICATION
                        && user.termsVersion() != null
                        && user.privacyVersion() != null
                        && user.consentTimestamp() != null));

    // Email verification triggered
    verify(keycloakPort).triggerEmailVerification(eq(keycloakUserId), eq(VERIFY_REDIRECT_URI));

    // Audit recorded
    verify(auditRecorder)
        .record(
            argThat(
                cmd ->
                    "STUDENT_REGISTERED".equals(cmd.action())
                        && "app_user".equals(cmd.entityType())));
  }

  // -------------------------------------------------------------------------
  // Non-institutional email rejected BEFORE Keycloak (RN-007 / RNF-SEG-006)
  // -------------------------------------------------------------------------

  @Test
  void register_withDisallowedDomain_throwsDomainNotAllowedException_beforeAnyKeycloakCall() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));

    var command = new RegisterStudentCommand("hacker@gmail.com", "Bad Actor", "1.0", "1.0");

    assertThatThrownBy(() -> sut.register(command))
        .isInstanceOf(DomainNotAllowedException.class)
        .hasMessageContaining("gmail.com");

    // Keycloak MUST NOT be called — domain check is the first gate
    verify(keycloakPort, never()).createUser(any());
    verify(saveAppUserPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // Duplicate email → EmailAlreadyRegisteredException (409 / 422 depending on contract)
  // -------------------------------------------------------------------------

  @Test
  void register_withExistingEmail_throwsEmailAlreadyRegisteredException() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadAppUserPort.existsByEmail("existing@uniquindio.edu.co")).thenReturn(true);

    var command =
        new RegisterStudentCommand("existing@uniquindio.edu.co", "Existing User", "1.0", "1.0");

    assertThatThrownBy(() -> sut.register(command))
        .isInstanceOf(EmailAlreadyRegisteredException.class);

    verify(keycloakPort, never()).createUser(any());
  }

  // -------------------------------------------------------------------------
  // Keycloak failure → AppUser NOT persisted (transactional guard)
  // -------------------------------------------------------------------------

  @Test
  void register_whenKeycloakCreateUserFails_doesNotPersistAppUser() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadAppUserPort.existsByEmail("student@uniquindio.edu.co")).thenReturn(false);
    when(keycloakPort.createUser(any()))
        .thenThrow(new KeycloakProvisioningException("Keycloak down", "KEYCLOAK_503"));

    var command =
        new RegisterStudentCommand("student@uniquindio.edu.co", "Juan Pérez", "1.0", "1.0");

    assertThatThrownBy(() -> sut.register(command))
        .isInstanceOf(KeycloakProvisioningException.class);

    // No AppUser row saved when Keycloak fails
    verify(saveAppUserPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // Email verification triggered AFTER provisioning
  // -------------------------------------------------------------------------

  @Test
  void register_shouldTriggerEmailVerification_afterSuccessfulProvisioning() {
    when(loadAllowedDomainsPort.loadAllowedDomains()).thenReturn(Set.of("uniquindio.edu.co"));
    when(loadAppUserPort.existsByEmail("student@uniquindio.edu.co")).thenReturn(false);

    var keycloakUserId = new KeycloakUserId("kc-user-id-abc");
    when(keycloakPort.createUser(any())).thenReturn(keycloakUserId);

    var savedUser = AppUser.register("student@uniquindio.edu.co", "Juan Pérez");
    when(saveAppUserPort.save(any())).thenReturn(savedUser);

    var command =
        new RegisterStudentCommand("student@uniquindio.edu.co", "Juan Pérez", "1.0", "1.0");

    sut.register(command);

    verify(keycloakPort).triggerEmailVerification(eq(keycloakUserId), eq(VERIFY_REDIRECT_URI));
  }
}
