package co.edu.uniquindio.brujula.imports.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Application-layer DTO for an ImportBatch read view.
 *
 * <p>Returned by UploadImportService and serialized to the idempotency store. The REST controller
 * (WU4) maps this to the generated ImportSummary model — not the reverse direction.
 *
 * <p>No Spring/JPA imports. No domain types (ARQ-001).
 */
public record ImportSummary(
    UUID id,
    String kind,
    UUID programId,
    UUID planId,
    UUID periodId,
    String status,
    int totalRows,
    int validRows,
    List<ImportIssueDto> errors,
    List<ImportIssueDto> warnings,
    Instant createdAt,
    long version) {

  /**
   * Nested DTO for a single import issue.
   *
   * @param row row number (0 = global/file-level)
   * @param column column header name (nullable)
   * @param code error code constant
   * @param message human-readable message
   * @param severity "ERROR" or "WARNING"
   */
  public record ImportIssueDto(
      int row, String column, String code, String message, String severity) {}
}
