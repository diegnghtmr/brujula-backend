package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED → GREEN test for V009__make_oidc_subject_nullable_in_app_user.sql.
 *
 * <p>Self-registered students (AppUser.register()) have no OIDC subject until their first OIDC
 * login. V002 defined oidc_subject as NOT NULL; V009 drops the NOT NULL constraint to allow null
 * oidc_subject for PENDING_VERIFICATION users.
 *
 * <p>The UNIQUE constraint on oidc_subject is preserved (two users cannot share the same subject
 * once assigned; null subjects do not violate the UNIQUE constraint in PostgreSQL).
 *
 * <p>Satisfies: S2-B-1 (oidc_subject nullable for self-registered students), Design §3 V009.
 */
@SpringBootTest
class FlywayV009OidcSubjectNullableTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void oidc_subject_column_allows_null_after_v009() {
    String isNullable =
        jdbcTemplate.queryForObject(
            """
            SELECT is_nullable
            FROM information_schema.columns
            WHERE table_schema = 'identity'
              AND table_name = 'app_user'
              AND column_name = 'oidc_subject'
            """,
            String.class);

    assertThat(isNullable)
        .as(
            "oidc_subject must be nullable after V009 (self-registered students have no OIDC subject)")
        .isEqualTo("YES");
  }

  /**
   * W-003/S-002: PostgreSQL allows multiple NULLs in a UNIQUE column (NULLS are not compared for
   * uniqueness by default). This is fundamental to the C-002 fix — self-registered students all
   * have oidc_subject=NULL until their first OIDC login. Inserting two rows with NULL must not
   * violate the unique constraint.
   *
   * <p>If the constraint were changed to UNIQUE NULLS NOT DISTINCT (Postgres 15+), this test would
   * catch the regression.
   */
  @Test
  void multiple_null_oidc_subjects_do_not_violate_unique_constraint() {
    // Insert two app_user rows with oidc_subject=NULL and distinct emails
    String email1 = "multi-null-1-" + UUID.randomUUID() + "@uniquindio.edu.co";
    String email2 = "multi-null-2-" + UUID.randomUUID() + "@uniquindio.edu.co";
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    assertThatCode(
            () -> {
              jdbcTemplate.update(
                  """
                  INSERT INTO identity.app_user
                      (id, oidc_subject, email, full_name, role, status, onboarding_completed,
                       onboarding_state, version)
                  VALUES (?::uuid, NULL, ?, 'Null Subject Test 1', 'STUDENT', 'ACTIVE', false,
                       'PENDING_VERIFICATION', 0)
                  """,
                  id1.toString(),
                  email1);

              jdbcTemplate.update(
                  """
                  INSERT INTO identity.app_user
                      (id, oidc_subject, email, full_name, role, status, onboarding_completed,
                       onboarding_state, version)
                  VALUES (?::uuid, NULL, ?, 'Null Subject Test 2', 'STUDENT', 'ACTIVE', false,
                       'PENDING_VERIFICATION', 0)
                  """,
                  id2.toString(),
                  email2);
            })
        .as(
            "Two app_user rows with oidc_subject=NULL must not violate the UNIQUE constraint "
                + "(PostgreSQL allows multiple NULLs in a UNIQUE column by default)")
        .doesNotThrowAnyException();

    // Verify both rows exist
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM identity.app_user WHERE id IN (?::uuid, ?::uuid)",
            Integer.class,
            id1.toString(),
            id2.toString());

    assertThat(count).isEqualTo(2);
  }

  @Test
  void oidc_subject_unique_constraint_still_exists_after_v009() {
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
        .as("oidc_subject UNIQUE constraint must still exist after V009")
        .isGreaterThanOrEqualTo(1);
  }
}
