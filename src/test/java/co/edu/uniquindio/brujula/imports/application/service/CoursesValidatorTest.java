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
 * Unit tests for {@link CoursesValidator} (T3.2).
 *
 * <p>Covers per-row validation, error code vocabulary, and duplicate detection.
 */
class CoursesValidatorTest {

  private CoursesValidator validator;

  @BeforeEach
  void setUp() {
    validator = new CoursesValidator();
  }

  /** T3.2 SC-002-A: blank course_name produces COURSE_NAME_MISSING error. */
  @Test
  void blank_course_name_produces_course_name_missing() {
    ImportRow row = new ImportRow(0, Map.of(
        "course_name", "",
        "credits", "3",
        "weekly_hours", "4",
        "recommended_semester", "1",
        "type", "OBLIGATORIA",
        "journey", "J1"
    ));

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(issue -> {
          assertThat(issue.code()).isEqualTo("COURSE_NAME_MISSING");
          assertThat(issue.column()).isEqualTo("course_name");
          assertThat(issue.isBlocking()).isTrue();
        });
  }

  /** T3.2: invalid credits (0) produces INVALID_CREDITS error. */
  @Test
  void invalid_credits_produces_invalid_credits_error() {
    ImportRow row = new ImportRow(0, Map.of(
        "course_name", "Cálculo I",
        "credits", "0",
        "weekly_hours", "4",
        "recommended_semester", "1",
        "type", "OBLIGATORIA",
        "journey", "J1"
    ));

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(issue -> {
          assertThat(issue.code()).isEqualTo("INVALID_CREDITS");
          assertThat(issue.isBlocking()).isTrue();
        });
  }

  /** T3.2: non-numeric credits produces INVALID_CREDITS error. */
  @Test
  void non_numeric_credits_produces_invalid_credits_error() {
    ImportRow row = new ImportRow(0, Map.of(
        "course_name", "Cálculo I",
        "credits", "abc",
        "weekly_hours", "4",
        "recommended_semester", "1",
        "type", "OBLIGATORIA",
        "journey", "J1"
    ));

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_CREDITS"));
  }

  /** T3.2: duplicate official_code within batch produces DUPLICATE_COURSE_IN_BATCH. */
  @Test
  void duplicate_official_code_in_batch_produces_duplicate_course_in_batch() {
    Map<String, Integer> seen = new HashMap<>();
    seen.put("MAT101", 1); // already seen at row 1

    ImportRow row = new ImportRow(1, Map.of(
        "course_name", "Cálculo II",
        "credits", "3",
        "weekly_hours", "4",
        "recommended_semester", "2",
        "type", "OBLIGATORIA",
        "official_code", "MAT101",
        "journey", "J1"
    ));

    List<ImportIssue> issues = validator.validate(row, seen);

    assertThat(issues)
        .anySatisfy(issue -> {
          assertThat(issue.code()).isEqualTo("DUPLICATE_COURSE_IN_BATCH");
          assertThat(issue.isBlocking()).isTrue();
        });
  }

  /** T3.2: valid row produces no issues. */
  @Test
  void valid_row_produces_no_issues() {
    ImportRow row = new ImportRow(0, Map.of(
        "course_name", "Cálculo I",
        "credits", "3",
        "weekly_hours", "4",
        "recommended_semester", "1",
        "type", "OBLIGATORIA",
        "journey", "J1"
    ));

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues).isEmpty();
  }

  /** T3.2: all rows invalid in batch → validateAll returns entries for all rows. */
  @Test
  void all_rows_invalid_returns_all_in_issue_map() {
    List<ImportRow> rows = List.of(
        new ImportRow(0, Map.of("course_name", "", "credits", "3", "weekly_hours", "4",
            "recommended_semester", "1", "type", "OBLIGATORIA", "journey", "J1")),
        new ImportRow(1, Map.of("course_name", "", "credits", "3", "weekly_hours", "4",
            "recommended_semester", "1", "type", "OBLIGATORIA", "journey", "J1"))
    );

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    assertThat(issueMap).containsKeys(0, 1);
  }

  /** T3.2: invalid course type produces INVALID_COURSE_TYPE. */
  @Test
  void invalid_course_type_produces_error() {
    ImportRow row = new ImportRow(0, Map.of(
        "course_name", "Cálculo I",
        "credits", "3",
        "weekly_hours", "4",
        "recommended_semester", "1",
        "type", "UNKNOWN_TYPE",
        "journey", "J1"
    ));

    List<ImportIssue> issues = validator.validate(row, new HashMap<>());

    assertThat(issues)
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_COURSE_TYPE"));
  }
}
