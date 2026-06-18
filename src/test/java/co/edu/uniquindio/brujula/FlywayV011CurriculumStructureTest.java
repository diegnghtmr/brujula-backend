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
 * RED test for V011__create_curriculum_tables.sql.
 *
 * <p>Verifies that the curriculum schema tables {@code curriculum.course}, {@code
 * curriculum.course_recommended_semester}, {@code curriculum.course_requirement}, {@code
 * curriculum.sequential_track}, {@code curriculum.sequential_track_level}, {@code
 * curriculum.sequential_track_level_option}, and {@code curriculum.degree_work_modality} exist with
 * correct columns, FKs, and indexes after migration. Fails until the migration file exists.
 *
 * <p>Verifies curriculum schema: columns, FKs, indexes, and CHECK constraints.
 */
@SpringBootTest
class FlywayV011CurriculumStructureTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  // -------------------------------------------------------------------------
  // course
  // -------------------------------------------------------------------------

  @Test
  void curriculum_course_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'curriculum'
              AND table_name = 'course'
            """,
            Integer.class);

    assertThat(count).as("curriculum.course table must exist after V011 migration").isEqualTo(1);
  }

  @Test
  void curriculum_course_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'curriculum'
              AND table_name = 'course'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("curriculum.course must have all required columns")
        .contains(
            "id",
            "plan_id",
            "program_id",
            "official_code",
            "name",
            "type",
            "credits",
            "curricular_weekly_hours",
            "status",
            "version");
  }

  @Test
  void curriculum_course_has_index_on_plan_id_and_status() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_indexes
            WHERE schemaname = 'curriculum'
              AND tablename = 'course'
              AND indexdef ILIKE '%plan_id%'
              AND indexdef ILIKE '%status%'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.course must have an index on (plan_id, status)")
        .isGreaterThanOrEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // course_recommended_semester
  // -------------------------------------------------------------------------

  @Test
  void curriculum_course_recommended_semester_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'curriculum'
              AND table_name = 'course_recommended_semester'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.course_recommended_semester table must exist after V011 migration")
        .isEqualTo(1);
  }

  @Test
  void curriculum_course_recommended_semester_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'curriculum'
              AND table_name = 'course_recommended_semester'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("curriculum.course_recommended_semester must have course_id, journey_code, semester")
        .contains("course_id", "journey_code", "semester");
  }

  @Test
  void curriculum_course_recommended_semester_has_composite_pk() {
    // Verify (course_id, journey_code) is the PK
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints tc
            WHERE tc.table_schema = 'curriculum'
              AND tc.table_name = 'course_recommended_semester'
              AND tc.constraint_type = 'PRIMARY KEY'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.course_recommended_semester must have a PRIMARY KEY")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void curriculum_course_recommended_semester_has_fk_to_course() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.referential_constraints rc
            JOIN information_schema.key_column_usage kcu
              ON rc.constraint_name = kcu.constraint_name
              AND rc.constraint_schema = kcu.constraint_schema
            WHERE kcu.table_schema = 'curriculum'
              AND kcu.table_name = 'course_recommended_semester'
              AND kcu.column_name = 'course_id'
            """,
            Integer.class);

    assertThat(count)
        .as(
            "curriculum.course_recommended_semester must have a FK course_id → curriculum.course.id")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void curriculum_course_recommended_semester_has_index_on_journey_code_and_semester() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_indexes
            WHERE schemaname = 'curriculum'
              AND tablename = 'course_recommended_semester'
              AND indexdef ILIKE '%journey_code%'
              AND indexdef ILIKE '%semester%'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.course_recommended_semester must have an index on (journey_code, semester)")
        .isGreaterThanOrEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // course_requirement
  // -------------------------------------------------------------------------

  @Test
  void curriculum_course_requirement_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'curriculum'
              AND table_name = 'course_requirement'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.course_requirement table must exist after V011 migration")
        .isEqualTo(1);
  }

  @Test
  void curriculum_course_requirement_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'curriculum'
              AND table_name = 'course_requirement'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("curriculum.course_requirement must have all required columns")
        .contains(
            "id",
            "target_course_id",
            "type",
            "required_course_id",
            "required_credits",
            "simultaneity_allowed",
            "confidence",
            "inferred",
            "status",
            "version");
  }

  @Test
  void curriculum_course_requirement_has_fk_on_target_course_id() {
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
            WHERE kcu.table_schema = 'curriculum'
              AND kcu.table_name = 'course_requirement'
              AND kcu.column_name = 'target_course_id'
              AND ccu.table_name = 'course'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.course_requirement must have a FK target_course_id → curriculum.course.id")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void curriculum_course_requirement_has_index_on_target_course_id() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_indexes
            WHERE schemaname = 'curriculum'
              AND tablename = 'course_requirement'
              AND indexdef ILIKE '%target_course_id%'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.course_requirement must have an index on target_course_id")
        .isGreaterThanOrEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // sequential_track
  // -------------------------------------------------------------------------

  @Test
  void curriculum_sequential_track_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'curriculum'
              AND table_name = 'sequential_track'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.sequential_track table must exist after V011 migration")
        .isEqualTo(1);
  }

  @Test
  void curriculum_sequential_track_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'curriculum'
              AND table_name = 'sequential_track'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("curriculum.sequential_track must have all required columns")
        .contains("id", "plan_id", "kind", "label", "consecutive_simultaneity_allowed", "version");
  }

  // -------------------------------------------------------------------------
  // sequential_track_level
  // -------------------------------------------------------------------------

  @Test
  void curriculum_sequential_track_level_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'curriculum'
              AND table_name = 'sequential_track_level'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.sequential_track_level table must exist after V011 migration")
        .isEqualTo(1);
  }

  @Test
  void curriculum_sequential_track_level_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'curriculum'
              AND table_name = 'sequential_track_level'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("curriculum.sequential_track_level must have all required columns")
        .contains("id", "track_id", "level");
  }

  @Test
  void curriculum_sequential_track_level_has_fk_to_sequential_track() {
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
            WHERE kcu.table_schema = 'curriculum'
              AND kcu.table_name = 'sequential_track_level'
              AND kcu.column_name = 'track_id'
              AND ccu.table_name = 'sequential_track'
            """,
            Integer.class);

    assertThat(count)
        .as(
            "curriculum.sequential_track_level must have a FK track_id →"
                + " curriculum.sequential_track.id")
        .isGreaterThanOrEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // sequential_track_level_option
  // -------------------------------------------------------------------------

  @Test
  void curriculum_sequential_track_level_option_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'curriculum'
              AND table_name = 'sequential_track_level_option'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.sequential_track_level_option table must exist after V011 migration")
        .isEqualTo(1);
  }

  @Test
  void curriculum_sequential_track_level_option_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'curriculum'
              AND table_name = 'sequential_track_level_option'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("curriculum.sequential_track_level_option must have level_id and course_id columns")
        .contains("level_id", "course_id");
  }

  @Test
  void curriculum_sequential_track_level_option_has_fk_to_level() {
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
            WHERE kcu.table_schema = 'curriculum'
              AND kcu.table_name = 'sequential_track_level_option'
              AND kcu.column_name = 'level_id'
              AND ccu.table_name = 'sequential_track_level'
            """,
            Integer.class);

    assertThat(count)
        .as(
            "curriculum.sequential_track_level_option must have a FK level_id →"
                + " curriculum.sequential_track_level.id")
        .isGreaterThanOrEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // degree_work_modality
  // -------------------------------------------------------------------------

  @Test
  void curriculum_degree_work_modality_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'curriculum'
              AND table_name = 'degree_work_modality'
            """,
            Integer.class);

    assertThat(count)
        .as("curriculum.degree_work_modality table must exist after V011 migration")
        .isEqualTo(1);
  }

  @Test
  void curriculum_degree_work_modality_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'curriculum'
              AND table_name = 'degree_work_modality'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("curriculum.degree_work_modality must have all required columns")
        .contains("id", "plan_id", "name", "status", "version");
  }

  // -------------------------------------------------------------------------
  // FIX-1 (W-001): required_course_id FK must be RESTRICT / NO ACTION
  // -------------------------------------------------------------------------

  /**
   * RED test (FIX-1 / W-001): asserts that the delete rule on the {@code
   * course_requirement_required_fk} foreign key is {@code NO ACTION} or {@code RESTRICT}.
   *
   * <p>ON DELETE SET NULL would silently corrupt COURSE_PASSED requirements when the referenced
   * course is deleted. The design prescribes archiving via status, not row deletion.
   */
  @Test
  void curriculum_course_requirement_required_fk_delete_rule_is_restrict() {
    String deleteRule =
        jdbcTemplate.queryForObject(
            """
            SELECT rc.delete_rule
            FROM information_schema.referential_constraints rc
            JOIN information_schema.key_column_usage kcu
              ON rc.constraint_name = kcu.constraint_name
              AND rc.constraint_schema = kcu.constraint_schema
            WHERE kcu.table_schema = 'curriculum'
              AND kcu.table_name = 'course_requirement'
              AND kcu.column_name = 'required_course_id'
            """,
            String.class);

    assertThat(deleteRule)
        .as(
            "course_requirement.required_course_id FK must use NO ACTION/RESTRICT to prevent"
                + " silent nulling of COURSE_PASSED requirements (FIX-1 / W-001)")
        .isIn("NO ACTION", "RESTRICT");
  }

  // -------------------------------------------------------------------------
  // Type-coherence CHECK — COURSE_PASSED/SEQUENCE needs required_course_id
  // -------------------------------------------------------------------------

  /**
   * Asserts that inserting a row with {@code type='COURSE_PASSED'} and {@code
   * required_course_id=NULL} is rejected by the DB-level CHECK constraint.
   *
   * <p>A COURSE_PASSED requirement without a required course is semantically invalid.
   */
  @Test
  void
      curriculum_course_requirement_type_coherence_check_rejects_course_passed_without_required_id() {
    // Insert a parent course first so we can reference it as target_course_id
    jdbcTemplate.execute(
        """
        INSERT INTO curriculum.course
          (id, plan_id, program_id, name, type, status)
        VALUES
          ('aaaaaaaa-0001-0001-0001-000000000001',
           'bbbbbbbb-0001-0001-0001-000000000001',
           'cccccccc-0001-0001-0001-000000000001',
           'Test Course For Coherence Check',
           'REQUIRED',
           'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """);

    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    """
                    INSERT INTO curriculum.course_requirement
                      (id, target_course_id, type, required_course_id, status)
                    VALUES
                      ('dddddddd-0001-0001-0001-000000000001',
                       'aaaaaaaa-0001-0001-0001-000000000001',
                       'COURSE_PASSED',
                       NULL,
                       'ACTIVE')
                    """))
        .as(
            "INSERT of type=COURSE_PASSED with required_course_id=NULL must be rejected by"
                + " course_requirement_type_coherence_chk (FIX-2 / W-002)")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  /**
   * Complementary GREEN acceptance test for FIX-2: a valid COURSE_PASSED row with non-null
   * required_course_id must be accepted.
   */
  @Test
  void curriculum_course_requirement_type_coherence_check_accepts_course_passed_with_required_id() {
    // Ensure parent courses exist
    jdbcTemplate.execute(
        """
        INSERT INTO curriculum.course
          (id, plan_id, program_id, name, type, status)
        VALUES
          ('aaaaaaaa-0002-0002-0002-000000000002',
           'bbbbbbbb-0002-0002-0002-000000000002',
           'cccccccc-0002-0002-0002-000000000002',
           'Target Course Coherence',
           'REQUIRED',
           'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """);

    jdbcTemplate.execute(
        """
        INSERT INTO curriculum.course
          (id, plan_id, program_id, name, type, status)
        VALUES
          ('aaaaaaaa-0003-0003-0003-000000000003',
           'bbbbbbbb-0002-0002-0002-000000000002',
           'cccccccc-0002-0002-0002-000000000002',
           'Required Course Coherence',
           'REQUIRED',
           'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """);

    // This must NOT throw — COURSE_PASSED with non-null required_course_id is valid
    jdbcTemplate.execute(
        """
        INSERT INTO curriculum.course_requirement
          (id, target_course_id, type, required_course_id, status)
        VALUES
          ('dddddddd-0002-0002-0002-000000000002',
           'aaaaaaaa-0002-0002-0002-000000000002',
           'COURSE_PASSED',
           'aaaaaaaa-0003-0003-0003-000000000003',
           'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """);

    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.course_requirement
            WHERE id = 'dddddddd-0002-0002-0002-000000000002'
              AND type = 'COURSE_PASSED'
            """,
            Integer.class);

    assertThat(count)
        .as("A COURSE_PASSED row with non-null required_course_id must be inserted successfully")
        .isEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // UNIQUE(program_id, official_code) constraint existence + violation
  // -------------------------------------------------------------------------

  /**
   * Asserts that the {@code course_official_code_uq} unique constraint exists on {@code
   * curriculum.course}.
   */
  @Test
  void curriculum_course_official_code_is_unique_per_program_constraint_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints
            WHERE table_schema = 'curriculum'
              AND table_name = 'course'
              AND constraint_type = 'UNIQUE'
              AND constraint_name = 'course_official_code_uq'
            """,
            Integer.class);

    assertThat(count)
        .as(
            "curriculum.course must have unique constraint course_official_code_uq on"
                + " (program_id, official_code)")
        .isEqualTo(1);
  }

  /**
   * Asserts that inserting two courses with the same (program_id, official_code) pair is rejected.
   */
  @Test
  void curriculum_course_official_code_unique_constraint_rejects_duplicate_per_program() {
    jdbcTemplate.execute(
        """
        INSERT INTO curriculum.course
          (id, plan_id, program_id, official_code, name, type, status)
        VALUES
          ('eeeeeeee-0001-0001-0001-000000000001',
           'ffffffff-0001-0001-0001-000000000001',
           '11111111-0001-0001-0001-000000000001',
           'SIS-S1-INTRO',
           'Course A S-001',
           'REQUIRED',
           'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """);

    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    """
                    INSERT INTO curriculum.course
                      (id, plan_id, program_id, official_code, name, type, status)
                    VALUES
                      ('eeeeeeee-0002-0002-0002-000000000002',
                       'ffffffff-0001-0001-0001-000000000001',
                       '11111111-0001-0001-0001-000000000001',
                       'SIS-S1-INTRO',
                       'Course B S-001 duplicate code',
                       'REQUIRED',
                       'ACTIVE')
                    """))
        .as(
            "Duplicate (program_id, official_code) must be rejected by course_official_code_uq"
                + " (FIX-3 / S-001)")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  // -------------------------------------------------------------------------
  // FIX-4 (S-002): UNIQUE(track_id, level) constraint existence + violation
  // -------------------------------------------------------------------------

  /**
   * Asserts that the {@code sequential_track_level_uq} unique constraint exists on {@code
   * curriculum.sequential_track_level}.
   */
  @Test
  void curriculum_sequential_track_level_unique_constraint_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints
            WHERE table_schema = 'curriculum'
              AND table_name = 'sequential_track_level'
              AND constraint_type = 'UNIQUE'
              AND constraint_name = 'sequential_track_level_uq'
            """,
            Integer.class);

    assertThat(count)
        .as(
            "curriculum.sequential_track_level must have unique constraint"
                + " sequential_track_level_uq on (track_id, level) (FIX-4 / S-002)")
        .isEqualTo(1);
  }

  /** Asserts that inserting two levels with the same (track_id, level) pair is rejected. */
  @Test
  void curriculum_sequential_track_level_unique_constraint_rejects_duplicate_track_level() {
    jdbcTemplate.execute(
        """
        INSERT INTO curriculum.sequential_track
          (id, plan_id, kind, label, consecutive_simultaneity_allowed)
        VALUES
          ('22222222-0001-0001-0001-000000000001',
           'bbbbbbbb-9999-9999-9999-000000000001',
           'PERSONAL_ELECTIVE',
           'Test Electiva Track S-002',
           false)
        ON CONFLICT (id) DO NOTHING
        """);

    jdbcTemplate.execute(
        """
        INSERT INTO curriculum.sequential_track_level
          (id, track_id, level)
        VALUES
          ('33333333-0001-0001-0001-000000000001',
           '22222222-0001-0001-0001-000000000001',
           1)
        ON CONFLICT (id) DO NOTHING
        """);

    assertThatThrownBy(
            () ->
                jdbcTemplate.execute(
                    """
                    INSERT INTO curriculum.sequential_track_level
                      (id, track_id, level)
                    VALUES
                      ('33333333-0002-0002-0002-000000000002',
                       '22222222-0001-0001-0001-000000000001',
                       1)
                    """))
        .as(
            "Duplicate (track_id, level) must be rejected by sequential_track_level_uq"
                + " (FIX-4 / S-002)")
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
