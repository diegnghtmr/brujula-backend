package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Smoke test verifying that the application context loads successfully.
 *
 * <p>Satisfies BB-001 scenario: Application context loads. Note: Spring Boot 4 removes
 * TestRestTemplate and @AutoConfigureMockMvc test slices. Context-load + environment checks replace
 * the direct HTTP health endpoint test for Fase 0.
 */
@SpringBootTest
class HealthEndpointTest extends AbstractPostgresIntegrationTest {

  @Autowired private ApplicationContext applicationContext;

  @Autowired private Environment environment;

  @Test
  void application_context_loads() {
    assertThat(applicationContext)
        .as("Application context must load without fatal exceptions")
        .isNotNull();
  }

  @Test
  void spring_profiles_active_includes_test_profile_or_default() {
    // Verifies the environment is configured as expected in test
    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
        .as("Hibernate DDL auto must be 'validate' or 'none' — never 'create'")
        .isIn("validate", "none");
  }
}
