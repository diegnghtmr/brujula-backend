package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/**
 * JPA entity mapping to {@code curriculum.course_requirement} (created by Flyway V011).
 *
 * <p>Package-private per ARQ-006. Optimistic locking via {@code @Version} (DB-006/DT-014).
 */
@Entity
@Table(name = "course_requirement", schema = "curriculum")
class CourseRequirementJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "target_course_id", nullable = false)
  private UUID targetCourseId;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "required_course_id")
  private UUID requiredCourseId; // nullable

  @Column(name = "required_credits")
  private Integer requiredCredits; // nullable

  @Column(name = "simultaneity_allowed")
  private Boolean simultaneityAllowed; // nullable

  @Column(name = "confidence")
  private String confidence; // nullable

  @Column(name = "inferred")
  private Boolean inferred; // nullable

  @Column(name = "status", nullable = false)
  private String status;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  // JPA requires a no-arg constructor
  protected CourseRequirementJpaEntity() {}

  UUID getId() { return id; }
  UUID getTargetCourseId() { return targetCourseId; }
  String getType() { return type; }
  UUID getRequiredCourseId() { return requiredCourseId; }
  Integer getRequiredCredits() { return requiredCredits; }
  Boolean getSimultaneityAllowed() { return simultaneityAllowed; }
  String getConfidence() { return confidence; }
  Boolean getInferred() { return inferred; }
  String getStatus() { return status; }
  long getVersion() { return version; }
}
