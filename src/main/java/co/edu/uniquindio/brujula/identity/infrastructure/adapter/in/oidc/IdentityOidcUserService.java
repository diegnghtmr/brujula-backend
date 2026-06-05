package co.edu.uniquindio.brujula.identity.infrastructure.adapter.in.oidc;

import co.edu.uniquindio.brujula.identity.application.dto.ProvisionCommand;
import co.edu.uniquindio.brujula.identity.application.port.in.ProvisionUserOnLoginUseCase;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * OIDC user service: loads the OIDC user and triggers identity provisioning on login.
 *
 * <p>Implements {@link OAuth2UserService} to integrate with Spring Security's oauth2Login. Called
 * during the OIDC callback. Steps:
 *
 * <ol>
 *   <li>Load the OIDC user from the default service (fetches userinfo endpoint).
 *   <li>Map Keycloak claims to a {@link ProvisionCommand} via {@link OidcUserClaimsMapper}.
 *   <li>Invoke {@link ProvisionUserOnLoginUseCase} to find-or-create the AppUser record.
 *   <li>Return a {@link DefaultOidcUser} with Spring-style {@code ROLE_X} authorities derived from
 *       the domain role.
 * </ol>
 *
 * <p>The returned authorities use the G3-verified pattern: {@code new
 * SimpleGrantedAuthority("ROLE_X")} so that {@code hasRole("ADMIN")} etc. work correctly with
 * {@code @EnableMethodSecurity} (Security-7 facts, obs #1548).
 *
 * <p>Wired into {@code SecurityConfiguration} in WU-B (replaces the default OidcUserService).
 * Satisfies: S1-B-5, Design §2.1 IdentityOidcUserService.
 */
@Component
public class IdentityOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

  private final OidcUserService delegate = new OidcUserService();
  private final ProvisionUserOnLoginUseCase provisionUseCase;
  private final OidcUserClaimsMapper claimsMapper;

  public IdentityOidcUserService(
      ProvisionUserOnLoginUseCase provisionUseCase, OidcUserClaimsMapper claimsMapper) {
    this.provisionUseCase = provisionUseCase;
    this.claimsMapper = claimsMapper;
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    // 1. Fetch userinfo from Keycloak
    OidcUser oidcUser = delegate.loadUser(userRequest);

    // 2. Map claims and provision the AppUser
    ProvisionCommand command = claimsMapper.toCommand(oidcUser);
    AppUser appUser = provisionUseCase.provision(command);

    // 3. Build Spring Security authorities from the domain role (G3 pattern)
    Collection<GrantedAuthority> authorities = buildAuthorities(appUser.role());

    // 4. Return a DefaultOidcUser with the provisioned authorities and user-name attribute
    String userNameAttributeName =
        userRequest
            .getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();

    return new DefaultOidcUser(
        authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), userNameAttributeName);
  }

  private Collection<GrantedAuthority> buildAuthorities(Role role) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
    return authorities;
  }
}
