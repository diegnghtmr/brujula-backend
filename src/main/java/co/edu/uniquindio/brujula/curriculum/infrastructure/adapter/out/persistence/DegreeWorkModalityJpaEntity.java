package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/**
 * JPA entity mapping to {@code curriculum.degree_work_modality} (created by Flyway V011).
 *
 * <p>Package-private per ARQ-006. Optimistic locking via {@code @Version} (DB-006/DT-014).
 */
@Entity
@Table(name = "degree_work_modality", schema = "curriculum")
class DegreeWorkModalityJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "plan_id", nullable = false)
  private UUID planId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "credits")
  private Integer credits; // nullable

  @Column(name = "curricular_weekly_hours")
  private Integer curricularWeeklyHours; // nullable

  @Column(name = "status", nullable = false)
  private String status;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  // JPA requires a no-arg constructor
  protected DegreeWorkModalityJpaEntity() {}

  UUID getId() {
    return id;
  }

  UUID getPlanId() {
    return planId;
  }

  String getName() {
    return name;
  }

  Integer getCredits() {
    return credits;
  }

  Integer getCurricularWeeklyHours() {
    return curricularWeeklyHours;
  }

  String getStatus() {
    return status;
  }

  long getVersion() {
    return version;
  }
}
