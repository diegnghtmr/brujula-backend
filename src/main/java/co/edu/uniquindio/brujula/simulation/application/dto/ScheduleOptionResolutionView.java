package co.edu.uniquindio.brujula.simulation.application.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Simulation-side resolution view of a schedule option.
 *
 * <p>Returned by {@link
 * co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort#resolveScheduleOption(UUID)}.
 * Kept in the simulation application layer so that the domain and application layers have ZERO
 * dependency on {@code offering::api-dto} types.
 *
 * <p>Mapping from {@code offering.application.dto.ScheduleOptionResolutionView} happens in {@code
 * OfferingReadAdapter} (infrastructure, which IS allowed to import offering::api-dto).
 *
 * @param scheduleOptionId schedule option UUID
 * @param offeringId parent offering UUID
 * @param courseId course UUID (opaque cross-schema reference)
 * @param courseName denormalized course name snapshot
 * @param programId program UUID
 * @param periodId period UUID
 * @param journeyCode journey identifier
 * @param weeklyHours computed weekly hours
 * @param offeringStatus offering status string ("DRAFT","PRELIMINARY","FINAL","ARCHIVED")
 * @param offeringVersion current offering version (snapshot at resolution time)
 * @param sessions list of time slots
 */
public record ScheduleOptionResolutionView(
    UUID scheduleOptionId,
    UUID offeringId,
    UUID courseId,
    String courseName,
    UUID programId,
    UUID periodId,
    String journeyCode,
    double weeklyHours,
    String offeringStatus,
    long offeringVersion,
    List<SessionSlot> sessions) {

  /** Sim-local session slot within the resolution view. */
  public record SessionSlot(String day, LocalTime startsAt, LocalTime endsAt) {}
}
