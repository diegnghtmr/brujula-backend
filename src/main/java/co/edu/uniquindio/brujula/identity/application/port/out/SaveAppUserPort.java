package co.edu.uniquindio.brujula.identity.application.port.out;

import co.edu.uniquindio.brujula.identity.domain.AppUser;

/**
 * Output port: persists an AppUser.
 *
 * <p>Implemented by {@code AppUserPersistenceAdapter} in the infrastructure layer.
 */
public interface SaveAppUserPort {

  /**
   * Save (insert or update) the given user.
   *
   * @param user the domain object to persist
   * @return the saved user (may have updated version from optimistic lock)
   */
  AppUser save(AppUser user);
}
