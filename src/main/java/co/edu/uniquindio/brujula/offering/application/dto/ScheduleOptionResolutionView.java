package co.edu.uniquindio.brujula.offering.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Read model DTO returned when the simulator resolves a schedule option.
 *
 * <p>Flat structure: includes the schedule option fields plus parent offering context so the
 * simulator can validate cross-domain rules without additional calls.
 *
 * @param scheduleOptionId the schedule option UUID
 * @param offeringId the parent offering UUID
 * @param courseId opaque reference to curriculum::Course
 * @param courseName denormalized course name snapshot (WU4 stores this in SimulationSelection)
 * @param programId opaque reference to programcatalog::Program
 * @param periodId opaque reference to the period
 * @param label schedule option label
 * @param journeyCode journey identifier
 * @param weeklyHours computed weekly hours
 * @param offeringStatus offering status string
 * @param offeringVersion snapshot version of the offering at resolution time
 * @param sessions ordered list of time slots
 */
public record ScheduleOptionResolutionView(
    UUID scheduleOptionId,
    UUID offeringId,
    UUID courseId,
    String courseName,
    UUID programId,
    UUID periodId,
    String label,
    String journeyCode,
    double weeklyHours,
    String offeringStatus,
    long offeringVersion,
    List<ScheduleSessionView> sessions) {}
