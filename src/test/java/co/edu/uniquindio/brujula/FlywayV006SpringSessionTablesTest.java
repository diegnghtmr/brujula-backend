package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * G1 RESOLVE + RED test for V006__create_spring_session_tables.sql.
 *
 * <p>Verifies that {@code platform.SPRING_SESSION} and {@code platform.SPRING_SESSION_ATTRIBUTES}
 * tables exist after Flyway migration, and that {@code spring.session.jdbc.initialize-schema} is
 * set to {@code never} (tables come from Flyway, not Spring Session DDL). Also probes the exact
 * {@code spring.session.jdbc.table-name} effective property key used by Spring Session 4 to reach
 * the schema-qualified table.
 *
 * <p>Satisfies: S1-A-1 (G1), S1-A-6, Spec §Session Persistence (ADR-007, RNF-SEG-003/004), Design
 * §3 V006.
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
              AND table_name = 'SPRING_SESSION'
            """,
            Integer.class);

    assertThat(count)
        .as("platform.SPRING_SESSION table must exist after V006 migration")
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
              AND table_name = 'SPRING_SESSION_ATTRIBUTES'
            """,
            Integer.class);

    assertThat(count)
        .as("platform.SPRING_SESSION_ATTRIBUTES table must exist after V006 migration")
        .isEqualTo(1);
  }

  /**
   * G1 RESOLUTION: Spring Session 4 uses {@code spring.session.jdbc.initialize-schema} to control
   * DDL auto-creation. Must be {@code never} so Flyway owns the schema.
   */
  @Test
  void spring_session_initialize_schema_is_never() {
    String initSchema = environment.getProperty("spring.session.jdbc.initialize-schema");

    assertThat(initSchema)
        .as(
            "spring.session.jdbc.initialize-schema must be 'never' — tables come from Flyway,"
                + " not Spring Session auto-DDL (ADR-F1-004)")
        .isEqualTo("never");
  }

  /**
   * G1 RESOLUTION: Spring Session 4 uses {@code spring.session.jdbc.table-name} to reach a
   * schema-qualified table. The effective value must be {@code platform.SPRING_SESSION}.
   */
  @Test
  void spring_session_table_name_is_schema_qualified() {
    String tableName = environment.getProperty("spring.session.jdbc.table-name");

    assertThat(tableName)
        .as(
            "spring.session.jdbc.table-name must be 'platform.SPRING_SESSION' so Spring Session"
                + " writes to the correct schema (G1 resolution)")
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
              AND table_name = 'SPRING_SESSION'
              AND column_name = 'PRIMARY_ID'
            """,
            Integer.class);

    assertThat(count)
        .as("platform.SPRING_SESSION must have PRIMARY_ID column (canonical Spring Session DDL)")
        .isEqualTo(1);
  }
}
