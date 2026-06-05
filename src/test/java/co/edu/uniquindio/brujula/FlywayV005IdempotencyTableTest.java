package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED test for V005__create_idempotency_table.sql.
 *
 * <p>Verifies that {@code platform.idempotency_record} exists with composite PK {@code
 * (idempotency_key, operation)} and all required columns. Fails until the migration file exists.
 *
 * <p>Satisfies: S1-A-5, Spec §Idempotent Create (ADR-009, TRD §13), Design §3 V005.
 */
@SpringBootTest
class FlywayV005IdempotencyTableTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void platform_idempotency_record_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'platform'
              AND table_name = 'idempotency_record'
            """,
            Integer.class);

    assertThat(count)
        .as("platform.idempotency_record table must exist after V005 migration")
        .isEqualTo(1);
  }

  @Test
  void platform_idempotency_record_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'platform'
              AND table_name = 'idempotency_record'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("platform.idempotency_record must have all required columns")
        .contains(
            "idempotency_key",
            "operation",
            "request_hash",
            "response_status",
            "response_body",
            "created_at");
  }

  @Test
  void platform_idempotency_record_has_composite_pk() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
              AND tc.table_schema = kcu.table_schema
            WHERE tc.table_schema = 'platform'
              AND tc.table_name = 'idempotency_record'
              AND tc.constraint_type = 'PRIMARY KEY'
              AND kcu.column_name IN ('idempotency_key', 'operation')
            """,
            Integer.class);

    assertThat(count)
        .as("platform.idempotency_record must have a composite PK on (idempotency_key, operation)")
        .isEqualTo(2);
  }

  @Test
  void platform_idempotency_record_response_body_is_jsonb() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = 'platform'
              AND table_name = 'idempotency_record'
              AND column_name = 'response_body'
              AND udt_name = 'jsonb'
            """,
            Integer.class);

    assertThat(count)
        .as("platform.idempotency_record.response_body must be of type jsonb")
        .isEqualTo(1);
  }
}
