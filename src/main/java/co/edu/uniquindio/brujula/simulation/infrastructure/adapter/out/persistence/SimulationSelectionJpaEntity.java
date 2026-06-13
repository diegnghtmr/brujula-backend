package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity mapping to {@code simulation.simulation_selection} (created by Flyway V018).
 *
 * <p>Package-private. {@code sessions} stored as JSONB snapshot. Optimistic locking via
 * {@code @Version}. Soft delete via {@code status} field. {@code selectedOfferingVersion} is a
 * snapshot for staleness detection.
 */
@Entity
@Table(name = "simulation_selection", schema = "simulation")
class SimulationSelectionJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "simulation_id", nullable = false)
  private UUID simulationId;

  @Column(name = "offering_id", nullable = false)
  private UUID offeringId;

  @Column(name = "schedule_option_id", nullable = false)
  private UUID scheduleOptionId;

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "course_name", nullable = false)
  private String courseName;

  @Column(name = "program_id", nullable = false)
  private UUID programId;

  @Column(name = "credits", nullable = false, precision = 5, scale = 2)
  private BigDecimal credits;

  @Column(name = "journey_code", nullable = false)
  private String journeyCode;

  @Column(name = "offering_status", nullable = false)
  private String offeringStatus;

  @Column(name = "selected_offering_version", nullable = false)
  private long selectedOfferingVersion;

  /** JSONB snapshot of sessions: [{day, startsAt, endsAt}, ...] */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "sessions", columnDefinition = "jsonb", nullable = false)
  private String sessions;

  @Column(name = "status", nullable = false)
  private String status;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  // JPA requires no-arg constructor
  protected SimulationSelectionJpaEntity() {}

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  UUID getSimulationId() {
    return simulationId;
  }

  void setSimulationId(UUID simulationId) {
    this.simulationId = simulationId;
  }

  UUID getOfferingId() {
    return offeringId;
  }

  void setOfferingId(UUID offeringId) {
    this.offeringId = offeringId;
  }

  UUID getScheduleOptionId() {
    return scheduleOptionId;
  }

  void setScheduleOptionId(UUID scheduleOptionId) {
    this.scheduleOptionId = scheduleOptionId;
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

  BigDecimal getCredits() {
    return credits;
  }

  void setCredits(BigDecimal credits) {
    this.credits = credits;
  }

  String getJourneyCode() {
    return journeyCode;
  }

  void setJourneyCode(String journeyCode) {
    this.journeyCode = journeyCode;
  }

  String getOfferingStatus() {
    return offeringStatus;
  }

  void setOfferingStatus(String offeringStatus) {
    this.offeringStatus = offeringStatus;
  }

  long getSelectedOfferingVersion() {
    return selectedOfferingVersion;
  }

  void setSelectedOfferingVersion(long selectedOfferingVersion) {
    this.selectedOfferingVersion = selectedOfferingVersion;
  }

  String getSessions() {
    return sessions;
  }

  void setSessions(String sessions) {
    this.sessions = sessions;
  }

  String getStatus() {
    return status;
  }

  void setStatus(String status) {
    this.status = status;
  }

  long getVersion() {
    return version;
  }

  void setVersion(long version) {
    this.version = version;
  }
}
