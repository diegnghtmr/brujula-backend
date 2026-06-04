package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Smoke test verifying that the application context loads and health endpoints respond correctly.
 *
 * <p>Satisfies BB-001 scenario: Health endpoints respond.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointTest extends AbstractPostgresIntegrationTest {

    @LocalServerPort private int port;

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void actuator_health_returns_up() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "http://localhost:" + port + "/actuator/health", String.class);

        assertThat(response.getStatusCode())
                .as("GET /actuator/health must return HTTP 200")
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .as("Health response must contain status UP")
                .contains("\"status\":\"UP\"");
    }

    @Test
    void actuator_health_readiness_returns_up() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "http://localhost:" + port + "/actuator/health/readiness", String.class);

        assertThat(response.getStatusCode())
                .as("GET /actuator/health/readiness must return HTTP 200")
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .as("Readiness response must contain status UP")
                .contains("\"status\":\"UP\"");
    }
}
