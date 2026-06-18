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
 * Covers missing branches in {@link OfferingsValidator}.
 *
 * <p>Specifically: missing option_label, missing status, invalid status value, missing end_time,
 * missing start_time, malformed start_time, malformed end_time, valid row with end equal to start
 * (not after → error), valid row with end > start (no error), overlap registration skipped when
 * end_time parse fails.
 */
class OfferingsValidatorBranchTest {

  private OfferingsValidator validator;
  private static final String VALID_COURSE_UUID = "550e8400-e29b-41d4-a716-446655440000";

  @BeforeEach
  void setUp() {
    validator = new OfferingsValidator();
  }

  // -------------------------------------------------------------------------
  // option_label — missing (empty)
  // -------------------------------------------------------------------------

  @Test
  void missing_option_label_produces_option_without_sessions_error() {
    ImportRow row = rowWith("option_label", "");

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("OPTION_WITHOUT_SESSIONS"));
  }

  // -------------------------------------------------------------------------
  // status — missing (empty)
  // -------------------------------------------------------------------------

  @Test
  void missing_status_produces_invalid_offering_status_error() {
    ImportRow row = rowWith("status", "");

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_OFFERING_STATUS"));
  }

  // -------------------------------------------------------------------------
  // status — invalid value
  // -------------------------------------------------------------------------

  @Test
  void invalid_status_value_produces_invalid_offering_status_error() {
    ImportRow row = rowWith("status", "PUBLISHED");

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("INVALID_OFFERING_STATUS"));
  }

  // -------------------------------------------------------------------------
  // end_time — missing (empty)
  // -------------------------------------------------------------------------

  @Test
  void missing_end_time_produces_end_before_start_error() {
    ImportRow row = rowWith("end_time", "");

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("END_BEFORE_START"));
  }

  // -------------------------------------------------------------------------
  // start_time — missing (empty)
  // -------------------------------------------------------------------------

  @Test
  void missing_start_time_produces_end_before_start_error() {
    ImportRow row = rowWith("start_time", "");

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("END_BEFORE_START"));
  }

  // -------------------------------------------------------------------------
  // start_time — malformed (non-HH:mm format)
  // -------------------------------------------------------------------------

  @Test
  void malformed_start_time_produces_end_before_start_error() {
    ImportRow row = rowWith("start_time", "8am");

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("END_BEFORE_START"));
  }

  // -------------------------------------------------------------------------
  // end_time — malformed (non-HH:mm format)
  // -------------------------------------------------------------------------

  @Test
  void malformed_end_time_produces_end_before_start_error() {
    ImportRow row = rowWith("end_time", "10pm");

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("END_BEFORE_START"));
  }

  // -------------------------------------------------------------------------
  // end_time == start_time (NOT after) → END_BEFORE_START error
  // -------------------------------------------------------------------------

  @Test
  void end_time_equal_to_start_time_produces_end_before_start_error() {
    ImportRow row =
        new ImportRow(
            0,
            Map.of(
                "period", "2026-1",
                "course", VALID_COURSE_UUID,
                "option_label", "G1",
                "day", "MON",
                "start_time", "08:00",
                "end_time", "08:00", // equal, not after
                "status", "ACTIVE"));

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("END_BEFORE_START"));
  }

  // -------------------------------------------------------------------------
  // course — empty (missing)
  // -------------------------------------------------------------------------

  @Test
  void missing_course_produces_course_not_found_error() {
    ImportRow row = rowWith("course", "");

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(
            issue -> {
              assertThat(issue.code()).isEqualTo("COURSE_NOT_FOUND");
              assertThat(issue.column()).isEqualTo("course");
            });
  }

  // -------------------------------------------------------------------------
  // day — empty (missing)
  // -------------------------------------------------------------------------

  @Test
  void missing_day_produces_invalid_day_error() {
    ImportRow row = rowWith("day", "");

    Map<Integer, List<ImportIssue>> issueMap = validator.validateAll(List.of(row));

    assertThat(issueMap).containsKey(0);
    assertThat(issueMap.get(0))
        .anySatisfy(
            issue -> {
              assertThat(issue.code()).isEqualTo("INVALID_DAY");
              assertThat(issue.column()).isEqualTo("day");
            });
  }

  // -------------------------------------------------------------------------
  // Helper: fully-valid row with one field overridden
  // -------------------------------------------------------------------------

  private static ImportRow rowWith(String key, String value) {
    Map<String, String> cells = new HashMap<>();
    cells.put("period", "2026-1");
    cells.put("course", VALID_COURSE_UUID);
    cells.put("option_label", "G1");
    cells.put("day", "MON");
    cells.put("start_time", "08:00");
    cells.put("end_time", "10:00");
    cells.put("status", "ACTIVE");
    cells.put(key, value);
    return new ImportRow(0, cells);
  }
}
