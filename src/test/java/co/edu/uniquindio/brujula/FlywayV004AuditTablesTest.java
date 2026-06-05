package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED test for V004__create_audit_tables.sql.
 *
 * <p>Verifies that {@code audit.audit_event} exists with required columns, JSONB fields, and
 * indexes. Fails until the migration file exists.
 *
 * <p>Satisfies: S1-A-4, Spec §Audit Log (RN-010, RNF-SEG-007), Design §3 V004.
 */
@SpringBootTest
class FlywayV004AuditTablesTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void audit_audit_event_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'audit'
              AND table_name = 'audit_event'
            """,
            Integer.class);

    assertThat(count).as("audit.audit_event table must exist after V004 migration").isEqualTo(1);
  }

  @Test
  void audit_audit_event_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'audit'
              AND table_name = 'audit_event'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("audit.audit_event must have all required columns")
        .contains(
            "id",
            "actor_id",
            "actor_role",
            "action",
            "entity_type",
            "entity_id",
            "program_id",
            "occurred_at",
            "previous_value",
            "new_value");
  }

  @Test
  void audit_audit_event_previous_value_is_jsonb() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = 'audit'
              AND table_name = 'audit_event'
              AND column_name = 'previous_value'
              AND udt_name = 'jsonb'
            """,
            Integer.class);

    assertThat(count).as("audit.audit_event.previous_value must be of type jsonb").isEqualTo(1);
  }

  @Test
  void audit_audit_event_new_value_is_jsonb() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = 'audit'
              AND table_name = 'audit_event'
              AND column_name = 'new_value'
              AND udt_name = 'jsonb'
            """,
            Integer.class);

    assertThat(count).as("audit.audit_event.new_value must be of type jsonb").isEqualTo(1);
  }

  @Test
  void audit_audit_event_has_index_on_actor_id() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_indexes
            WHERE schemaname = 'audit'
              AND tablename = 'audit_event'
              AND indexdef ILIKE '%actor_id%'
            """,
            Integer.class);

    assertThat(count)
        .as("audit.audit_event must have an index on actor_id")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void audit_audit_event_has_index_on_occurred_at() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_indexes
            WHERE schemaname = 'audit'
              AND tablename = 'audit_event'
              AND indexdef ILIKE '%occurred_at%'
            """,
            Integer.class);

    assertThat(count)
        .as("audit.audit_event must have an index on occurred_at")
        .isGreaterThanOrEqualTo(1);
  }
}
