package co.edu.uniquindio.brujula.simulation.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure validation rule: detects schedule session overlaps between a candidate selection and the
 * existing selections in the same simulation (spec VAL-003).
 *
 * <p>Clash condition (strict interval overlap, same day): {@code candidateSession.startsAt <
 * existingSession.endsAt && candidateSession.endsAt > existingSession.startsAt}. Touching intervals
 * (candidate ends exactly when existing starts) are NOT a clash per spec.
 *
 * <p>Package-private — only used by {@link SimulationValidationService}. Pure Java, no Spring/JPA
 * (ADR-F4-004).
 */
class ScheduleClashDetector {

  /**
   * Returns the first clash notice found, if any.
   *
   * @param candidate the selection being added
   * @param existingSelections existing active selections in the simulation
   * @return optional blocking notice with code {@code SIMULATION_SCHEDULE_CLASH}
   */
  Optional<ValidationNotice> detect(
      CandidateSelection candidate, List<ExistingSelection> existingSelections) {

    for (ExistingSelection existing : existingSelections) {
      for (SimScheduleSession candidateSession : candidate.sessions()) {
        for (SimScheduleSession existingSession : existing.sessions()) {
          if (candidateSession.overlaps(existingSession)) {
            return Optional.of(buildNotice(candidateSession, existing.courseName()));
          }
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Collect ALL clash notices across existing selections (used for whole-simulation re-validation
   * in WU5). Each clash generates one notice with the specific day/time/conflictingCourse metadata.
   *
   * @param candidate the candidate being evaluated
   * @param existingSelections existing selections
   * @return list of blocking notices (may be empty)
   */
  List<ValidationNotice> detectAll(
      CandidateSelection candidate, List<ExistingSelection> existingSelections) {

    List<ValidationNotice> notices = new ArrayList<>();
    for (ExistingSelection existing : existingSelections) {
      for (SimScheduleSession candidateSession : candidate.sessions()) {
        for (SimScheduleSession existingSession : existing.sessions()) {
          if (candidateSession.overlaps(existingSession)) {
            notices.add(buildNotice(candidateSession, existing.courseName()));
          }
        }
      }
    }
    return notices;
  }

  private ValidationNotice buildNotice(
      SimScheduleSession clashingSession, String conflictingCourseName) {
    return new ValidationNotice(
        "SIMULATION_SCHEDULE_CLASH",
        "Schedule clash detected with course: " + conflictingCourseName,
        Map.of(
            "day", clashingSession.day(),
            "startsAt", clashingSession.startsAt().toString(),
            "endsAt", clashingSession.endsAt().toString(),
            "conflictingCourseName", conflictingCourseName));
  }
}
