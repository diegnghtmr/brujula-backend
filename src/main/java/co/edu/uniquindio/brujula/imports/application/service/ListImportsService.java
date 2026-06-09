package co.edu.uniquindio.brujula.imports.application.service;

import co.edu.uniquindio.brujula.imports.application.dto.ImportPageView;
import co.edu.uniquindio.brujula.imports.application.dto.ImportSummary;
import co.edu.uniquindio.brujula.imports.application.dto.ListImportsQuery;
import co.edu.uniquindio.brujula.imports.application.port.in.ListImportsUseCase;
import co.edu.uniquindio.brujula.imports.application.port.out.LoadImportBatchPort;
import co.edu.uniquindio.brujula.imports.application.port.out.StaffAssignmentPort;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;
import co.edu.uniquindio.brujula.imports.domain.model.Severity;

import java.util.ArrayList; // still used in toSummary
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service: list imports scoped to the actor's assigned programs (IMP-003).
 *
 * <p>STAFF actors see only batches belonging to programs they are actively assigned to.
 * ADMIN actors see all batches (no assignment filter).
 *
 * <p>Framework-free — wired by {@link co.edu.uniquindio.brujula.imports.infrastructure.configuration.ImportsConfiguration}.
 */
public class ListImportsService implements ListImportsUseCase {

  private final LoadImportBatchPort loadImportBatchPort;
  private final StaffAssignmentPort staffAssignmentPort;

  public ListImportsService(
      LoadImportBatchPort loadImportBatchPort,
      StaffAssignmentPort staffAssignmentPort) {
    this.loadImportBatchPort = loadImportBatchPort;
    this.staffAssignmentPort = staffAssignmentPort;
  }

  @Override
  public ImportPageView list(ListImportsQuery query) {
    ImportKind kindFilter =
        query.kindFilter() != null ? ImportKind.valueOf(query.kindFilter()) : null;
    ImportStatus statusFilter =
        query.statusFilter() != null ? ImportStatus.valueOf(query.statusFilter()) : null;

    List<ImportBatch> batches;

    if ("ADMIN".equalsIgnoreCase(query.actorRole())) {
      // ADMIN sees all programs — load without program restriction
      batches = loadAllBatches(kindFilter, statusFilter);
    } else {
      // STAFF: resolve assigned programs and aggregate their batches
      Optional<UUID> staffId = staffAssignmentPort.resolveActiveStaffId(query.actorEmail());
      if (staffId.isEmpty()) {
        // Inactive or unknown staff → return empty page (actor cannot have any batches)
        return emptyPage(query.page(), query.size());
      }
      batches = loadBatchesForStaff(staffId.get(), kindFilter, statusFilter);
    }

    // Apply pagination in-memory (the persistence adapter returns full lists per-program)
    List<ImportSummary> summaries = batches.stream()
        .map(this::toSummary)
        .toList();

    int total = summaries.size();
    int fromIndex = Math.min(query.page() * query.size(), total);
    int toIndex = Math.min(fromIndex + query.size(), total);
    List<ImportSummary> paginated = summaries.subList(fromIndex, toIndex);

    int totalPages = query.size() > 0 ? (int) Math.ceil((double) total / query.size()) : 0;

    return new ImportPageView(paginated, query.page(), query.size(), total, totalPages);
  }

  /**
   * Load all batches across all programs for ADMIN access.
   */
  private List<ImportBatch> loadAllBatches(ImportKind kindFilter, ImportStatus statusFilter) {
    return loadImportBatchPort.findAll(kindFilter, statusFilter);
  }

  /**
   * Load batches for all programs assigned to the given staff member.
   *
   * <p>QAL-PERF-001: replaced the per-program O(N) loop with a single IN-clause batch query,
   * issuing exactly 1 SQL statement regardless of how many programs are assigned.
   */
  private List<ImportBatch> loadBatchesForStaff(
      UUID staffId, ImportKind kindFilter, ImportStatus statusFilter) {
    // Get assigned program IDs
    List<UUID> assignedPrograms = staffAssignmentPort.getAssignedPrograms(staffId);
    if (assignedPrograms.isEmpty()) {
      return List.of();
    }

    // Single IN-clause query — O(1) round-trips instead of O(N)
    return loadImportBatchPort.findByProgramIds(assignedPrograms, kindFilter, statusFilter);
  }

  private ImportPageView emptyPage(int page, int size) {
    return new ImportPageView(List.of(), page, size, 0L, 0);
  }

  private ImportSummary toSummary(ImportBatch batch) {
    List<ImportSummary.ImportIssueDto> errors = new ArrayList<>();
    List<ImportSummary.ImportIssueDto> warnings = new ArrayList<>();

    for (ImportIssue issue : batch.getIssues()) {
      ImportSummary.ImportIssueDto dto =
          new ImportSummary.ImportIssueDto(
              issue.row(),
              issue.column(),
              issue.code(),
              issue.message(),
              issue.severity().name());
      if (issue.severity() == Severity.ERROR) {
        errors.add(dto);
      } else {
        warnings.add(dto);
      }
    }

    return new ImportSummary(
        batch.getId().value(),
        batch.getKind().name(),
        batch.getProgramId(),
        batch.getPlanId(),
        batch.getPeriodId(),
        batch.getStatus().name(),
        batch.getTotalRows(),
        batch.getValidRows(),
        errors,
        warnings,
        batch.getCreatedAt(),
        batch.getVersion());
  }
}
