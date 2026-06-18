package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED test for V002__create_identity_tables.sql.
 *
 * <p>Verifies that {@code identity.app_user} is created with all required columns, constraints, and
 * indexes after the full Flyway migration run. Fails until the migration file exists.
 *
 * <p>Verifies identity provisioning schema: required columns, unique constraints.
 */
@SpringBootTest
class FlywayV002IdentityTablesTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void identity_app_user_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'identity'
              AND table_name = 'app_user'
            """,
            Integer.class);

    assertThat(count).as("identity.app_user table must exist after V002 migration").isEqualTo(1);
  }

  @Test
  void identity_app_user_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'identity'
              AND table_name = 'app_user'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("identity.app_user must have all required columns")
        .contains(
            "id",
            "oidc_subject",
            "email",
            "full_name",
            "role",
            "status",
            "onboarding_completed",
            "version",
            "created_at",
            "updated_at");
  }

  @Test
  void identity_app_user_has_unique_oidc_subject_constraint() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu
              ON tc.constraint_name = ccu.constraint_name
              AND tc.table_schema = ccu.table_schema
            WHERE tc.table_schema = 'identity'
              AND tc.table_name = 'app_user'
              AND tc.constraint_type = 'UNIQUE'
              AND ccu.column_name = 'oidc_subject'
            """,
            Integer.class);

    assertThat(count)
        .as("identity.app_user must have a UNIQUE constraint on oidc_subject")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void identity_app_user_has_unique_email_constraint() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu
              ON tc.constraint_name = ccu.constraint_name
              AND tc.table_schema = ccu.table_schema
            WHERE tc.table_schema = 'identity'
              AND tc.table_name = 'app_user'
              AND tc.constraint_type = 'UNIQUE'
              AND ccu.column_name = 'email'
            """,
            Integer.class);

    assertThat(count)
        .as("identity.app_user must have a UNIQUE constraint on email")
        .isGreaterThanOrEqualTo(1);
  }
}
