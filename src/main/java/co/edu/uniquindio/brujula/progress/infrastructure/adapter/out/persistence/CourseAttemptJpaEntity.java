package co.edu.uniquindio.brujula.progress.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping to {@code progress.course_attempt} (created by Flyway V015).
 *
 * <p>Package-private. This stub allows Flyway validation to pass. Full application code for
 * CourseAttempt (repositories, ports, use cases) is implemented separately.
 *
 * <p>This entity is the persistence-side placeholder ensuring the table is mapped. Do NOT add
 * business logic here.
 */
@Entity
@Table(name = "course_attempt", schema = "progress")
class CourseAttemptJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "subscription_id", nullable = false)
  private UUID subscriptionId;

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "final_grade", precision = 3, scale = 2)
  private BigDecimal finalGrade; // nullable

  @Column(name = "period_id")
  private UUID periodId; // opaque, nullable

  @Column(name = "journey_code", nullable = false)
  private String journeyCode;

  @Column(name = "registered_at", nullable = false, updatable = false)
  private Instant registeredAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected CourseAttemptJpaEntity() {}

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  UUID getSubscriptionId() {
    return subscriptionId;
  }

  void setSubscriptionId(UUID subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  UUID getCourseId() {
    return courseId;
  }

  void setCourseId(UUID courseId) {
    this.courseId = courseId;
  }

  String getStatus() {
    return status;
  }

  void setStatus(String status) {
    this.status = status;
  }

  BigDecimal getFinalGrade() {
    return finalGrade;
  }

  void setFinalGrade(BigDecimal finalGrade) {
    this.finalGrade = finalGrade;
  }

  UUID getPeriodId() {
    return periodId;
  }

  void setPeriodId(UUID periodId) {
    this.periodId = periodId;
  }

  String getJourneyCode() {
    return journeyCode;
  }

  void setJourneyCode(String journeyCode) {
    this.journeyCode = journeyCode;
  }

  Instant getRegisteredAt() {
    return registeredAt;
  }

  void setRegisteredAt(Instant registeredAt) {
    this.registeredAt = registeredAt;
  }

  long getVersion() {
    return version;
  }

  void setVersion(long version) {
    this.version = version;
  }
}
