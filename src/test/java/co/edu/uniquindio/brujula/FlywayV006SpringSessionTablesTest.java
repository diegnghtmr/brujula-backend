package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that {@code platform.spring_session} and {@code platform.spring_session_attributes}
 * tables exist after Flyway migration, and that {@code spring.session.jdbc.initialize-schema} is
 * set to {@code never} (tables come from Flyway, not Spring Session DDL). Also probes the exact
 * {@code spring.session.jdbc.table-name} effective property key used by Spring Session 4 to reach
 * the schema-qualified table.
 *
 * <p>V006 originally created these tables as quoted uppercase identifiers. V020 renamed them to
 * lowercase (unquoted), so {@code information_schema} now reflects lowercase names. The {@code
 * spring.session.jdbc.table-name} config property retains the value {@code platform.SPRING_SESSION}
 * — Spring Session emits unquoted SQL, and PostgreSQL folds unquoted identifiers to lowercase at
 * runtime, matching V020's renamed tables.
 */
@SpringBootTest
class FlywayV006SpringSessionTablesTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private Environment environment;

  @Test
  void platform_spring_session_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'platform'
              AND table_name = 'spring_session'
            """,
            Integer.class);

    assertThat(count)
        .as("platform.spring_session table must exist after V006+V020 migration")
        .isEqualTo(1);
  }

  @Test
  void platform_spring_session_attributes_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'platform'
              AND table_name = 'spring_session_attributes'
            """,
            Integer.class);

    assertThat(count)
        .as("platform.spring_session_attributes table must exist after V006+V020 migration")
        .isEqualTo(1);
  }

  /**
   * Spring Session 4 uses {@code spring.session.jdbc.initialize-schema} to control DDL
   * auto-creation. Must be {@code never} so Flyway owns the schema.
   */
  @Test
  void spring_session_initialize_schema_is_never() {
    String initSchema = environment.getProperty("spring.session.jdbc.initialize-schema");

    assertThat(initSchema)
        .as(
            "spring.session.jdbc.initialize-schema must be 'never' — tables come from Flyway,"
                + " not Spring Session auto-DDL")
        .isEqualTo("never");
  }

  /**
   * Spring Session 4 uses {@code spring.session.jdbc.table-name} to reach a schema-qualified table.
   * The effective value must be {@code platform.SPRING_SESSION}.
   */
  @Test
  void spring_session_table_name_is_schema_qualified() {
    String tableName = environment.getProperty("spring.session.jdbc.table-name");

    assertThat(tableName)
        .as(
            "spring.session.jdbc.table-name must be 'platform.SPRING_SESSION' so Spring Session"
                + " writes to the correct schema")
        .isEqualTo("platform.SPRING_SESSION");
  }

  @Test
  void spring_session_primary_id_column_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = 'platform'
              AND table_name = 'spring_session'
              AND column_name = 'primary_id'
            """,
            Integer.class);

    assertThat(count)
        .as(
            "platform.spring_session must have primary_id column after V006+V020 migration"
                + " (V020 renamed column to lowercase)")
        .isEqualTo(1);
  }
}
