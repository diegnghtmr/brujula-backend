package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Flyway V015 creates {@code progress.student_program_subscription} and {@code
 * progress.course_attempt} tables with correct columns, constraints, and indexes.
 *
 * <p>Uses {@link AbstractPostgresIntegrationTest} (not {@code @DataJpaTest}).
 */
@SpringBootTest
class FlywayV015ProgressTablesTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  // -------------------------------------------------------------------------
  // student_program_subscription
  // -------------------------------------------------------------------------

  @Test
  void progress_student_program_subscription_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'progress'
              AND table_name = 'student_program_subscription'
            """,
            Integer.class);

    assertThat(count)
        .as("progress.student_program_subscription must exist after V015 migration")
        .isEqualTo(1);
  }

  @Test
  void progress_student_program_subscription_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'progress'
              AND table_name = 'student_program_subscription'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("progress.student_program_subscription must have all required columns")
        .contains(
            "id",
            "user_id",
            "program_id",
            "plan_id",
            "reference_journey_code",
            "status",
            "created_at",
            "version");
  }

  @Test
  void progress_student_program_subscription_must_not_have_is_primary_column() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'progress'
              AND table_name = 'student_program_subscription'
            """,
            String.class);

    assertThat(columns)
        .as(
            "progress.student_program_subscription must NOT have is_primary column (contract alignment #1630)")
        .doesNotContain("is_primary");
  }

  @Test
  void progress_student_program_subscription_has_index_on_user_id_and_status() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_indexes
            WHERE schemaname = 'progress'
              AND tablename = 'student_program_subscription'
              AND indexdef ILIKE '%user_id%'
              AND indexdef ILIKE '%status%'
            """,
            Integer.class);

    assertThat(count)
        .as("progress.student_program_subscription must have an index on (user_id, status)")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void progress_student_program_subscription_status_check_constraint_rejects_invalid_value() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    """
                    INSERT INTO progress.student_program_subscription
                      (id, user_id, program_id, plan_id, reference_journey_code, status)
                    VALUES
                      ('11111111-aaaa-4000-8000-000000000001',
                       '22222222-aaaa-4000-8000-000000000001',
                       '33333333-aaaa-4000-8000-000000000001',
                       '44444444-aaaa-4000-8000-000000000001',
                       'DIURNA',
                       'INVALID_STATUS')
                    """))
        .as("status CHECK constraint must reject values outside ('ACTIVE','DELETED')")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void progress_student_program_subscription_accepts_active_and_deleted_status() {
    jdbcTemplate.execute(
        """
        INSERT INTO progress.student_program_subscription
          (id, user_id, program_id, plan_id, reference_journey_code, status)
        VALUES
          ('11111111-bbbb-4000-8000-000000000001',
           '22222222-bbbb-4000-8000-000000000001',
           '33333333-bbbb-4000-8000-000000000001',
           '44444444-bbbb-4000-8000-000000000001',
           'DIURNA',
           'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """);

    jdbcTemplate.execute(
        """
        INSERT INTO progress.student_program_subscription
          (id, user_id, program_id, plan_id, reference_journey_code, status)
        VALUES
          ('11111111-cccc-4000-8000-000000000001',
           '22222222-bbbb-4000-8000-000000000001',
           '33333333-bbbb-4000-8000-000000000001',
           '44444444-bbbb-4000-8000-000000000001',
           'NOCTURNA',
           'DELETED')
        ON CONFLICT (id) DO NOTHING
        """);

    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM progress.student_program_subscription
            WHERE user_id = '22222222-bbbb-4000-8000-000000000001'
            """,
            Integer.class);

    assertThat(count).as("Both ACTIVE and DELETED status values must be accepted").isEqualTo(2);
  }

  // -------------------------------------------------------------------------
  // course_attempt
  // -------------------------------------------------------------------------

  @Test
  void progress_course_attempt_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'progress'
              AND table_name = 'course_attempt'
            """,
            Integer.class);

    assertThat(count).as("progress.course_attempt must exist after V015 migration").isEqualTo(1);
  }

  @Test
  void progress_course_attempt_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'progress'
              AND table_name = 'course_attempt'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("progress.course_attempt must have all required columns")
        .contains(
            "id",
            "subscription_id",
            "course_id",
            "status",
            "final_grade",
            "period_id",
            "journey_code",
            "registered_at",
            "version");
  }

  @Test
  void progress_course_attempt_has_index_on_subscription_id_course_id_status() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_indexes
            WHERE schemaname = 'progress'
              AND tablename = 'course_attempt'
              AND indexdef ILIKE '%subscription_id%'
              AND indexdef ILIKE '%course_id%'
              AND indexdef ILIKE '%status%'
            """,
            Integer.class);

    assertThat(count)
        .as("progress.course_attempt must have an index on (subscription_id, course_id, status)")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void progress_course_attempt_grade_status_check_rejects_passed_with_grade_below_3() {
    // Insert a subscription to use as FK
    jdbcTemplate.execute(
        """
        INSERT INTO progress.student_program_subscription
          (id, user_id, program_id, plan_id, reference_journey_code, status)
        VALUES
          ('55555555-aaaa-4000-8000-000000000001',
           '66666666-aaaa-4000-8000-000000000001',
           '77777777-aaaa-4000-8000-000000000001',
           '88888888-aaaa-4000-8000-000000000001',
           'DIURNA',
           'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """);

    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    """
                    INSERT INTO progress.course_attempt
                      (id, subscription_id, course_id, status, final_grade, journey_code)
                    VALUES
                      ('aaaaaaaa-1111-4000-8000-000000000001',
                       '55555555-aaaa-4000-8000-000000000001',
                       'bbbbbbbb-1111-4000-8000-000000000001',
                       'PASSED',
                       2.5,
                       'DIURNA')
                    """))
        .as("PASSED with grade < 3.0 must be rejected by attempt_grade_status_chk")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void progress_course_attempt_grade_status_check_rejects_cancelled_with_grade() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    """
                    INSERT INTO progress.course_attempt
                      (id, subscription_id, course_id, status, final_grade, journey_code)
                    VALUES
                      ('aaaaaaaa-2222-4000-8000-000000000001',
                       '55555555-aaaa-4000-8000-000000000001',
                       'bbbbbbbb-2222-4000-8000-000000000001',
                       'CANCELLED',
                       3.5,
                       'DIURNA')
                    """))
        .as("CANCELLED with a final_grade must be rejected by attempt_grade_status_chk")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void progress_course_attempt_accepts_passed_with_valid_grade() {
    jdbcTemplate.execute(
        """
        INSERT INTO progress.course_attempt
          (id, subscription_id, course_id, status, final_grade, journey_code)
        VALUES
          ('aaaaaaaa-3333-4000-8000-000000000001',
           '55555555-aaaa-4000-8000-000000000001',
           'bbbbbbbb-3333-4000-8000-000000000001',
           'PASSED',
           4.2,
           'DIURNA')
        ON CONFLICT (id) DO NOTHING
        """);

    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM progress.course_attempt
            WHERE id = 'aaaaaaaa-3333-4000-8000-000000000001'
              AND status = 'PASSED'
            """,
            Integer.class);

    assertThat(count).as("PASSED attempt with grade 4.2 must be accepted").isEqualTo(1);
  }

  @Test
  void progress_course_attempt_fk_to_subscription_enforced() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    """
                    INSERT INTO progress.course_attempt
                      (id, subscription_id, course_id, status, journey_code)
                    VALUES
                      ('aaaaaaaa-4444-4000-8000-000000000001',
                       'ffffffff-ffff-4000-8000-000000000001',
                       'bbbbbbbb-4444-4000-8000-000000000001',
                       'CANCELLED',
                       'DIURNA')
                    """))
        .as("FK to student_program_subscription must be enforced on course_attempt.subscription_id")
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
