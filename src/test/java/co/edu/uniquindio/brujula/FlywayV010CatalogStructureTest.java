package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RED test for V010__create_catalog_tables.sql.
 *
 * <p>Verifies that the catalog schema tables {@code catalog.academic_program}, {@code
 * catalog.journey}, {@code catalog.study_plan}, and {@code catalog.program_journey} exist with
 * correct columns, constraints, and FK relationships after migration. Fails until the migration
 * file exists.
 *
 * <p>Satisfies: CAT-001, CAT-002, CAT-003, Design §5 V010.
 */
@SpringBootTest
class FlywayV010CatalogStructureTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  // -------------------------------------------------------------------------
  // academic_program
  // -------------------------------------------------------------------------

  @Test
  void catalog_academic_program_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'catalog'
              AND table_name = 'academic_program'
            """,
            Integer.class);

    assertThat(count)
        .as("catalog.academic_program table must exist after V010 migration")
        .isEqualTo(1);
  }

  @Test
  void catalog_academic_program_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'catalog'
              AND table_name = 'academic_program'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("catalog.academic_program must have all required columns")
        .contains("id", "name", "faculty", "status", "version", "created_at", "updated_at");
  }

  @Test
  void catalog_academic_program_has_pk_on_id() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
              AND tc.table_schema = kcu.table_schema
            WHERE tc.table_schema = 'catalog'
              AND tc.table_name = 'academic_program'
              AND tc.constraint_type = 'PRIMARY KEY'
              AND kcu.column_name = 'id'
            """,
            Integer.class);

    assertThat(count)
        .as("catalog.academic_program must have a PRIMARY KEY on id")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void catalog_academic_program_version_defaults_to_zero() {
    String defaultValue =
        jdbcTemplate.queryForObject(
            """
            SELECT column_default
            FROM information_schema.columns
            WHERE table_schema = 'catalog'
              AND table_name = 'academic_program'
              AND column_name = 'version'
            """,
            String.class);

    assertThat(defaultValue)
        .as("catalog.academic_program.version must default to 0")
        .isNotNull()
        .contains("0");
  }

  // -------------------------------------------------------------------------
  // journey
  // -------------------------------------------------------------------------

  @Test
  void catalog_journey_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'catalog'
              AND table_name = 'journey'
            """,
            Integer.class);

    assertThat(count).as("catalog.journey table must exist after V010 migration").isEqualTo(1);
  }

  @Test
  void catalog_journey_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'catalog'
              AND table_name = 'journey'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("catalog.journey must have all required columns")
        .contains("id", "code", "name", "active");
  }

  @Test
  void catalog_journey_code_is_unique() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu
              ON tc.constraint_name = ccu.constraint_name
              AND tc.table_schema = ccu.table_schema
            WHERE tc.table_schema = 'catalog'
              AND tc.table_name = 'journey'
              AND tc.constraint_type = 'UNIQUE'
              AND ccu.column_name = 'code'
            """,
            Integer.class);

    assertThat(count)
        .as("catalog.journey must have a UNIQUE constraint on code")
        .isGreaterThanOrEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // study_plan
  // -------------------------------------------------------------------------

  @Test
  void catalog_study_plan_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'catalog'
              AND table_name = 'study_plan'
            """,
            Integer.class);

    assertThat(count).as("catalog.study_plan table must exist after V010 migration").isEqualTo(1);
  }

  @Test
  void catalog_study_plan_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'catalog'
              AND table_name = 'study_plan'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("catalog.study_plan must have all required columns")
        .contains(
            "id",
            "program_id",
            "name",
            "status",
            "total_credits",
            "reference_year",
            "version",
            "created_at",
            "updated_at");
  }

  @Test
  void catalog_study_plan_has_fk_to_academic_program() {
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
            WHERE kcu.table_schema = 'catalog'
              AND kcu.table_name = 'study_plan'
              AND kcu.column_name = 'program_id'
              AND ccu.table_name = 'academic_program'
            """,
            Integer.class);

    assertThat(count)
        .as("catalog.study_plan must have a FK program_id → catalog.academic_program.id")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void catalog_study_plan_version_defaults_to_zero() {
    String defaultValue =
        jdbcTemplate.queryForObject(
            """
            SELECT column_default
            FROM information_schema.columns
            WHERE table_schema = 'catalog'
              AND table_name = 'study_plan'
              AND column_name = 'version'
            """,
            String.class);

    assertThat(defaultValue)
        .as("catalog.study_plan.version must default to 0")
        .isNotNull()
        .contains("0");
  }

  // -------------------------------------------------------------------------
  // program_journey
  // -------------------------------------------------------------------------

  @Test
  void catalog_program_journey_table_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'catalog'
              AND table_name = 'program_journey'
            """,
            Integer.class);

    assertThat(count)
        .as("catalog.program_journey table must exist after V010 migration")
        .isEqualTo(1);
  }

  @Test
  void catalog_program_journey_has_required_columns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'catalog'
              AND table_name = 'program_journey'
            ORDER BY column_name
            """,
            String.class);

    assertThat(columns)
        .as("catalog.program_journey must have program_id and journey_id columns")
        .contains("program_id", "journey_id");
  }

  @Test
  void catalog_program_journey_has_composite_pk() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.table_constraints
            WHERE table_schema = 'catalog'
              AND table_name = 'program_journey'
              AND constraint_type = 'PRIMARY KEY'
            """,
            Integer.class);

    assertThat(count)
        .as("catalog.program_journey must have a composite PRIMARY KEY")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void catalog_program_journey_has_fk_to_academic_program() {
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
            WHERE kcu.table_schema = 'catalog'
              AND kcu.table_name = 'program_journey'
              AND kcu.column_name = 'program_id'
              AND ccu.table_name = 'academic_program'
            """,
            Integer.class);

    assertThat(count)
        .as("catalog.program_journey must have a FK program_id → catalog.academic_program.id")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void catalog_program_journey_has_fk_to_journey() {
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
            WHERE kcu.table_schema = 'catalog'
              AND kcu.table_name = 'program_journey'
              AND kcu.column_name = 'journey_id'
              AND ccu.table_name = 'journey'
            """,
            Integer.class);

    assertThat(count)
        .as("catalog.program_journey must have a FK journey_id → catalog.journey.id")
        .isGreaterThanOrEqualTo(1);
  }
}
