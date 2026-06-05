package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

/**
 * HTTP-level smoke test for the Actuator health endpoints.
 *
 * <p>Satisfies BB-001 scenario "Health endpoints respond": asserts that {@code GET
 * /actuator/health} and {@code GET /actuator/health/readiness} return HTTP 200 and contain {@code
 * "status":"UP"}.
 *
 * <p>Uses {@code webEnvironment = RANDOM_PORT} so a real embedded Tomcat is started and real HTTP
 * requests are issued via {@link RestClient} (Spring Framework 7, replaces the removed
 * TestRestTemplate). No Docker Compose / Keycloak required — datasource is overridden to H2 and
 * Flyway is disabled so the test is self-contained.
 *
 * <p>Security note: The management endpoints are protected with HTTP Basic by default in Boot 4.
 * Credentials are set in {@code application.yaml} ({@code actuator}/{@code actuator}); this test
 * passes them via {@link RestClient} default headers.
 *
 * <p>Establishes the web-test pattern for Fase 1 feature tests.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.flyway.enabled=false",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.datasource.url=jdbc:h2:mem:health-test;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
    })
class ActuatorHealthHttpTest {

  @LocalServerPort private int port;

  @Value("${spring.security.user.name:actuator}")
  private String actuatorUser;

  @Value("${spring.security.user.password:actuator}")
  private String actuatorPassword;

  private RestClient restClient() {
    return RestClient.builder()
        .baseUrl("http://localhost:" + port)
        .defaultHeaders(headers -> headers.setBasicAuth(actuatorUser, actuatorPassword))
        .build();
  }

  @Test
  void actuator_health_returns_200_and_status_up() {
    var response = restClient().get().uri("/actuator/health").retrieve().toEntity(String.class);

    assertThat(response.getStatusCode().value())
        .as("GET /actuator/health must return HTTP 200")
        .isEqualTo(200);
    assertThat(response.getBody())
        .as("Health response body must contain status UP")
        .contains("\"status\":\"UP\"");
  }

  @Test
  void actuator_health_readiness_returns_200_and_status_up() {
    var response =
        restClient().get().uri("/actuator/health/readiness").retrieve().toEntity(String.class);

    assertThat(response.getStatusCode().value())
        .as("GET /actuator/health/readiness must return HTTP 200")
        .isEqualTo(200);
    assertThat(response.getBody())
        .as("Readiness probe body must contain status UP")
        .contains("\"status\":\"UP\"");
  }
}
