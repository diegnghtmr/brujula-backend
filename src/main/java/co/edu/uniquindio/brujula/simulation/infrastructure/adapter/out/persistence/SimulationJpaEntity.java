package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

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
 * JPA entity mapping to {@code simulation.simulation} (created by Flyway V018).
 *
 * <p>Package-private per ARQ-006b. Optimistic locking via {@code @Version}.
 * {@code status} stores only ACTIVE/DELETED (INV-004: CURRENT/OUTDATED never persisted).
 * {@code programIds} uses Postgres {@code uuid[]} via hypersistence UUIDArrayType.
 */
@Entity
@Table(name = "simulation", schema = "simulation")
class SimulationJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "period_id", nullable = false)
  private UUID periodId;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "program_ids", columnDefinition = "uuid[]", nullable = false)
  private UUID[] programIds;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  // JPA requires no-arg constructor
  protected SimulationJpaEntity() {}

  UUID getId() { return id; }
  void setId(UUID id) { this.id = id; }

  UUID getOwnerId() { return ownerId; }
  void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

  String getName() { return name; }
  void setName(String name) { this.name = name; }

  UUID getPeriodId() { return periodId; }
  void setPeriodId(UUID periodId) { this.periodId = periodId; }

  UUID[] getProgramIds() { return programIds; }
  void setProgramIds(UUID[] programIds) { this.programIds = programIds; }

  String getStatus() { return status; }
  void setStatus(String status) { this.status = status; }

  Instant getCreatedAt() { return createdAt; }
  void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  Instant getUpdatedAt() { return updatedAt; }
  void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

  long getVersion() { return version; }
  void setVersion(long version) { this.version = version; }
}
