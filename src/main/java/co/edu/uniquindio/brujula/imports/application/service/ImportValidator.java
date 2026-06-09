package co.edu.uniquindio.brujula.imports.application.service;

import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrating import validator — delegates to per-kind validators and assembles the result into a
 * validated or invalid {@link ImportBatch} (IMP-002, SC-002-A through SC-002-E).
 *
 * <p>Framework-free. Wired in {@link
 * co.edu.uniquindio.brujula.imports.infrastructure.configuration.ImportsConfiguration}.
 */
public class ImportValidator {

  private final CoursesValidator coursesValidator;
  private final RequirementsValidator requirementsValidator;
  private final OfferingsValidator offeringsValidator;

  public ImportValidator(
      CoursesValidator coursesValidator,
      RequirementsValidator requirementsValidator,
      OfferingsValidator offeringsValidator) {
    this.coursesValidator = coursesValidator;
    this.requirementsValidator = requirementsValidator;
    this.offeringsValidator = offeringsValidator;
  }

  /**
   * Validate parsed rows and build an ImportBatch.
   *
   * @param batchId the pre-generated batch ID
   * @param kind import kind
   * @param programId target program UUID
   * @param planId target plan UUID (null for OFFERINGS)
   * @param periodId target period UUID (null for COURSES/REQUIREMENTS)
   * @param rows parsed rows from the file
   * @param globalIssues any global (file-level) issues from the parser
   * @param createdAt creation timestamp
   * @return VALIDATED or INVALID ImportBatch
   */
  public ImportBatch validate(
      ImportBatchId batchId,
      ImportKind kind,
      UUID programId,
      UUID planId,
      UUID periodId,
      List<ImportRow> rows,
      List<ImportIssue> globalIssues,
      Instant createdAt) {

    // If there are global issues (structural errors), the batch is immediately INVALID
    if (!globalIssues.isEmpty()) {
      List<ImportIssue> allIssues = new ArrayList<>(globalIssues);
      return ImportBatch.invalid(
          batchId, kind, programId, planId, periodId, 0, 0, allIssues, createdAt);
    }

    // Delegate to the per-kind validator
    Map<Integer, List<ImportIssue>> rowIssueMap = validateByKind(kind, rows);

    // Categorize rows and issues
    List<ImportRow> validRows = new ArrayList<>();
    List<ImportIssue> allIssues = new ArrayList<>();
    int totalRows = rows.size();
    int errorCount = 0;

    for (ImportRow row : rows) {
      List<ImportIssue> rowIssues = rowIssueMap.getOrDefault(row.index(), List.of());
      boolean hasBlockingError = rowIssues.stream().anyMatch(ImportIssue::isBlocking);
      if (hasBlockingError) {
        errorCount++;
        allIssues.addAll(rowIssues);
      } else {
        validRows.add(row);
        // Add warnings (non-blocking issues) to the issues list too
        allIssues.addAll(rowIssues);
      }
    }

    int validRowCount = validRows.size();

    // Decide batch status: INVALID if any blocking error, VALIDATED otherwise
    // Per spec SC-002-C: if ALL rows have blocking errors → INVALID
    // Per spec open item 4: strict position — any blocking error → INVALID, not confirmable
    // We follow the STRICT interpretation: ≥1 blocking error → INVALID
    if (errorCount > 0) {
      // INVALID: carry all issues; no validated rows
      if (allIssues.isEmpty()) {
        allIssues.add(ImportIssue.error(0, null, "EMPTY_FILE", "No valid rows found."));
      }
      return ImportBatch.invalid(
          batchId,
          kind,
          programId,
          planId,
          periodId,
          validRowCount,
          totalRows,
          allIssues,
          createdAt);
    }

    // All rows are valid (or have only warnings)
    if (validRows.isEmpty()) {
      List<ImportIssue> emptyIssues = new ArrayList<>();
      emptyIssues.add(ImportIssue.error(0, null, "EMPTY_FILE", "No valid rows found."));
      return ImportBatch.invalid(
          batchId, kind, programId, planId, periodId, 0, totalRows, emptyIssues, createdAt);
    }

    return ImportBatch.validated(
        batchId,
        kind,
        programId,
        planId,
        periodId,
        validRowCount,
        totalRows,
        allIssues,
        validRows,
        createdAt);
  }

  private Map<Integer, List<ImportIssue>> validateByKind(ImportKind kind, List<ImportRow> rows) {
    return switch (kind) {
      case COURSES -> coursesValidator.validateAll(rows);
      case REQUIREMENTS -> requirementsValidator.validateAll(rows);
      case OFFERINGS -> offeringsValidator.validateAll(rows);
    };
  }
}
