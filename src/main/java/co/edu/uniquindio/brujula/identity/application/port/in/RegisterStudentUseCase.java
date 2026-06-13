package co.edu.uniquindio.brujula.identity.application.port.in;

import co.edu.uniquindio.brujula.identity.application.dto.RegisterStudentCommand;
import co.edu.uniquindio.brujula.identity.application.dto.RegisterStudentResult;

/**
 * Input port: register a new student via self-registration.
 *
 * <p>Implemented by {@code RegisterStudentService} in the application service layer.
 *
 * <p>Framework-free: no Spring imports at this layer.
 */
public interface RegisterStudentUseCase {

  /**
   * Register a new student.
   *
   * @param command registration data (email, fullName, password, termsVersion, privacyVersion)
   * @return the result containing the new AppUser's id, email, and onboardingState
   * @throws co.edu.uniquindio.brujula.identity.domain.DomainNotAllowedException if the email domain
   *     is not in the allowed list
   * @throws EmailAlreadyRegisteredException if a user with this email already exists
   * @throws
   *     co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort.KeycloakProvisioningException
   *     if Keycloak returns an error during user creation
   */
  RegisterStudentResult register(RegisterStudentCommand command);
}
