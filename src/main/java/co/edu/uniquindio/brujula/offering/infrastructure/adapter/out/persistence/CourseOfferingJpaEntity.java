package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity mapping to {@code offering.course_offering} (created by Flyway V016).
 *
 * <p>Package-private. Cascade ALL / LAZY fetch for schedule options.
 */
@Entity
@Table(name = "course_offering", schema = "offering")
class CourseOfferingJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  /** Opaque cross-schema reference — no FK. */
  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  /** Denormalized course name snapshot — avoids runtime curriculum coupling. */
  @Column(name = "course_name", nullable = false)
  private String courseName;

  /** Opaque cross-schema reference — no FK. */
  @Column(name = "program_id", nullable = false)
  private UUID programId;

  @Column(name = "period_id", nullable = false)
  private UUID periodId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "notes")
  private String notes;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @OneToMany(
      mappedBy = "offering",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @OrderBy("id ASC")
  private Set<ScheduleOptionJpaEntity> scheduleOptions = new LinkedHashSet<>();

  protected CourseOfferingJpaEntity() {}

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  UUID getCourseId() {
    return courseId;
  }

  void setCourseId(UUID courseId) {
    this.courseId = courseId;
  }

  String getCourseName() {
    return courseName;
  }

  void setCourseName(String courseName) {
    this.courseName = courseName;
  }

  UUID getProgramId() {
    return programId;
  }

  void setProgramId(UUID programId) {
    this.programId = programId;
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

  String getNotes() {
    return notes;
  }

  void setNotes(String notes) {
    this.notes = notes;
  }

  long getVersion() {
    return version;
  }

  void setVersion(long version) {
    this.version = version;
  }

  Set<ScheduleOptionJpaEntity> getScheduleOptions() {
    return scheduleOptions;
  }

  void setScheduleOptions(Set<ScheduleOptionJpaEntity> scheduleOptions) {
    this.scheduleOptions = scheduleOptions;
  }
}
