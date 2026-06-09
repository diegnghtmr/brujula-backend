package co.edu.uniquindio.brujula.imports.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity mapping to {@code imports.import_batch} (created by Flyway V019).
 *
 * <p>Package-private per ARQ-006b. The two JSONB columns ({@code validatedRowsJson} and {@code
 * issuesJson}) are mapped via {@link JdbcTypeCode}{@code (SqlTypes.JSON)} which instructs Hibernate
 * to treat the column as a JSONB type at the PostgreSQL level.
 *
 * <p>Optimistic locking is handled by {@code @Version} on {@code version}. Schema-version guard
 * (NFR-006) is implemented in the mapper / adapter layer.
 */
@Entity
@Table(name = "import_batch", schema = "imports")
class ImportBatchJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "kind", nullable = false)
  private String kind;

  @Column(name = "program_id", nullable = false)
  private UUID programId;

  @Column(name = "plan_id")
  private UUID planId;

  @Column(name = "period_id")
  private UUID periodId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "total_rows", nullable = false)
  private int totalRows;

  @Column(name = "valid_rows", nullable = false)
  private int validRows;

  /** JSONB payload: serialized {@code List<ImportRow>} for confirm replay (ADR-F6-002). */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "validated_rows_json", columnDefinition = "jsonb", nullable = false)
  private String validatedRowsJson;

  /** JSONB payload: serialized {@code List<ImportIssue>}. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "issues_json", columnDefinition = "jsonb", nullable = false)
  private String issuesJson;

  @Column(name = "schema_version", nullable = false)
  private int schemaVersion;

  @Column(name = "idempotency_key")
  private UUID idempotencyKey;

  @Column(name = "idempotency_hash")
  private String idempotencyHash;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // JPA requires no-arg constructor
  protected ImportBatchJpaEntity() {}

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  String getKind() {
    return kind;
  }

  void setKind(String kind) {
    this.kind = kind;
  }

  UUID getProgramId() {
    return programId;
  }

  void setProgramId(UUID programId) {
    this.programId = programId;
  }

  UUID getPlanId() {
    return planId;
  }

  void setPlanId(UUID planId) {
    this.planId = planId;
  }

  UUID getPeriodId() {
    return periodId;
  }

  void setPeriodId(UUID periodId) {
    this.periodId = periodId;
  }

  String getStatus() {
    return status;
  }

  void setStatus(String status) {
    this.status = status;
  }

  int getTotalRows() {
    return totalRows;
  }

  void setTotalRows(int totalRows) {
    this.totalRows = totalRows;
  }

  int getValidRows() {
    return validRows;
  }

  void setValidRows(int validRows) {
    this.validRows = validRows;
  }

  String getValidatedRowsJson() {
    return validatedRowsJson;
  }

  void setValidatedRowsJson(String validatedRowsJson) {
    this.validatedRowsJson = validatedRowsJson;
  }

  String getIssuesJson() {
    return issuesJson;
  }

  void setIssuesJson(String issuesJson) {
    this.issuesJson = issuesJson;
  }

  int getSchemaVersion() {
    return schemaVersion;
  }

  void setSchemaVersion(int schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  UUID getIdempotencyKey() {
    return idempotencyKey;
  }

  void setIdempotencyKey(UUID idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  String getIdempotencyHash() {
    return idempotencyHash;
  }

  void setIdempotencyHash(String idempotencyHash) {
    this.idempotencyHash = idempotencyHash;
  }

  long getVersion() {
    return version;
  }

  void setVersion(long version) {
    this.version = version;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
