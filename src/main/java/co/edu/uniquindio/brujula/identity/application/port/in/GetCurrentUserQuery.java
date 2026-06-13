package co.edu.uniquindio.brujula.identity.application.port.in;

import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import java.util.UUID;

/**
 * Input port: retrieve the authenticated user's own profile.
 *
 * <p>Backs {@code GET /me}. The caller (REST adapter) resolves the user id from the session
 * principal — no external id parameter accepted (BOLA defense).
 *
 * <p>Returns {@link UserProfile} (application-layer DTO) so that REST adapters do not import domain
 * classes directly.
 */
public interface GetCurrentUserQuery {

  /**
   * Return the user profile for the given session user id.
   *
   * @param userId the internal UUID of the authenticated user
   * @return the UserProfile DTO
   * @throws co.edu.uniquindio.brujula.identity.domain.UserNotFoundException if not found
   */
  UserProfile getCurrentUser(UUID userId);

  /**
   * Return the user profile by OIDC subject string (for REST adapter resolution from principal).
   *
   * @param oidcSubject the {@code sub} claim from the session principal
   * @return the UserProfile DTO
   * @throws co.edu.uniquindio.brujula.identity.domain.UserNotFoundException if not found
   */
  UserProfile getCurrentUserBySubject(String oidcSubject);
}
