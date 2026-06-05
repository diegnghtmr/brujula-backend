package co.edu.uniquindio.brujula;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies that Swagger UI endpoints respect the local-profile guard.
 *
 * <p>Satisfies COD-001 scenarios:
 *
 * <ul>
 *   <li>"Swagger UI accessible on local profile only" — asserts /v3/api-docs returns 200 on local.
 *   <li>"Swagger UI disabled on non-local profiles" — separate nested class asserts 404 on default.
 * </ul>
 *
 * <p>Boot 4 note: TestRestTemplate and @AutoConfigureMockMvc were removed. MockMvc is built
 * manually from WebApplicationContext via MockMvcBuilders.webAppContextSetup().
 */
class SwaggerUiProfileGuardTest {

  /**
   * Local-profile slice: springdoc is enabled via application-local.yaml.
   *
   * <p>Uses a real PostgreSQL container (via AbstractPostgresIntegrationTest) because the full
   * Spring context is needed to load springdoc auto-configuration.
   *
   * <p>Fase 1 note: The local profile uses static Keycloak provider URIs (not issuer-uri discovery)
   * so tests can boot without a running Keycloak instance. See application.yaml §spring.security.
   */
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @ActiveProfiles("local")
  static class LocalProfileSwaggerEnabled extends AbstractPostgresIntegrationTest {

    @Autowired private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void swagger_api_docs_returns_200_on_local_profile() throws Exception {
      mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void swagger_ui_returns_200_on_local_profile() throws Exception {
      // Spring Boot redirects /swagger-ui/index.html; accept any 2xx or 3xx as "accessible"
      mockMvc
          .perform(get("/swagger-ui/index.html"))
          .andExpect(
              result -> {
                int status = result.getResponse().getStatus();
                if (status < 200 || status >= 400) {
                  throw new AssertionError(
                      "Expected Swagger UI to be accessible on local profile, but got HTTP "
                          + status);
                }
              });
    }
  }

  /**
   * Default-profile slice: springdoc is disabled via application.yaml (springdoc.api-docs.enabled:
   * false). Endpoints must not be registered.
   *
   * <p>Uses property overrides to disable Flyway and JPA datasource so no DB container is needed
   * for a config-level guard test.
   */
  @SpringBootTest(
      webEnvironment = SpringBootTest.WebEnvironment.MOCK,
      properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
      })
  static class DefaultProfileSwaggerDisabled {

    @Autowired private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void swagger_api_docs_returns_404_on_default_profile() throws Exception {
      mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }

    @Test
    void swagger_ui_returns_404_on_default_profile() throws Exception {
      mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }
  }
}
