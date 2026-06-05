package co.edu.uniquindio.brujula;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests requiring a PostgreSQL database.
 *
 * <p>Uses a true JVM-level singleton Testcontainers PostgreSQL container shared across ALL test
 * classes in the suite. The container is started once (lazily via {@code static} initializer),
 * reused by every subclass, and stopped at JVM shutdown by Testcontainers' own cleanup hook.
 *
 * <p>Why singleton (not {@code @Container static} per-subclass): Spring context caching de-dupes
 * contexts by their configuration fingerprint. If two test classes extend this base and each
 * declares its own {@code @Container static} field, Spring can only cache one context (both have
 * the same fingerprint from the inherited {@code @DynamicPropertySource}), so the second class
 * receives a context pointing to the first class's now-dead container port. A JVM-level singleton
 * avoids this by always returning the same {@code POSTGRES::getJdbcUrl} for all subclasses,
 * ensuring Spring shares one context safely.
 */
public abstract class AbstractPostgresIntegrationTest {

  // JVM-level singleton: started once, reused by all subclasses, stopped at JVM shutdown.
  // NOT managed via @Container/@Testcontainers — manual lifecycle for true cross-class sharing.
  static final PostgreSQLContainer<?> POSTGRES;

  static {
    POSTGRES =
        new PostgreSQLContainer<>("postgres:18.4")
            .withDatabaseName("planner_db")
            .withUsername("brujula")
            .withPassword("brujula");
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }
}
