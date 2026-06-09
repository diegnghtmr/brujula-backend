package co.edu.uniquindio.brujula.imports.application.service;

import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-kind validator for COURSES import rows (IMP-006, IMP-002).
 *
 * <p>Validates each row against the COURSES column mapping (ADR-014). Produces {@link ImportIssue}
 * entries with error codes from the spec vocabulary. Framework-free.
 *
 * <p>Wired as a plain bean in {@link
 * co.edu.uniquindio.brujula.imports.infrastructure.configuration.ImportsConfiguration}.
 */
public class CoursesValidator {

  private static final Set<String> VALID_COURSE_TYPES =
      Set.of("OBLIGATORIA", "ELECTIVA", "LIBRE_ELECCION", "TRABAJO_GRADO");

  /** Min and max allowed credits. */
  private static final int MIN_CREDITS = 1;

  private static final int MAX_CREDITS = 20;

  /** Min and max weekly hours. */
  private static final int MIN_WEEKLY_HOURS = 1;

  private static final int MAX_WEEKLY_HOURS = 40;

  /** Allowed recommended semester range (1-10). */
  private static final int MIN_SEMESTER = 1;

  private static final int MAX_SEMESTER = 10;

  /**
   * Validate a single COURSES row.
   *
   * @param row the import row (0-based index)
   * @param seenOfficialCodes mutable set of official_code values seen earlier in this batch (for
   *     duplicate detection)
   * @return list of issues for this row (empty = valid)
   */
  public List<ImportIssue> validate(ImportRow row, Map<String, Integer> seenOfficialCodes) {
    List<ImportIssue> issues = new ArrayList<>();
    int rowNumber = row.index() + 1; // 1-based for reporting
    Map<String, String> cells = row.cells();

    // course_name: required, non-blank
    String courseName = cells.getOrDefault("course_name", "").trim();
    if (courseName.isEmpty()) {
      issues.add(
          ImportIssue.error(
              rowNumber,
              "course_name",
              "COURSE_NAME_MISSING",
              "course_name is required and must not be blank."));
    }

    // credits: required, positive integer in range
    String creditsStr = cells.getOrDefault("credits", "").trim();
    if (!creditsStr.isEmpty()) {
      try {
        int credits = Integer.parseInt(creditsStr);
        if (credits < MIN_CREDITS || credits > MAX_CREDITS) {
          issues.add(
              ImportIssue.error(
                  rowNumber,
                  "credits",
                  "INVALID_CREDITS",
                  "credits must be between %d and %d, got: %d"
                      .formatted(MIN_CREDITS, MAX_CREDITS, credits)));
        }
      } catch (NumberFormatException e) {
        issues.add(
            ImportIssue.error(
                rowNumber,
                "credits",
                "INVALID_CREDITS",
                "credits must be a positive integer, got: " + creditsStr));
      }
    } else {
      issues.add(
          ImportIssue.error(rowNumber, "credits", "INVALID_CREDITS", "credits is required."));
    }

    // weekly_hours: required, positive integer
    String weeklyHoursStr = cells.getOrDefault("weekly_hours", "").trim();
    if (!weeklyHoursStr.isEmpty()) {
      try {
        int weeklyHours = Integer.parseInt(weeklyHoursStr);
        if (weeklyHours < MIN_WEEKLY_HOURS || weeklyHours > MAX_WEEKLY_HOURS) {
          issues.add(
              ImportIssue.error(
                  rowNumber,
                  "weekly_hours",
                  "INVALID_WEEKLY_HOURS",
                  "weekly_hours must be between %d and %d, got: %d"
                      .formatted(MIN_WEEKLY_HOURS, MAX_WEEKLY_HOURS, weeklyHours)));
        }
      } catch (NumberFormatException e) {
        issues.add(
            ImportIssue.error(
                rowNumber,
                "weekly_hours",
                "INVALID_WEEKLY_HOURS",
                "weekly_hours must be a positive integer, got: " + weeklyHoursStr));
      }
    } else {
      issues.add(
          ImportIssue.error(
              rowNumber, "weekly_hours", "INVALID_WEEKLY_HOURS", "weekly_hours is required."));
    }

    // recommended_semester: required, 1-10
    String semesterStr = cells.getOrDefault("recommended_semester", "").trim();
    if (!semesterStr.isEmpty()) {
      try {
        int semester = Integer.parseInt(semesterStr);
        if (semester < MIN_SEMESTER || semester > MAX_SEMESTER) {
          issues.add(
              ImportIssue.error(
                  rowNumber,
                  "recommended_semester",
                  "INVALID_RECOMMENDED_SEMESTER",
                  "recommended_semester must be between %d and %d, got: %d"
                      .formatted(MIN_SEMESTER, MAX_SEMESTER, semester)));
        }
      } catch (NumberFormatException e) {
        issues.add(
            ImportIssue.error(
                rowNumber,
                "recommended_semester",
                "INVALID_RECOMMENDED_SEMESTER",
                "recommended_semester must be an integer, got: " + semesterStr));
      }
    } else {
      issues.add(
          ImportIssue.error(
              rowNumber,
              "recommended_semester",
              "INVALID_RECOMMENDED_SEMESTER",
              "recommended_semester is required."));
    }

    // type: required, must be in allowed set
    String type = cells.getOrDefault("type", "").trim().toUpperCase();
    if (type.isEmpty()) {
      issues.add(ImportIssue.error(rowNumber, "type", "INVALID_COURSE_TYPE", "type is required."));
    } else if (!VALID_COURSE_TYPES.contains(type)) {
      issues.add(
          ImportIssue.error(
              rowNumber,
              "type",
              "INVALID_COURSE_TYPE",
              "type '%s' is not valid. Allowed: %s".formatted(type, VALID_COURSE_TYPES)));
    }

    // official_code: optional but must be unique within the batch if present
    String officialCode = cells.getOrDefault("official_code", "").trim();
    if (!officialCode.isEmpty()) {
      if (seenOfficialCodes.containsKey(officialCode)) {
        int firstRow = seenOfficialCodes.get(officialCode);
        issues.add(
            ImportIssue.error(
                rowNumber,
                "official_code",
                "DUPLICATE_COURSE_IN_BATCH",
                "official_code '%s' already appears in row %d of this file."
                    .formatted(officialCode, firstRow)));
      } else {
        seenOfficialCodes.put(officialCode, rowNumber);
      }
    }

    // journey: required
    String journey = cells.getOrDefault("journey", "").trim();
    if (journey.isEmpty()) {
      issues.add(
          ImportIssue.error(rowNumber, "journey", "UNRECOGNIZED_JOURNEY", "journey is required."));
    }

    return issues;
  }

  /**
   * Validate all rows in a COURSES batch, tracking duplicates across the batch.
   *
   * @param rows list of import rows
   * @return map of row index → issues for that row
   */
  public Map<Integer, List<ImportIssue>> validateAll(List<ImportRow> rows) {
    Map<Integer, List<ImportIssue>> result = new HashMap<>();
    Map<String, Integer> seenOfficialCodes = new HashMap<>();
    for (ImportRow row : rows) {
      List<ImportIssue> issues = validate(row, seenOfficialCodes);
      if (!issues.isEmpty()) {
        result.put(row.index(), issues);
      }
    }
    return result;
  }
}
