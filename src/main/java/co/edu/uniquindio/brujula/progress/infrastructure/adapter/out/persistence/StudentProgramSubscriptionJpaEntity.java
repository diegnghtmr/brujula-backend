package co.edu.uniquindio.brujula.progress.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping to {@code progress.student_program_subscription} (created by Flyway V015).
 *
 * <p>Package-private. Optimistic locking via {@code @Version}. No primary flag (contract
 * alignment). No domain types — just persistence state.
 */
@Entity
@Table(name = "student_program_subscription", schema = "progress")
class StudentProgramSubscriptionJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "program_id", nullable = false)
  private UUID programId;

  @Column(name = "plan_id", nullable = false)
  private UUID planId;

  @Column(name = "reference_journey_code", nullable = false)
  private String referenceJourneyCode;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  // JPA requires no-arg constructor
  protected StudentProgramSubscriptionJpaEntity() {}

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  UUID getUserId() {
    return userId;
  }

  void setUserId(UUID userId) {
    this.userId = userId;
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

  String getReferenceJourneyCode() {
    return referenceJourneyCode;
  }

  void setReferenceJourneyCode(String referenceJourneyCode) {
    this.referenceJourneyCode = referenceJourneyCode;
  }

  String getStatus() {
    return status;
  }

  void setStatus(String status) {
    this.status = status;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  long getVersion() {
    return version;
  }

  void setVersion(long version) {
    this.version = version;
  }
}
