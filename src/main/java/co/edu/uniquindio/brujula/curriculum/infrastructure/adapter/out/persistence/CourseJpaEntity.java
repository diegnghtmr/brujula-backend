package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping to {@code curriculum.course} (created by Flyway V011).
 *
 * <p>Package-private. Schema-qualified table annotation following the identity module pattern (Boot
 * 4.0.3 / Hibernate 6.x confirmed working).
 *
 * <p>Optimistic locking via {@code @Version} on the {@code version} column.
 */
@Entity
@Table(name = "course", schema = "curriculum")
class CourseJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "plan_id", nullable = false)
  private UUID planId;

  @Column(name = "program_id", nullable = false)
  private UUID programId;

  @Column(name = "official_code")
  private String officialCode; // nullable

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "credits", nullable = false)
  private int credits;

  @Column(name = "curricular_weekly_hours", nullable = false)
  private int curricularWeeklyHours;

  @Column(name = "status", nullable = false)
  private String status;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @OneToMany(
      mappedBy = "course",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<CourseRecommendedSemesterJpaEntity> recommendedSemesters = new ArrayList<>();

  // JPA requires a no-arg constructor
  protected CourseJpaEntity() {}

  UUID getId() {
    return id;
  }

  UUID getPlanId() {
    return planId;
  }

  UUID getProgramId() {
    return programId;
  }

  String getOfficialCode() {
    return officialCode;
  }

  String getName() {
    return name;
  }

  String getType() {
    return type;
  }

  int getCredits() {
    return credits;
  }

  int getCurricularWeeklyHours() {
    return curricularWeeklyHours;
  }

  String getStatus() {
    return status;
  }

  long getVersion() {
    return version;
  }

  List<CourseRecommendedSemesterJpaEntity> getRecommendedSemesters() {
    return recommendedSemesters;
  }

  // Setters for the write path (CUR-001)
  void setId(UUID id) {
    this.id = id;
  }

  void setPlanId(UUID planId) {
    this.planId = planId;
  }

  void setProgramId(UUID programId) {
    this.programId = programId;
  }

  void setOfficialCode(String officialCode) {
    this.officialCode = officialCode;
  }

  void setName(String name) {
    this.name = name;
  }

  void setType(String type) {
    this.type = type;
  }

  void setCredits(int credits) {
    this.credits = credits;
  }

  void setCurricularWeeklyHours(int curricularWeeklyHours) {
    this.curricularWeeklyHours = curricularWeeklyHours;
  }

  void setStatus(String status) {
    this.status = status;
  }

  void setVersion(long version) {
    this.version = version;
  }

  void setRecommendedSemesters(List<CourseRecommendedSemesterJpaEntity> recommendedSemesters) {
    this.recommendedSemesters.clear();
    if (recommendedSemesters != null) {
      this.recommendedSemesters.addAll(recommendedSemesters);
    }
  }
}
