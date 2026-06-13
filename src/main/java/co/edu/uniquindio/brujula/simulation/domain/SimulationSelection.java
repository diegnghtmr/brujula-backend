package co.edu.uniquindio.brujula.simulation.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain object representing a student's selection of a schedule option within a simulation.
 *
 * <p>Stores a snapshot of the offering state at selection time ({@code selectedOfferingVersion},
 * {@code offeringStatus}, {@code sessions}) to support staleness detection.
 *
 * <p>Framework-free — no Spring/JPA annotations. Lifecycle is ACTIVE or DELETED (soft delete).
 * CURRENT/OUTDATED are never persisted here.
 */
public class SimulationSelection {

  private final SimulationSelectionId id;
  private final UUID simulationId;
  private final UUID offeringId;
  private final UUID scheduleOptionId;
  private final UUID courseId;
  private final String courseName;
  private final UUID programId;
  private final double credits;
  private final String journeyCode;

  /** Snapshot of offering status at selection time. */
  private final String offeringStatus;

  /** Snapshot of the offering version at selection time (for staleness detection). */
  private final long selectedOfferingVersion;

  /** Denormalised snapshot of schedule sessions (JSONB in persistence). */
  private final List<SimScheduleSession> sessions;

  /** Lifecycle: ACTIVE or DELETED (soft delete). */
  private String status;

  private long version;

  /** Full constructor — used by reconstitute() and the persistence mapper. */
  SimulationSelection(
      SimulationSelectionId id,
      UUID simulationId,
      UUID offeringId,
      UUID scheduleOptionId,
      UUID courseId,
      String courseName,
      UUID programId,
      double credits,
      String journeyCode,
      String offeringStatus,
      long selectedOfferingVersion,
      List<SimScheduleSession> sessions,
      String status,
      long version) {
    this.id = id;
    this.simulationId = simulationId;
    this.offeringId = offeringId;
    this.scheduleOptionId = scheduleOptionId;
    this.courseId = courseId;
    this.courseName = courseName;
    this.programId = programId;
    this.credits = credits;
    this.journeyCode = journeyCode;
    this.offeringStatus = offeringStatus;
    this.selectedOfferingVersion = selectedOfferingVersion;
    this.sessions = sessions == null ? List.of() : List.copyOf(sessions);
    this.status = status;
    this.version = version;
  }

  /** Factory: create a new selection from a resolved candidate. */
  public static SimulationSelection create(
      SimulationSelectionId id, UUID simulationId, CandidateSelection candidate) {
    Objects.requireNonNull(id, "id required");
    Objects.requireNonNull(simulationId, "simulationId required");
    Objects.requireNonNull(candidate, "candidate required");
    return new SimulationSelection(
        id,
        simulationId,
        candidate.offeringId(),
        candidate.scheduleOptionId(),
        candidate.courseId(),
        candidate.courseName(),
        candidate.programId(),
        candidate.credits(),
        candidate.journeyCode(),
        candidate.offeringStatus(),
        candidate.offeringVersion(),
        candidate.sessions(),
        "ACTIVE",
        0L);
  }

  /** Reconstitute from persistence. Called by the persistence mapper only. */
  public static SimulationSelection reconstitute(
      SimulationSelectionId id,
      UUID simulationId,
      UUID offeringId,
      UUID scheduleOptionId,
      UUID courseId,
      String courseName,
      UUID programId,
      double credits,
      String journeyCode,
      String offeringStatus,
      long selectedOfferingVersion,
      List<SimScheduleSession> sessions,
      String status,
      long version) {
    return new SimulationSelection(
        id,
        simulationId,
        offeringId,
        scheduleOptionId,
        courseId,
        courseName,
        programId,
        credits,
        journeyCode,
        offeringStatus,
        selectedOfferingVersion,
        sessions,
        status,
        version);
  }

  /** Soft-delete this selection. */
  public void markDeleted() {
    this.status = "DELETED";
  }

  /**
   * Return a new selection with a refreshed offering snapshot (used by reviewSimulation). Bumps
   * {@code selectedOfferingVersion} to the current version and updates the sessions snapshot.
   *
   * @param currentOfferingVersion the live offering version
   * @param currentSessions fresh session snapshot from the offering module
   * @param currentOfferingStatus current offering status string
   * @return new selection with refreshed snapshot; same id, simulationId, course data
   */
  public SimulationSelection withRefreshedSnapshot(
      long currentOfferingVersion,
      List<SimScheduleSession> currentSessions,
      String currentOfferingStatus) {
    return new SimulationSelection(
        this.id,
        this.simulationId,
        this.offeringId,
        this.scheduleOptionId,
        this.courseId,
        this.courseName,
        this.programId,
        this.credits,
        this.journeyCode,
        currentOfferingStatus,
        currentOfferingVersion,
        currentSessions,
        this.status,
        this.version);
  }

  // -------------------------------------------------------------------------
  // Accessors
  // -------------------------------------------------------------------------

  public SimulationSelectionId getId() {
    return id;
  }

  public UUID getSimulationId() {
    return simulationId;
  }

  public UUID getOfferingId() {
    return offeringId;
  }

  public UUID getScheduleOptionId() {
    return scheduleOptionId;
  }

  public UUID getCourseId() {
    return courseId;
  }

  public String getCourseName() {
    return courseName;
  }

  public UUID getProgramId() {
    return programId;
  }

  public double getCredits() {
    return credits;
  }

  public String getJourneyCode() {
    return journeyCode;
  }

  public String getOfferingStatus() {
    return offeringStatus;
  }

  public long getSelectedOfferingVersion() {
    return selectedOfferingVersion;
  }

  public List<SimScheduleSession> getSessions() {
    return sessions;
  }

  public String getStatus() {
    return status;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }
}
