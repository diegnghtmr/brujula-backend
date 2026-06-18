package co.edu.uniquindio.brujula.imports.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers missing branches in {@link CoursesValidator}.
 *
 * <p>Specifically: missing/empty credits field, missing/empty weekly_hours field, missing/empty
 * recommended_semester field, missing/empty type field, non-numeric weekly_hours, non-numeric
 * recommended_semester, out-of-range weekly_hours, out-of-range recommended_semester, new
 * official_code registered (not duplicate), missing journey field.
 */
class CoursesValidatorBranchTest {

  private CoursesValidator validator;

  @BeforeEach
  void setUp() {
    validator = new CoursesValidator();
  }

  // -------------------------------------------------------------------------
  // credits — missing field (empty string → "credits is required")
  // -------------------------------------------------------------------------

  @Test
  void missing_credits_field_produces_invalid_credits_required_error() {
    ImportRow row = rowWith("credits", "");

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(
            issue -> {
              assertThat(issue.code()).isEqualTo("INVALID_CREDITS");
              assertThat(issue.column()).isEqualTo("credits");
            });
  }

  // -------------------------------------------------------------------------
  // credits — out-of-range (> MAX_CREDITS = 20)
  // -------------------------------------------------------------------------

  @Test
  void credits_above_max_produces_invalid_credits_error() {
    ImportRow row = rowWith("credits", "21");

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues).anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_CREDITS"));
  }

  // -------------------------------------------------------------------------
  // weekly_hours — missing field
  // -------------------------------------------------------------------------

  @Test
  void missing_weekly_hours_field_produces_invalid_weekly_hours_required_error() {
    ImportRow row = rowWith("weekly_hours", "");

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(
            issue -> {
              assertThat(issue.code()).isEqualTo("INVALID_WEEKLY_HOURS");
              assertThat(issue.column()).isEqualTo("weekly_hours");
            });
  }

  // -------------------------------------------------------------------------
  // weekly_hours — non-numeric
  // -------------------------------------------------------------------------

  @Test
  void non_numeric_weekly_hours_produces_invalid_weekly_hours_error() {
    ImportRow row = rowWith("weekly_hours", "not-a-number");

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_WEEKLY_HOURS"));
  }

  // -------------------------------------------------------------------------
  // weekly_hours — out-of-range (> MAX_WEEKLY_HOURS = 40)
  // -------------------------------------------------------------------------

  @Test
  void weekly_hours_above_max_produces_invalid_weekly_hours_error() {
    ImportRow row = rowWith("weekly_hours", "41");

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_WEEKLY_HOURS"));
  }

  // -------------------------------------------------------------------------
  // recommended_semester — missing field
  // -------------------------------------------------------------------------

  @Test
  void missing_recommended_semester_produces_invalid_recommended_semester_required_error() {
    ImportRow row = rowWith("recommended_semester", "");

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(
            issue -> {
              assertThat(issue.code()).isEqualTo("INVALID_RECOMMENDED_SEMESTER");
              assertThat(issue.column()).isEqualTo("recommended_semester");
            });
  }

  // -------------------------------------------------------------------------
  // recommended_semester — non-numeric
  // -------------------------------------------------------------------------

  @Test
  void non_numeric_recommended_semester_produces_error() {
    ImportRow row = rowWith("recommended_semester", "abc");

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_RECOMMENDED_SEMESTER"));
  }

  // -------------------------------------------------------------------------
  // recommended_semester — out-of-range (> MAX_SEMESTER = 10)
  // -------------------------------------------------------------------------

  @Test
  void recommended_semester_above_max_produces_error() {
    ImportRow row = rowWith("recommended_semester", "11");

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_RECOMMENDED_SEMESTER"));
  }

  // -------------------------------------------------------------------------
  // type — missing field (empty)
  // -------------------------------------------------------------------------

  @Test
  void missing_type_field_produces_invalid_course_type_required_error() {
    ImportRow row = rowWith("type", "");

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(
            issue -> {
              assertThat(issue.code()).isEqualTo("INVALID_COURSE_TYPE");
              assertThat(issue.column()).isEqualTo("type");
            });
  }

  // -------------------------------------------------------------------------
  // official_code — new unique code is registered without error
  // -------------------------------------------------------------------------

  @Test
  void new_unique_official_code_is_registered_and_produces_no_duplicate_error() {
    Map<String, Integer> seen = new HashMap<>();
    // Row 0: first occurrence of ISC-001 — must be registered, no duplicate error
    ImportRow row =
        new ImportRow(
            0,
            Map.of(
                "course_name", "Programación I",
                "credits", "3",
                "weekly_hours", "4",
                "recommended_semester", "1",
                "type", "OBLIGATORIA",
                "official_code", "ISC-001",
                "journey", "J1"));

    List<ImportIssue> issues = validator.validate(row, seen);

    // No DUPLICATE_COURSE_IN_BATCH error
    assertThat(issues)
        .noneSatisfy(issue -> assertThat(issue.code()).isEqualTo("DUPLICATE_COURSE_IN_BATCH"));
    // Code is now registered in seen map
    assertThat(seen).containsKey("ISC-001");
  }

  // -------------------------------------------------------------------------
  // journey — missing field
  // -------------------------------------------------------------------------

  @Test
  void missing_journey_field_produces_unrecognized_journey_error() {
    ImportRow row = rowWith("journey", "");

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(
            issue -> {
              assertThat(issue.code()).isEqualTo("UNRECOGNIZED_JOURNEY");
              assertThat(issue.column()).isEqualTo("journey");
            });
  }

  // -------------------------------------------------------------------------
  // Helper: build a row where all required fields are valid except the one being overridden
  // -------------------------------------------------------------------------

  private static ImportRow rowWith(String overrideKey, String overrideValue) {
    Map<String, String> cells = new HashMap<>();
    cells.put("course_name", "Cálculo I");
    cells.put("credits", "3");
    cells.put("weekly_hours", "4");
    cells.put("recommended_semester", "1");
    cells.put("type", "OBLIGATORIA");
    cells.put("journey", "J1");
    cells.put(overrideKey, overrideValue); // override the field under test
    return new ImportRow(0, cells);
  }
}
