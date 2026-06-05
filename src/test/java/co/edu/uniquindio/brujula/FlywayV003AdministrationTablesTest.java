package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED test for V003__create_administration_tables.sql.
 *
 * <p>Verifies that {@code administration.staff_account}, {@code administration.allowed_email_domain},
 * and {@code administration.staff_program_assignment} exist with correct columns and constraints.
 * Fails until the migration file exists.
 *
 * <p>Satisfies: S1-A-3, Spec §Admin Staff Account Management (RF-ADM-002, RN-127..132), Design §3
 * V003.
 */
@SpringBootTest
class FlywayV003AdministrationTablesTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void administration_staff_account_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'administration'
              AND table_name = 'staff_account'
            """,
            Integer.class);

    assertThat(count)
        .as("administration.staff_account table must exist after V003 migration")
        .isEqualTo(1);
  }

  @Test
  void administration_staff_account_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'administration'
              AND table_name = 'staff_account'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("administration.staff_account must have all required columns")
        .contains("id", "email", "full_name", "status", "version", "created_at", "updated_at");
  }

  @Test
  void administration_staff_account_has_unique_email_constraint() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu
              ON tc.constraint_name = ccu.constraint_name
              AND tc.table_schema = ccu.table_schema
            WHERE tc.table_schema = 'administration'
              AND tc.table_name = 'staff_account'
              AND tc.constraint_type = 'UNIQUE'
              AND ccu.column_name = 'email'
            """,
            Integer.class);

    assertThat(count)
        .as("administration.staff_account must have a UNIQUE constraint on email")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void administration_allowed_email_domain_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'administration'
              AND table_name = 'allowed_email_domain'
            """,
            Integer.class);

    assertThat(count)
        .as("administration.allowed_email_domain table must exist after V003 migration")
        .isEqualTo(1);
  }

  @Test
  void administration_allowed_email_domain_has_unique_domain_constraint() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu
              ON tc.constraint_name = ccu.constraint_name
              AND tc.table_schema = ccu.table_schema
            WHERE tc.table_schema = 'administration'
              AND tc.table_name = 'allowed_email_domain'
              AND tc.constraint_type = 'UNIQUE'
              AND ccu.column_name = 'domain'
            """,
            Integer.class);

    assertThat(count)
        .as("administration.allowed_email_domain must have a UNIQUE constraint on domain")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void administration_staff_program_assignment_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'administration'
              AND table_name = 'staff_program_assignment'
            """,
            Integer.class);

    assertThat(count)
        .as("administration.staff_program_assignment table must exist after V003 migration")
        .isEqualTo(1);
  }

  @Test
  void staff_program_assignment_has_fk_to_staff_account() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.referential_constraints rc
            JOIN information_schema.key_column_usage kcu
              ON rc.constraint_name = kcu.constraint_name
              AND rc.constraint_schema = kcu.constraint_schema
            JOIN information_schema.constraint_column_usage ccu
              ON rc.unique_constraint_name = ccu.constraint_name
              AND rc.unique_constraint_schema = ccu.constraint_schema
            WHERE kcu.table_schema = 'administration'
              AND kcu.table_name = 'staff_program_assignment'
              AND kcu.column_name = 'staff_id'
              AND ccu.table_name = 'staff_account'
            """,
            Integer.class);

    assertThat(count)
        .as(
            "administration.staff_program_assignment must have a FK staff_id → staff_account.id")
        .isGreaterThanOrEqualTo(1);
  }
}
