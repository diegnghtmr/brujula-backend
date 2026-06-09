package co.edu.uniquindio.brujula.simulation.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable snapshot of an already-persisted selection within the same simulation.
 *
 * <p>Used by validation rules that need to compare the candidate against existing state (schedule
 * clash, credit summation, duplicate detection).
 *
 * @param selectionId selection UUID
 * @param courseId course UUID
 * @param courseName human-readable course name
 * @param programId program UUID
 * @param credits credit load
 * @param sessions time slots (sim-local records)
 */
public record ExistingSelection(
    UUID selectionId,
    UUID courseId,
    String courseName,
    UUID programId,
    double credits,
    List<SimScheduleSession> sessions) {

  public ExistingSelection {
    Objects.requireNonNull(selectionId, "selectionId required");
    Objects.requireNonNull(courseId, "courseId required");
    Objects.requireNonNull(courseName, "courseName required");
    Objects.requireNonNull(programId, "programId required");
    sessions = sessions == null ? List.of() : List.copyOf(sessions);
  }
}
