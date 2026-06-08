package co.edu.uniquindio.brujula.imports.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OfferingsValidator} (T3.2).
 */
class OfferingsValidatorTest {

  private OfferingsValidator validator;

  @BeforeEach
  void setUp() {
    validator = new OfferingsValidator();
  }

  /** T3.2: end_before_start produces END_BEFORE_START error. */
  @Test
  void end_before_start_produces_error() {
    ImportRow row = new ImportRow(0, Map.of(
        "period", "2026-1",
        "course", "MAT101",
        "option_label", "G1",
        "journey", "J1",
        "day", "MON",
        "start_time", "10:00",
        "end_time", "08:00",
        "status", "ACTIVE"
    ));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("END_BEFORE_START"));
  }

  /** T3.2: missing period produces PERIOD_NOT_FOUND error. */
  @Test
  void period_not_found_produces_period_not_found_error() {
    ImportRow row = new ImportRow(0, Map.of(
        "period", "",
        "course", "MAT101",
        "option_label", "G1",
        "journey", "J1",
        "day", "MON",
        "start_time", "08:00",
        "end_time", "10:00",
        "status", "ACTIVE"
    ));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("PERIOD_NOT_FOUND"));
  }

  /** T3.2: invalid day produces INVALID_DAY error. */
  @Test
  void invalid_day_produces_error() {
    ImportRow row = new ImportRow(0, Map.of(
        "period", "2026-1",
        "course", "MAT101",
        "option_label", "G1",
        "journey", "J1",
        "day", "SUNDAY", // invalid; should be SUN
        "start_time", "08:00",
        "end_time", "10:00",
        "status", "ACTIVE"
    ));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_DAY"));
  }

  /** T3.2: valid offering row produces no issues. */
  @Test
  void valid_offering_row_produces_no_issues() {
    ImportRow row = new ImportRow(0, Map.of(
        "period", "2026-1",
        "course", "MAT101",
        "option_label", "G1",
        "journey", "J1",
        "day", "MON",
        "start_time", "08:00",
        "end_time", "10:00",
        "status", "ACTIVE"
    ));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).doesNotContainKey(0);
  }

  /** T3.2: duplicate option_label for same course produces DUPLICATE_OPTION_IN_BATCH. */
  @Test
  void duplicate_option_label_for_same_course_produces_error() {
    List<ImportRow> rows = List.of(
        new ImportRow(0, Map.of(
            "period", "2026-1", "course", "MAT101", "option_label", "G1",
            "journey", "J1", "day", "MON", "start_time", "08:00",
            "end_time", "10:00", "status", "ACTIVE")),
        new ImportRow(1, Map.of(
            "period", "2026-1", "course", "MAT101", "option_label", "G1",
            "journey", "J1", "day", "TUE", "start_time", "08:00",
            "end_time", "10:00", "status", "ACTIVE"))
    );

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    assertThat(issueMap).containsKey(1);
    assertThat(issueMap.get(1))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("DUPLICATE_OPTION_IN_BATCH"));
  }
}
