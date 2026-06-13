package co.edu.uniquindio.brujula.imports.application.service;

import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-kind validator for REQUIREMENTS import rows.
 *
 * <p>Validates each row against the REQUIREMENTS column mapping and detects:
 *
 * <ul>
 *   <li>Missing or unknown destination/required course references
 *   <li>Invalid requirement_type values
 *   <li>Intra-batch prerequisite cycles (CIRCULAR_SEQUENCE) via {@link ImportCycleDetector}
 * </ul>
 *
 * <p>NOTE: At import-validation time, course UUIDs are not yet available (we only have
 * official_code / name strings from the file). Resolution to UUIDs happens at confirm time. This
 * validator performs structural and intra-batch cycle detection on the string representation. For
 * full graph cycle detection including existing DB requirements, the confirm service re-checks with
 * real UUIDs.
 *
 * <p>Uses {@link ImportCycleDetector} (module-local) instead of {@code
 * curriculum.domain.PrerequisiteAcyclicityService} to respect the {@code imports} module boundary
 * (imports may only depend on {@code curriculum::api}, not curriculum domain classes).
 *
 * <p>Framework-free. Wired as a plain bean in {@link
 * co.edu.uniquindio.brujula.imports.infrastructure.configuration.ImportsConfiguration}.
 */
public class RequirementsValidator {

  private static final Set<String> VALID_REQUIREMENT_TYPES =
      Set.of("COURSE_PASSED", "SEQUENCE", "MINIMUM_CREDITS", "CONFIGURABLE");

  /** No-arg constructor — the old PrerequisiteAcyclicityService dependency is removed. */
  public RequirementsValidator() {}

  /**
   * Validate all rows in a REQUIREMENTS batch.
   *
   * <p>Uses a local {@link ImportCycleDetector} to detect intra-batch cycles without DB lookups and
   * without coupling to the curriculum domain layer.
   *
   * @param rows list of import rows
   * @return map of row index → issues for that row
   */
  public Map<Integer, List<ImportIssue>> validateAll(List<ImportRow> rows) {
    Map<Integer, List<ImportIssue>> result = new HashMap<>();

    // Phase 1: basic per-row validation
    Set<String> destinationPairsSeen = new HashSet<>();
    List<ImportRow> structurallyValidRows = new ArrayList<>();

    for (ImportRow row : rows) {
      List<ImportIssue> issues = validateSingleRow(row, destinationPairsSeen);
      if (!issues.isEmpty()) {
        result.put(row.index(), issues);
      } else {
        structurallyValidRows.add(row);
      }
    }

    // Phase 2: intra-batch cycle detection on structurally valid rows
    if (structurallyValidRows.size() > 1) {
      detectIntrabatchCycles(structurallyValidRows, result);
    }

    return result;
  }

  private List<ImportIssue> validateSingleRow(ImportRow row, Set<String> destinationPairsSeen) {
    List<ImportIssue> issues = new ArrayList<>();
    int rowNumber = row.index() + 1;
    Map<String, String> cells = row.cells();

    // destination_course: required
    String destinationCourse = cells.getOrDefault("destination_course", "").trim();
    if (destinationCourse.isEmpty()) {
      issues.add(
          ImportIssue.error(
              rowNumber,
              "destination_course",
              "DESTINATION_COURSE_NOT_FOUND",
              "destination_course is required."));
    }

    // required_course_or_value: required
    String requiredCourse = cells.getOrDefault("required_course_or_value", "").trim();
    if (requiredCourse.isEmpty()) {
      issues.add(
          ImportIssue.error(
              rowNumber,
              "required_course_or_value",
              "REQUIRED_COURSE_NOT_FOUND",
              "required_course_or_value is required."));
    }

    // requirement_type: required, must be in allowed set
    String reqType = cells.getOrDefault("requirement_type", "").trim().toUpperCase();
    if (reqType.isEmpty()) {
      issues.add(
          ImportIssue.error(
              rowNumber,
              "requirement_type",
              "INVALID_REQUIREMENT_TYPE",
              "requirement_type is required."));
    } else if (!VALID_REQUIREMENT_TYPES.contains(reqType)) {
      issues.add(
          ImportIssue.error(
              rowNumber,
              "requirement_type",
              "INVALID_REQUIREMENT_TYPE",
              "requirement_type '%s' is not valid. Allowed: %s"
                  .formatted(reqType, VALID_REQUIREMENT_TYPES)));
    }

    // duplicate check: (destination_course, required_course_or_value) pair
    if (!destinationCourse.isEmpty() && !requiredCourse.isEmpty()) {
      String pair = destinationCourse + "||" + requiredCourse;
      if (destinationPairsSeen.contains(pair)) {
        issues.add(
            ImportIssue.error(
                rowNumber,
                null,
                "DUPLICATE_REQUIREMENT_IN_BATCH",
                "The requirement ('%s' → '%s') already appears in this file."
                    .formatted(requiredCourse, destinationCourse)));
      } else {
        destinationPairsSeen.add(pair);
      }
    }

    return issues;
  }

  /**
   * Detects intra-batch cycles using a string-keyed {@link ImportCycleDetector}.
   *
   * <p>Incrementally adds edges and checks for cycles after each addition. The first edge that
   * introduces a cycle produces a CIRCULAR_SEQUENCE error on that row.
   */
  private void detectIntrabatchCycles(
      List<ImportRow> validRows, Map<Integer, List<ImportIssue>> result) {
    ImportCycleDetector detector = new ImportCycleDetector();
    List<int[]> rowsWithCycles = new ArrayList<>();

    for (ImportRow row : validRows) {
      String dest = row.cells().getOrDefault("destination_course", "").trim();
      String req = row.cells().getOrDefault("required_course_or_value", "").trim();
      if (dest.isEmpty() || req.isEmpty()) continue;

      // Edge: req → dest (req must be satisfied before dest)
      detector.addEdge(req, dest);
      if (detector.hasCycle()) {
        rowsWithCycles.add(new int[] {row.index()});
        // Remove the offending edge by rebuilding without it
        detector = rebuildDetectorWithout(validRows, rowsWithCycles);
      }
    }

    for (int[] cycleRow : rowsWithCycles) {
      int rowIndex = cycleRow[0];
      result
          .computeIfAbsent(rowIndex, k -> new ArrayList<>())
          .add(
              ImportIssue.error(
                  rowIndex + 1,
                  null,
                  "CIRCULAR_SEQUENCE",
                  "Adding this requirement would create a prerequisite cycle."));
    }
  }

  /**
   * Rebuilds the cycle detector from all valid rows, excluding rows whose index is already flagged
   * as a cycle row.
   */
  private ImportCycleDetector rebuildDetectorWithout(
      List<ImportRow> rows, List<int[]> alreadyFlaggedCycleRows) {
    Set<Integer> flaggedIndices = new HashSet<>();
    for (int[] flagged : alreadyFlaggedCycleRows) {
      flaggedIndices.add(flagged[0]);
    }

    ImportCycleDetector rebuilt = new ImportCycleDetector();
    for (ImportRow row : rows) {
      if (flaggedIndices.contains(row.index())) continue;
      String dest = row.cells().getOrDefault("destination_course", "").trim();
      String req = row.cells().getOrDefault("required_course_or_value", "").trim();
      if (dest.isEmpty() || req.isEmpty()) continue;
      rebuilt.addEdge(req, dest);
    }
    return rebuilt;
  }
}
