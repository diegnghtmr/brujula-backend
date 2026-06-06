package co.edu.uniquindio.brujula.identity.infrastructure.adapter.out.client;

import co.edu.uniquindio.brujula.identity.application.port.out.KeycloakUserProvisioningPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for the Keycloak Admin API adapter.
 *
 * <p>Wires {@link KeycloakAdminApiAdapter} as a Spring bean implementing {@link
 * KeycloakUserProvisioningPort}. Uses explicit {@code @Bean} wiring (hexagonal pattern) to keep the
 * adapter class Spring-free and easily testable in isolation.
 *
 * <p>NOTE: Spring Boot 4 no longer auto-configures a {@link RestClient.Builder} bean. This
 * configuration creates a dedicated {@link RestTemplate} for the Keycloak Admin API and wraps it
 * with {@code RestClient.builder(restTemplate)}. This ensures that {@code MockRestServiceServer}
 * can intercept ALL HTTP calls (token + admin) in tests.
 *
 * <p>Configuration properties (from {@code application.yaml}; overridden by environment):
 *
 * <ul>
 *   <li>{@code brujula.keycloak.admin-base-url} — Admin REST API base URL (e.g. {@code
 *       http://localhost:9090/admin/realms/brujula-dev})
 *   <li>{@code brujula.keycloak.token-url} — token endpoint URL (e.g. {@code
 *       http://localhost:9090/realms/brujula-dev/protocol/openid-connect/token})
 *   <li>{@code brujula.keycloak.registration-svc.client-id} — service-account client ID
 *   <li>{@code brujula.keycloak.registration-svc.client-secret} — secret (from env, never
 *       committed)
 * </ul>
 */
@Configuration
class KeycloakAdminApiConfiguration {

  /**
   * Dedicated {@link RestTemplate} for the Keycloak Admin API adapter.
   *
   * <p>Declared as a named bean to avoid conflicts with any other RestTemplate beans that may exist
   * in the context. Tests can bind {@code MockRestServiceServer} to this instance.
   */
  @Bean("keycloakAdminRestTemplate")
  RestTemplate keycloakAdminRestTemplate() {
    return new RestTemplate();
  }

  @Bean
  KeycloakUserProvisioningPort keycloakUserProvisioningPort(
      RestTemplate keycloakAdminRestTemplate,
      @Value("${brujula.keycloak.admin-base-url}") String adminBaseUrl,
      @Value("${brujula.keycloak.token-url}") String tokenUrl,
      @Value("${brujula.keycloak.registration-svc.client-id}") String clientId,
      @Value("${brujula.keycloak.registration-svc.client-secret}") String clientSecret) {
    return new KeycloakAdminApiAdapter(
        RestClient.builder(keycloakAdminRestTemplate),
        adminBaseUrl,
        tokenUrl,
        clientId,
        clientSecret);
  }
}
