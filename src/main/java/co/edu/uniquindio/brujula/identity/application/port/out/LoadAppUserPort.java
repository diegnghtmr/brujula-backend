package co.edu.uniquindio.brujula.identity.application.port.out;

import co.edu.uniquindio.brujula.identity.domain.AppUser;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: loads an AppUser from persistence.
 *
 * <p>Implemented by {@code AppUserPersistenceAdapter} in the infrastructure layer.
 */
public interface LoadAppUserPort {

  /**
   * Find an app_user by OIDC subject (sub claim).
   *
   * @param oidcSubject the OIDC subject identifier from the JWT/OIDC token
   * @return the user if one has been provisioned for this subject
   */
  Optional<AppUser> findByOidcSubject(String oidcSubject);

  /**
   * Find an app_user by its internal UUID.
   *
   * @param id the internal user identifier
   * @return the user if found
   */
  Optional<AppUser> findById(UUID id);
}
