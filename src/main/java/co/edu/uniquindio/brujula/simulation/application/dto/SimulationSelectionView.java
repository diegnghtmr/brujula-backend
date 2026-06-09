package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application-layer view of a simulation selection.
 *
 * <p>ARQ-001: only primitive/String/UUID types — no domain entities. Maps 1:1 to the generated
 * {@code SimulationSelection} API model.
 *
 * @param id selection UUID
 * @param offeringId parent offering UUID
 * @param scheduleOptionId schedule option UUID
 * @param courseId course UUID
 * @param courseName denormalized course name
 * @param programId program UUID
 * @param credits credit load
 * @param journeyCode journey identifier
 * @param offeringStatus snapshot of offering status at selection time
 * @param selectedOfferingVersion snapshot offering version (for staleness, STALE-001)
 * @param sessions list of session views [{day, startsAt, endsAt}]
 * @param warnings non-blocking advisory notices (empty in WU4a)
 */
public record SimulationSelectionView(
    UUID id,
    UUID offeringId,
    UUID scheduleOptionId,
    UUID courseId,
    String courseName,
    UUID programId,
    double credits,
    String journeyCode,
    String offeringStatus,
    Long selectedOfferingVersion,
    List<SessionView> sessions,
    List<ValidationNoticeView> warnings) {

  /** Sim-local session snapshot for the REST response. */
  public record SessionView(String day, String startsAt, String endsAt) {}

  /** Validation notice for the REST response (WU4a: warnings is always empty). */
  public record ValidationNoticeView(String code, String message, Map<String, Object> metadata) {}
}
