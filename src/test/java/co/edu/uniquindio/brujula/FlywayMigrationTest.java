package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Flyway migration integration test.
 *
 * <p>Verifies that V001__create_schemas.sql creates all 10 PostgreSQL schemas on a fresh database,
 * the migration history table records exactly one successful entry, and Hibernate DDL auto is set
 * to validate.
 */
@SpringBootTest
class FlywayMigrationTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private Environment environment;

  @Test
  void v001_creates_all_10_schemas() {
    List<String> schemas =
        jdbcTemplate.queryForList(
            "SELECT schema_name FROM information_schema.schemata"
                + " WHERE schema_name IN"
                + " ('identity','administration','catalog','curriculum',"
                + "  'offering','progress','simulation','imports','audit','platform')"
                + " ORDER BY schema_name",
            String.class);

    assertThat(schemas)
        .as("All 10 Brujula schemas must exist after V001 migration")
        .containsExactlyInAnyOrder(
            "administration",
            "audit",
            "catalog",
            "curriculum",
            "identity",
            "imports",
            "offering",
            "platform",
            "progress",
            "simulation");
  }

  @Test
  void flyway_history_records_v001_as_successful() {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history"
                + " WHERE script = 'V001__create_schemas.sql'"
                + " AND success = true",
            Integer.class);

    assertThat(count)
        .as("flyway_schema_history must contain exactly one successful entry for V001")
        .isEqualTo(1);
  }

  @Test
  void hibernate_ddl_auto_is_validate() {
    String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");

    assertThat(ddlAuto)
        .as("Hibernate DDL auto must be 'validate', never 'create' or 'update'")
        .isIn("validate", "none");
  }
}
