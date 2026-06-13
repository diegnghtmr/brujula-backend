package co.edu.uniquindio.brujula.identity.application.service;

import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.UserNotFoundException;
import java.util.UUID;

/**
 * Query service: returns the authenticated user's profile.
 *
 * <p>Backs {@code GET /me}. The caller resolves the user id from the session principal; this
 * service only loads by internal UUID or OIDC subject. BOLA defence: no external id override path.
 *
 * <p>Maps {@link AppUser} (domain) to {@link UserProfile} (application DTO). Domain types are never
 * returned directly to REST adapters.
 */
public class GetCurrentUserService implements GetCurrentUserQuery {

  private final LoadAppUserPort loadAppUserPort;

  public GetCurrentUserService(LoadAppUserPort loadAppUserPort) {
    this.loadAppUserPort = loadAppUserPort;
  }

  @Override
  public UserProfile getCurrentUser(UUID userId) {
    AppUser user =
        loadAppUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    return toProfile(user);
  }

  @Override
  public UserProfile getCurrentUserBySubject(String oidcSubject) {
    AppUser user =
        loadAppUserPort
            .findByOidcSubject(oidcSubject)
            .orElseThrow(() -> new UserNotFoundException(oidcSubject));
    return toProfile(user);
  }

  private UserProfile toProfile(AppUser user) {
    return new UserProfile(
        user.id(),
        user.oidcSubject(),
        user.email(),
        user.fullName(),
        user.role().name(),
        user.status().name(),
        user.onboardingCompleted());
  }
}
