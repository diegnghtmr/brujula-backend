package co.edu.uniquindio.brujula.imports.application.service;

import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-kind validator for OFFERINGS import rows.
 *
 * <p>Validates each row against the OFFERINGS column mapping. Produces {@link ImportIssue} entries
 * with error codes from the spec vocabulary. Framework-free.
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
   * schedule option. A conflict exists when two rows within the same {@code (course, option_label,
   * day)} group have overlapping half-open time intervals {@code [start, end)}. Touching endpoints
   * (e.g. 08:00-10:00 and 10:00-12:00) are NOT a conflict.
   *
   * @param rows list of import rows
   * @return map of row index → issues for that row
   */
  public Map<Integer, List<ImportIssue>> validateAll(List<ImportRow> rows) {
    Map<Integer, List<ImportIssue>> result = new HashMap<>();
    // Track seen (start, end) intervals per (course, option_label, day) group key.
    // Two sessions in the same group conflict when their half-open intervals overlap:
    //   newStart < existingEnd  &&  newEnd > existingStart
    // Touching endpoints (e.g. 08:00-10:00 and 10:00-12:00) are NOT a conflict.
    Map<String, List<LocalTime[]>> seenIntervalsByGroup = new HashMap<>();

    for (ImportRow row : rows) {
      List<ImportIssue> issues = validateSingleRow(row, seenIntervalsByGroup);
      if (!issues.isEmpty()) {
        result.put(row.index(), issues);
      }
    }
    return result;
  }

  private List<ImportIssue> validateSingleRow(
      ImportRow row, Map<String, List<LocalTime[]>> seenIntervalsByGroup) {
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

    // Session-level interval-overlap detection.
    // Group key: (course, option_label, day). Within each group, no two sessions may have
    // overlapping half-open intervals [start, end). Touching endpoints are NOT a conflict.
    // Overlap condition: newStart < existingEnd  &&  newEnd > existingStart.
    if (!course.isEmpty()
        && !optionLabel.isEmpty()
        && !day.isEmpty()
        && startTime != null
        && !endTimeStr.isEmpty()) {
      try {
        LocalTime endTime = LocalTime.parse(endTimeStr, TIME_FORMATTER);
        if (endTime.isAfter(startTime)) {
          // Capture as effectively-final for use inside the lambda.
          final LocalTime sessionStart = startTime;
          final LocalTime sessionEnd = endTime;
          // Only check/register the interval when both times are structurally valid (end > start).
          String groupKey = course + "||" + optionLabel + "||" + day;
          List<LocalTime[]> existing =
              seenIntervalsByGroup.computeIfAbsent(groupKey, k -> new ArrayList<>());
          boolean overlaps =
              existing.stream()
                  .anyMatch(
                      interval ->
                          sessionStart.isBefore(interval[1]) && sessionEnd.isAfter(interval[0]));
          if (overlaps) {
            issues.add(
                ImportIssue.error(
                    rowNumber,
                    "option_label",
                    "DUPLICATE_SESSION_IN_OPTION",
                    "A session for option_label '%s', course '%s', day '%s' overlapping [%s, %s) already exists in this file."
                        .formatted(optionLabel, course, day, startTimeStr, endTimeStr)));
          } else {
            existing.add(new LocalTime[] {sessionStart, sessionEnd});
          }
        }
      } catch (DateTimeParseException ignored) {
        // end_time parse failure is already reported above; skip overlap registration.
      }
    }

    return issues;
  }
}
