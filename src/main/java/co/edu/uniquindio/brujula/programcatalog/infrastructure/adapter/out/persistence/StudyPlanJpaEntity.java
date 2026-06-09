package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code catalog.study_plan} table (created by Flyway V010).
 *
 * <p>Package-private per ARQ-006. Optimistic locking via {@code @Version} (DB-006/DT-014).
 */
@Entity
@Table(name = "study_plan", schema = "catalog")
class StudyPlanJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "program_id", nullable = false, updatable = false)
  private UUID programId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "total_credits", nullable = false)
  private int totalCredits;

  @Column(name = "reference_year")
  private Integer referenceYear;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  // JPA requires a no-arg constructor
  protected StudyPlanJpaEntity() {}

  StudyPlanJpaEntity(
      UUID id,
      UUID programId,
      String name,
      String status,
      int totalCredits,
      Integer referenceYear) {
    this.id = id;
    this.programId = programId;
    this.name = name;
    this.status = status;
    this.totalCredits = totalCredits;
    this.referenceYear = referenceYear;
  }

  UUID getId() {
    return id;
  }

  UUID getProgramId() {
    return programId;
  }

  String getName() {
    return name;
  }

  String getStatus() {
    return status;
  }

  int getTotalCredits() {
    return totalCredits;
  }

  Integer getReferenceYear() {
    return referenceYear;
  }

  long getVersion() {
    return version;
  }
}
