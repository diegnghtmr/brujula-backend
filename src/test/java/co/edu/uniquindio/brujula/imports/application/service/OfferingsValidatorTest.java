package co.edu.uniquindio.brujula.imports.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OfferingsValidator}. */
class OfferingsValidatorTest {

  private OfferingsValidator validator;

  @BeforeEach
  void setUp() {
    validator = new OfferingsValidator();
  }

  /** end_before_start produces END_BEFORE_START error. */
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

  /** missing period produces PERIOD_NOT_FOUND error. */
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

  /** invalid day produces INVALID_DAY error. */
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

  /** valid offering row produces no issues. */
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

  /** Malformed course (non-UUID) produces INVALID_COURSE_REFERENCE error. */
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

  /** Valid UUID course value produces no course-related issues. */
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
   * Same (course, option_label) on DIFFERENT weekdays must be VALID. These are two weekly sessions
   * of the same schedule option — NOT a duplicate. Before the fix, the seenOptionPairs guard emits
   * DUPLICATE_OPTION_IN_BATCH on the second row. After the fix, both rows pass with zero issues.
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
   * Same (course, option_label, day, start_time) is a TRUE duplicate session within the same
   * option. Must produce DUPLICATE_SESSION_IN_OPTION error. Two rows for the same option at the
   * same time is a scheduling conflict.
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

  // -------------------------------------------------------------------------
  // Interval-overlap detection within same (course, option, day)
  // -------------------------------------------------------------------------

  /**
   * Same (course, option_label, day), times 08:00-10:00 vs 09:00-11:00 — partial overlap →
   * DUPLICATE_SESSION_IN_OPTION on the second row.
   */
  @Test
  void overlapping_sessions_same_option_same_day_produces_duplicate_error() {
    String courseId = "550e8400-e29b-41d4-a716-446655440000";
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "period", "2026-1",
                    "course", courseId,
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
                    "course", courseId,
                    "option_label", "G1",
                    "journey", "J1",
                    "day", "MON",
                    "start_time", "09:00",
                    "end_time", "11:00",
                    "status", "ACTIVE")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    assertThat(issueMap)
        .as(
            "08:00-10:00 vs 09:00-11:00 on the same (course, option, day) overlap; "
                + "second row must produce DUPLICATE_SESSION_IN_OPTION")
        .containsKey(1);
    assertThat(issueMap.get(1))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("DUPLICATE_SESSION_IN_OPTION"));
  }

  /**
   * Same (course, option_label, day), times 08:00-10:00 vs 10:00-12:00 — adjacent (touching
   * endpoints, half-open intervals) → BOTH valid (no error).
   */
  @Test
  void adjacent_sessions_same_option_same_day_are_valid() {
    String courseId = "550e8400-e29b-41d4-a716-446655440000";
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "period", "2026-1",
                    "course", courseId,
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
                    "course", courseId,
                    "option_label", "G1",
                    "journey", "J1",
                    "day", "MON",
                    "start_time", "10:00",
                    "end_time", "12:00",
                    "status", "ACTIVE")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    assertThat(issueMap)
        .as(
            "08:00-10:00 and 10:00-12:00 are adjacent (touching endpoints, half-open); "
                + "they must NOT produce a conflict")
        .doesNotContainKey(0);
    assertThat(issueMap).doesNotContainKey(1);
  }

  /**
   * Same (course, option_label, day, start_time, end_time) exact duplicate →
   * DUPLICATE_SESSION_IN_OPTION. Existing behaviour preserved.
   */
  @Test
  void exact_duplicate_session_same_option_same_day_produces_error() {
    String courseId = "550e8400-e29b-41d4-a716-446655440000";
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "period", "2026-1",
                    "course", courseId,
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
                    "course", courseId,
                    "option_label", "G1",
                    "journey", "J1",
                    "day", "MON",
                    "start_time", "08:00",
                    "end_time", "10:00",
                    "status", "ACTIVE")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    assertThat(issueMap)
        .as("Exact duplicate (same start_time) must still produce DUPLICATE_SESSION_IN_OPTION")
        .containsKey(1);
    assertThat(issueMap.get(1))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("DUPLICATE_SESSION_IN_OPTION"));
  }

  /**
   * Overlapping times on DIFFERENT days → BOTH valid. Overlap is only flagged within the same
   * (course, option_label, day) group.
   */
  @Test
  void overlapping_times_different_days_same_option_are_valid() {
    String courseId = "550e8400-e29b-41d4-a716-446655440000";
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "period", "2026-1",
                    "course", courseId,
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
                    "course", courseId,
                    "option_label", "G1",
                    "journey", "J1",
                    "day", "WED",
                    "start_time", "09:00",
                    "end_time", "11:00",
                    "status", "ACTIVE")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    assertThat(issueMap)
        .as(
            "Overlapping time intervals on different days within the same option must both be valid"
                + " — overlap detection is scoped to the same (course, option_label, day) group")
        .doesNotContainKey(0);
    assertThat(issueMap).doesNotContainKey(1);
  }

  /**
   * Overlapping times on same day but DIFFERENT option_labels → BOTH valid. Overlap detection is
   * scoped to the same option group.
   */
  @Test
  void overlapping_times_different_option_labels_same_day_are_valid() {
    String courseId = "550e8400-e29b-41d4-a716-446655440000";
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "period", "2026-1",
                    "course", courseId,
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
                    "course", courseId,
                    "option_label", "G2",
                    "journey", "J1",
                    "day", "MON",
                    "start_time", "09:00",
                    "end_time", "11:00",
                    "status", "ACTIVE")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    assertThat(issueMap)
        .as(
            "Overlapping times with different option_labels on the same day must both be valid"
                + " — overlap is only detected within the same (course, option_label, day) group")
        .doesNotContainKey(0);
    assertThat(issueMap).doesNotContainKey(1);
  }

  /**
   * Fully-contained interval — row0 08:00-11:00, row1 09:00-09:30 on the same (course,
   * option_label, day) → DUPLICATE_SESSION_IN_OPTION on row1.
   *
   * <p>Exercises the case where one session is completely enclosed by another, confirming the
   * half-open overlap condition {@code newStart < existingEnd && newEnd > existingStart} catches
   * containment as well as partial overlap.
   */
  @Test
  void contained_session_inside_existing_session_produces_error() {
    String courseId = "550e8400-e29b-41d4-a716-446655440000";
    List<ImportRow> rows =
        List.of(
            new ImportRow(
                0,
                Map.of(
                    "period", "2026-1",
                    "course", courseId,
                    "option_label", "G1",
                    "journey", "J1",
                    "day", "MON",
                    "start_time", "08:00",
                    "end_time", "11:00",
                    "status", "ACTIVE")),
            new ImportRow(
                1,
                Map.of(
                    "period", "2026-1",
                    "course", courseId,
                    "option_label", "G1",
                    "journey", "J1",
                    "day", "MON",
                    "start_time", "09:00",
                    "end_time", "09:30",
                    "status", "ACTIVE")));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(rows);

    assertThat(issueMap)
        .as(
            "09:00-09:30 is fully contained inside 08:00-11:00 on the same (course, option, day); "
                + "second row must produce DUPLICATE_SESSION_IN_OPTION")
        .containsKey(1);
    assertThat(issueMap.get(1))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("DUPLICATE_SESSION_IN_OPTION"));
  }

  /**
   * 3 rows same (course, option_label) on MON, WED, FRI → all valid. This is the core multi-session
   * grouping scenario.
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
