package co.edu.uniquindio.brujula.imports.application.service;

import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-kind validator for OFFERINGS import rows (IMP-008, IMP-002).
 *
 * <p>Validates each row against the OFFERINGS column mapping (ADR-014). Produces {@link
 * ImportIssue} entries with error codes from the spec vocabulary. Framework-free.
 *
 * <p>Wired as a plain bean in {@link
 * co.edu.uniquindio.brujula.imports.infrastructure.configuration.ImportsConfiguration}.
 */
public class OfferingsValidator {

  private static final Set<String> VALID_DAYS =
      Set.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

  private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "INACTIVE", "DRAFT");

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  /**
   * Validate all rows in an OFFERINGS batch.
   *
   * <p>Multiple rows sharing the same {@code (course, option_label)} are VALID when they carry
   * different weekday/time combinations — they represent multiple weekly sessions of the same
   * schedule option (QAL-IMP-001 / DEBT-F6-002). A conflict only exists when two rows within the
   * same option share an identical {@code (course, option_label, day, start_time)} tuple, which
   * would create overlapping/duplicate sessions.
   *
   * @param rows list of import rows
   * @return map of row index → issues for that row
   */
  public Map<Integer, List<ImportIssue>> validateAll(List<ImportRow> rows) {
    Map<Integer, List<ImportIssue>> result = new HashMap<>();
    // Track duplicate (course, option_label, day, start_time) tuples — session-level conflict.
    // A repeated (course, option_label) with a DIFFERENT (day, start_time) is a VALID additional
    // session of the same option, NOT a duplicate.
    Set<String> seenSessionKeys = new HashSet<>();

    for (ImportRow row : rows) {
      List<ImportIssue> issues = validateSingleRow(row, seenSessionKeys);
      if (!issues.isEmpty()) {
        result.put(row.index(), issues);
      }
    }
    return result;
  }

  private List<ImportIssue> validateSingleRow(ImportRow row, Set<String> seenSessionKeys) {
    List<ImportIssue> issues = new ArrayList<>();
    int rowNumber = row.index() + 1;
    Map<String, String> cells = row.cells();

    // period: required
    String period = cells.getOrDefault("period", "").trim();
    if (period.isEmpty()) {
      issues.add(ImportIssue.error(rowNumber, "period", "PERIOD_NOT_FOUND", "period is required."));
    }

    // course: required and must be a valid UUID
    String course = cells.getOrDefault("course", "").trim();
    if (course.isEmpty()) {
      issues.add(ImportIssue.error(rowNumber, "course", "COURSE_NOT_FOUND", "course is required."));
    } else {
      try {
        UUID.fromString(course);
      } catch (IllegalArgumentException e) {
        issues.add(
            ImportIssue.error(
                rowNumber,
                "course",
                "INVALID_COURSE_REFERENCE",
                "course '%s' is not a valid UUID.".formatted(course)));
      }
    }

    // option_label: required
    String optionLabel = cells.getOrDefault("option_label", "").trim();
    if (optionLabel.isEmpty()) {
      issues.add(
          ImportIssue.error(
              rowNumber, "option_label", "OPTION_WITHOUT_SESSIONS", "option_label is required."));
    }

    // day: required, must be in valid set
    String day = cells.getOrDefault("day", "").trim().toUpperCase();
    if (day.isEmpty()) {
      issues.add(ImportIssue.error(rowNumber, "day", "INVALID_DAY", "day is required."));
    } else if (!VALID_DAYS.contains(day)) {
      issues.add(
          ImportIssue.error(
              rowNumber,
              "day",
              "INVALID_DAY",
              "day '%s' is not valid. Allowed: %s".formatted(day, VALID_DAYS)));
    }

    // start_time: required, HH:mm
    String startTimeStr = cells.getOrDefault("start_time", "").trim();
    LocalTime startTime = null;
    if (startTimeStr.isEmpty()) {
      issues.add(
          ImportIssue.error(
              rowNumber, "start_time", "END_BEFORE_START", "start_time is required."));
    } else {
      try {
        startTime = LocalTime.parse(startTimeStr, TIME_FORMATTER);
      } catch (DateTimeParseException e) {
        issues.add(
            ImportIssue.error(
                rowNumber,
                "start_time",
                "END_BEFORE_START",
                "start_time must be in HH:mm format, got: " + startTimeStr));
      }
    }

    // end_time: required, HH:mm, must be after start_time
    String endTimeStr = cells.getOrDefault("end_time", "").trim();
    if (endTimeStr.isEmpty()) {
      issues.add(
          ImportIssue.error(rowNumber, "end_time", "END_BEFORE_START", "end_time is required."));
    } else {
      try {
        LocalTime endTime = LocalTime.parse(endTimeStr, TIME_FORMATTER);
        if (startTime != null && !endTime.isAfter(startTime)) {
          issues.add(
              ImportIssue.error(
                  rowNumber,
                  "end_time",
                  "END_BEFORE_START",
                  "end_time (%s) must be after start_time (%s)."
                      .formatted(endTimeStr, startTimeStr)));
        }
      } catch (DateTimeParseException e) {
        issues.add(
            ImportIssue.error(
                rowNumber,
                "end_time",
                "END_BEFORE_START",
                "end_time must be in HH:mm format, got: " + endTimeStr));
      }
    }

    // status: required, must be in valid set
    String status = cells.getOrDefault("status", "").trim().toUpperCase();
    if (status.isEmpty()) {
      issues.add(
          ImportIssue.error(rowNumber, "status", "INVALID_OFFERING_STATUS", "status is required."));
    } else if (!VALID_STATUSES.contains(status)) {
      issues.add(
          ImportIssue.error(
              rowNumber,
              "status",
              "INVALID_OFFERING_STATUS",
              "status '%s' is not valid. Allowed: %s".formatted(status, VALID_STATUSES)));
    }

    // Session-level duplicate detection: (course, option_label, day, start_time) must be unique.
    // A repeated (course, option_label) on a DIFFERENT day/time is a valid additional weekly
    // session of the same schedule option. Only an identical time-slot within the same option
    // is a genuine conflict (would create overlapping sessions — QAL-IMP-001 / DEBT-F6-002).
    if (!course.isEmpty() && !optionLabel.isEmpty() && !day.isEmpty() && !startTimeStr.isEmpty()) {
      String sessionKey = course + "||" + optionLabel + "||" + day + "||" + startTimeStr;
      if (seenSessionKeys.contains(sessionKey)) {
        issues.add(
            ImportIssue.error(
                rowNumber,
                "option_label",
                "DUPLICATE_SESSION_IN_OPTION",
                "A session for option_label '%s', course '%s', day '%s' at %s already exists in this file."
                    .formatted(optionLabel, course, day, startTimeStr)));
      } else {
        seenSessionKeys.add(sessionKey);
      }
    }

    return issues;
  }
}
