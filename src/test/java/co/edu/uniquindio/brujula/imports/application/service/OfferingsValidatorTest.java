package co.edu.uniquindio.brujula.imports.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OfferingsValidator} (T3.2). */
class OfferingsValidatorTest {

  private OfferingsValidator validator;

  @BeforeEach
  void setUp() {
    validator = new OfferingsValidator();
  }

  /** T3.2: end_before_start produces END_BEFORE_START error. */
  @Test
  void end_before_start_produces_error() {
    ImportRow row =
        new ImportRow(
            0,
            Map.of(
                "period", "2026-1",
                "course", "MAT101",
                "option_label", "G1",
                "journey", "J1",
                "day", "MON",
                "start_time", "10:00",
                "end_time", "08:00",
                "status", "ACTIVE"));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("END_BEFORE_START"));
  }

  /** T3.2: missing period produces PERIOD_NOT_FOUND error. */
  @Test
  void period_not_found_produces_period_not_found_error() {
    ImportRow row =
        new ImportRow(
            0,
            Map.of(
                "period", "",
                "course", "MAT101",
                "option_label", "G1",
                "journey", "J1",
                "day", "MON",
                "start_time", "08:00",
                "end_time", "10:00",
                "status", "ACTIVE"));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("PERIOD_NOT_FOUND"));
  }

  /** T3.2: invalid day produces INVALID_DAY error. */
  @Test
  void invalid_day_produces_error() {
    ImportRow row =
        new ImportRow(
            0,
            Map.of(
                "period", "2026-1",
                "course", "MAT101",
                "option_label", "G1",
                "journey", "J1",
                "day", "SUNDAY", // invalid; should be SUN
                "start_time", "08:00",
                "end_time", "10:00",
                "status", "ACTIVE"));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_DAY"));
  }

  /** T3.2: valid offering row produces no issues. */
  @Test
  void valid_offering_row_produces_no_issues() {
    ImportRow row =
        new ImportRow(
            0,
            Map.of(
                "period", "2026-1",
                "course", "550e8400-e29b-41d4-a716-446655440000",
                "option_label", "G1",
                "journey", "J1",
                "day", "MON",
                "start_time", "08:00",
                "end_time", "10:00",
                "status", "ACTIVE"));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).doesNotContainKey(0);
  }

  /** W1/S2: malformed course (non-UUID) produces INVALID_COURSE_REFERENCE error. */
  @Test
  void malformed_course_uuid_produces_invalid_course_reference_error() {
    ImportRow row =
        new ImportRow(
            0,
            Map.of(
                "period", "2026-1",
                "course", "NOT-A-UUID",
                "option_label", "G1",
                "journey", "J1",
                "day", "MON",
                "start_time", "08:00",
                "end_time", "10:00",
                "status", "ACTIVE"));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_COURSE_REFERENCE"));
  }

  /** W1/S2: valid UUID course value produces no course-related issues. */
  @Test
  void valid_uuid_course_produces_no_course_issues() {
    ImportRow row =
        new ImportRow(
            0,
            Map.of(
                "period", "2026-1",
                "course", "550e8400-e29b-41d4-a716-446655440000",
                "option_label", "G1",
                "journey", "J1",
                "day", "MON",
                "start_time", "08:00",
                "end_time", "10:00",
                "status", "ACTIVE"));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).doesNotContainKey(0);
  }

  /**
   * WU4-T1 (RED→GREEN): same (course, option_label) on DIFFERENT weekdays must be VALID. These are
   * two weekly sessions of the same schedule option — NOT a duplicate. Before the fix, the
   * seenOptionPairs guard emits DUPLICATE_OPTION_IN_BATCH on the second row. After the fix, both
   * rows pass with zero issues.
   */
  @Test
  void same_option_label_different_day_is_valid_multi_session() {
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "period", "2026-1",
                    "course", "550e8400-e29b-41d4-a716-446655440000",
                    "option_label", "G1",
                    "journey", "J1",
                    "day", "MON",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")),
            new ImportRow(
                1,
                Map.of(
                    "period", "2026-1",
                    "course", "550e8400-e29b-41d4-a716-446655440000",
                    "option_label", "G1",
                    "journey", "J1",
                    "day", "WED",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    assertThat(issueMap)
        .as(
            "Two rows sharing (course, option_label) on different weekdays must both be valid "
                + "— they represent sessions of the same schedule option, not duplicates")
        .doesNotContainKey(0);
    assertThat(issueMap).doesNotContainKey(1);
  }

  /**
   * WU4-T2 (RED→GREEN): same (course, option_label, day, start_time) is a TRUE duplicate session
   * within the same option. Must produce DUPLICATE_SESSION_IN_OPTION error. Two rows for the same
   * option at the same time is a scheduling conflict.
   */
  @Test
  void same_option_label_same_day_same_time_is_duplicate_session_error() {
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "period", "2026-1",
                    "course", "550e8400-e29b-41d4-a716-446655440000",
                    "option_label", "G1",
                    "journey", "J1",
                    "day", "MON",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")),
            new ImportRow(
                1,
                Map.of(
                    "period", "2026-1",
                    "course", "550e8400-e29b-41d4-a716-446655440000",
                    "option_label", "G1",
                    "journey", "J1",
                    "day", "MON",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    assertThat(issueMap)
        .as(
            "Two rows with identical (course, option_label, day, start_time) must "
                + "produce DUPLICATE_SESSION_IN_OPTION — same time-slot within same option is a conflict")
        .containsKey(1);
    assertThat(issueMap.get(1))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("DUPLICATE_SESSION_IN_OPTION"));
  }

  /**
   * WU4-T3 (RED→GREEN): 3 rows same (course, option_label) on MON, WED, FRI → all valid. This is
   * the core multi-session grouping scenario.
   */
  @Test
  void three_sessions_same_option_different_days_all_valid() {
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "period",
                    "2026-1",
                    "course",
                    "550e8400-e29b-41d4-a716-446655440000",
                    "option_label",
                    "G1",
                    "journey",
                    "J1",
                    "day",
                    "MON",
                    "start_time",
                    "08:00",
                    "end_time",
                    "10:00",
                    "status",
                    "ACTIVE")),
            new ImportRow(
                1,
                Map.of(
                    "period",
                    "2026-1",
                    "course",
                    "550e8400-e29b-41d4-a716-446655440000",
                    "option_label",
                    "G1",
                    "journey",
                    "J1",
                    "day",
                    "WED",
                    "start_time",
                    "08:00",
                    "end_time",
                    "10:00",
                    "status",
                    "ACTIVE")),
            new ImportRow(
                2,
                Map.of(
                    "period",
                    "2026-1",
                    "course",
                    "550e8400-e29b-41d4-a716-446655440000",
                    "option_label",
                    "G1",
                    "journey",
                    "J1",
                    "day",
                    "FRI",
                    "start_time",
                    "08:00",
                    "end_time",
                    "10:00",
                    "status",
                    "ACTIVE")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    assertThat(issueMap)
        .as(
            "Three rows sharing (course, option_label) on MON/WED/FRI must all be valid "
                + "— they are three weekly sessions of the same schedule option")
        .doesNotContainKey(0);
    assertThat(issueMap).doesNotContainKey(1);
    assertThat(issueMap).doesNotContainKey(2);
  }
}
