package co.edu.uniquindio.brujula.identity.infrastructure.adapter.in.oidc;

import co.edu.uniquindio.brujula.identity.application.dto.ProvisionCommand;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Maps Keycloak OIDC claims to a {@link ProvisionCommand}.
 *
 * <p>Keycloak includes realm roles in the {@code realm_access.roles} claim (configured in the realm
 * export as a mapper). This component extracts those roles and the standard claims (sub, email,
 * name/preferred_username).
 */
@Component
public class OidcUserClaimsMapper {

  /**
   * Build a {@link ProvisionCommand} from the Keycloak OIDC user claims.
   *
   * @param oidcUser the authenticated OIDC principal
   * @return a provision command ready for the use case
   */
  public ProvisionCommand toCommand(OidcUser oidcUser) {
    String sub = oidcUser.getSubject();
    String email = oidcUser.getEmail();
    String fullName = resolveFullName(oidcUser);
    Set<String> roles = extractRealmRoles(oidcUser);
    // Extract email_verified claim for onboardingState transition on sync-on-login.
    Boolean emailVerified = oidcUser.getEmailVerified();
    return new ProvisionCommand(sub, email, fullName, roles, emailVerified);
  }

  private String resolveFullName(OidcUser oidcUser) {
    String name = oidcUser.getFullName();
    if (name != null && !name.isBlank()) {
      return name;
    }
    String preferred = oidcUser.getPreferredUsername();
    if (preferred != null && !preferred.isBlank()) {
      return preferred;
    }
    return oidcUser.getEmail();
  }

  @SuppressWarnings("unchecked")
  private Set<String> extractRealmRoles(OidcUser oidcUser) {
    Object realmAccess = oidcUser.getClaims().get("realm_access");
    if (realmAccess instanceof Map<?, ?> map) {
      Object roles = map.get("roles");
      if (roles instanceof List<?> list) {
        return list.stream()
            .filter(r -> r instanceof String)
            .map(r -> ((String) r).toUpperCase())
            .collect(Collectors.toUnmodifiableSet());
      }
    }
    return Collections.emptySet();
  }
}
