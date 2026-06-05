package co.edu.uniquindio.brujula.identity.application.port.in;

import co.edu.uniquindio.brujula.identity.application.dto.ProvisionCommand;
import co.edu.uniquindio.brujula.identity.domain.AppUser;

/**
 * Input port: provision (find-or-create) an AppUser on OIDC login.
 *
 * <p>Invoked by the OIDC adapter ({@code IdentityOidcUserService}) during the Spring Security
 * OAuth2 callback. Idempotent: first call creates a record; subsequent calls for the same subject
 * load the existing user.
 */
public interface ProvisionUserOnLoginUseCase {

  /**
   * Provision or load the user for the given OIDC claims.
   *
   * @param command OIDC claims from Keycloak token
   * @return the domain AppUser (newly created or existing)
   * @throws co.edu.uniquindio.brujula.identity.domain.DomainException if the email domain is not in
   *     the allowed list
   */
  AppUser provision(ProvisionCommand command);
}
