package co.edu.uniquindio.brujula.simulation.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure input record describing the schedule option the student wants to add.
 *
 * <p>Injected into the validation engine by the application service after resolving the offering
 * data through {@code OfferingReadPort}. No Spring/JPA dependencies.
 *
 * @param scheduleOptionId schedule option UUID
 * @param offeringId parent offering UUID
 * @param courseId course UUID (used by prereq + already-passed rules)
 * @param courseName human-readable course name (for metadata in notices)
 * @param programId program UUID (used by credit-limit rule)
 * @param credits credit load of the course
 * @param journeyCode journey identifier
 * @param offeringStatus offering status string (e.g. "FINAL", "ARCHIVED")
 * @param offeringVersion current offering version snapshot
 * @param sessions list of time slots (sim-local, not offering domain)
 */
public record CandidateSelection(
    UUID scheduleOptionId,
    UUID offeringId,
    UUID courseId,
    String courseName,
    UUID programId,
    double credits,
    String journeyCode,
    String offeringStatus,
    long offeringVersion,
    List<SimScheduleSession> sessions) {

  public CandidateSelection {
    Objects.requireNonNull(scheduleOptionId, "scheduleOptionId required");
    Objects.requireNonNull(offeringId, "offeringId required");
    Objects.requireNonNull(courseId, "courseId required");
    Objects.requireNonNull(courseName, "courseName required");
    Objects.requireNonNull(programId, "programId required");
    Objects.requireNonNull(offeringStatus, "offeringStatus required");
    sessions = sessions == null ? List.of() : List.copyOf(sessions);
  }
}
