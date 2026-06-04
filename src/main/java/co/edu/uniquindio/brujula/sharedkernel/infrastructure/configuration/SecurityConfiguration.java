package co.edu.uniquindio.brujula.sharedkernel.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Fase 0.
 *
 * <p>Permissive configuration allowing access to actuator health endpoints and Swagger UI.
 * Real OIDC authentication (Keycloak) and CSRF enforcement are wired in Fase 1.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                authorize ->
                        authorize
                                // Actuator health is public
                                .requestMatchers("/actuator/health/**")
                                .permitAll()
                                // Swagger UI and OpenAPI docs (local profile only, enabled via config)
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",
                                        "/v3/api-docs")
                                .permitAll()
                                // All other requests require authentication (Fase 1 wires OIDC)
                                .anyRequest()
                                .authenticated())
                // CSRF seam: disabled for Fase 0 (no real write endpoints active)
                // Fase 1 enables cookie-based CSRF via CookieCsrfTokenRepository
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
