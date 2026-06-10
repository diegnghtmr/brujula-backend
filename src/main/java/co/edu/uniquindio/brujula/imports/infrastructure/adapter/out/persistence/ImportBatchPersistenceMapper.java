package co.edu.uniquindio.brujula.imports.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.imports.domain.exception.SchemaVersionMismatchException;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatch;
import co.edu.uniquindio.brujula.imports.domain.model.ImportBatchId;
import co.edu.uniquindio.brujula.imports.domain.model.ImportIssue;
import co.edu.uniquindio.brujula.imports.domain.model.ImportKind;
import co.edu.uniquindio.brujula.imports.domain.model.ImportRow;
import co.edu.uniquindio.brujula.imports.domain.model.ImportStatus;
import co.edu.uniquindio.brujula.imports.domain.model.Severity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

/**
 * Maps between {@link ImportBatch} domain aggregates and {@link ImportBatchJpaEntity}.
 *
 * <p>JSONB serialization strategy (ADR-F6-002): {@code validatedRows} and {@code issues} are stored
 * as JSON strings in the {@code validated_rows_json} and {@code issues_json} columns.
 * Deserialization uses a dedicated Jackson {@link ObjectMapper} to avoid coupling with the
 * HTTP-layer Jackson 3 mapper. No framework annotations required here — the mapper is instantiated
 * by the persistence adapter.
 *
 * <p>Schema version guard (NFR-006): {@link #toDomain} checks the stored {@code schema_version}
 * against {@link ImportBatch#CURRENT_SCHEMA_VERSION} and throws {@link
 * SchemaVersionMismatchException} if they differ.
 *
 * <p>Package-private per ARQ-006b.
 */
class ImportBatchPersistenceMapper {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  /**
   * Converts a JPA entity to a domain aggregate.
   *
   * @throws SchemaVersionMismatchException if {@code entity.schemaVersion} ≠ {@link
   *     ImportBatch#CURRENT_SCHEMA_VERSION}
   */
  ImportBatch toDomain(ImportBatchJpaEntity entity) {
    // NFR-006: schema version guard
    if (entity.getSchemaVersion() != ImportBatch.CURRENT_SCHEMA_VERSION) {
      throw new SchemaVersionMismatchException(
          ImportBatchId.of(entity.getId()),
          entity.getSchemaVersion(),
          ImportBatch.CURRENT_SCHEMA_VERSION);
    }

    List<ImportRow> validatedRows = deserializeRows(entity.getValidatedRowsJson());
    List<ImportIssue> issues = deserializeIssues(entity.getIssuesJson());

    return ImportBatch.reconstitute(
        ImportBatchId.of(entity.getId()),
        ImportKind.valueOf(entity.getKind()),
        entity.getProgramId(),
        entity.getPlanId(),
        entity.getPeriodId(),
        ImportStatus.valueOf(entity.getStatus()),
        entity.getTotalRows(),
        entity.getValidRows(),
        issues,
        validatedRows,
        entity.getSchemaVersion(),
        entity.getVersion(),
        entity.getCreatedAt());
  }

  ImportBatchJpaEntity toJpaEntity(ImportBatch batch) {
    ImportBatchJpaEntity entity = new ImportBatchJpaEntity();
    entity.setId(batch.getId().value());
    entity.setKind(batch.getKind().name());
    entity.setProgramId(batch.getProgramId());
    entity.setPlanId(batch.getPlanId());
    entity.setPeriodId(batch.getPeriodId());
    entity.setStatus(batch.getStatus().name());
    entity.setTotalRows(batch.getTotalRows());
    entity.setValidRows(batch.getValidRows());
    entity.setValidatedRowsJson(serializeRows(batch.getValidatedRows()));
    entity.setIssuesJson(serializeIssues(batch.getIssues()));
    entity.setSchemaVersion(batch.getSchemaVersion());
    entity.setVersion(batch.getVersion());
    entity.setCreatedAt(batch.getCreatedAt());
    return entity;
  }

  // -------------------------------------------------------------------------
  // JSONB serialization helpers
  // -------------------------------------------------------------------------

  private String serializeRows(List<ImportRow> rows) {
    if (rows == null || rows.isEmpty()) {
      return "[]";
    }
    try {
      List<Map<String, Object>> list =
          rows.stream()
              .map(r -> Map.<String, Object>of("index", r.index(), "cells", r.cells()))
              .toList();
      return MAPPER.writeValueAsString(list);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize ImportRow list to JSON", e);
    }
  }

  @SuppressWarnings("unchecked")
  private List<ImportRow> deserializeRows(String json) {
    if (json == null || json.isBlank() || json.equals("[]")) {
      return List.of();
    }
    try {
      List<Map<String, Object>> list =
          MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
      return list.stream()
          .map(
              m -> {
                int index = ((Number) m.get("index")).intValue();
                Map<String, String> cells = (Map<String, String>) m.get("cells");
                return new ImportRow(index, cells != null ? cells : Map.of());
              })
          .toList();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to deserialize ImportRow list from JSON: " + json, e);
    }
  }

  private String serializeIssues(List<ImportIssue> issues) {
    if (issues == null || issues.isEmpty()) {
      return "[]";
    }
    try {
      List<Map<String, Object>> list =
          issues.stream()
              .map(
                  issue -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("row", issue.row());
                    m.put("column", issue.column());
                    m.put("code", issue.code());
                    m.put("message", issue.message());
                    m.put("severity", issue.severity().name());
                    return m;
                  })
              .toList();
      return MAPPER.writeValueAsString(list);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize ImportIssue list to JSON", e);
    }
  }

  private List<ImportIssue> deserializeIssues(String json) {
    if (json == null || json.isBlank() || json.equals("[]")) {
      return List.of();
    }
    try {
      List<Map<String, Object>> list =
          MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
      return list.stream()
          .map(
              m ->
                  new ImportIssue(
                      ((Number) m.get("row")).intValue(),
                      (String) m.get("column"),
                      (String) m.get("code"),
                      (String) m.get("message"),
                      Severity.valueOf((String) m.get("severity"))))
          .toList();
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to deserialize ImportIssue list from JSON: " + json, e);
    }
  }
}
