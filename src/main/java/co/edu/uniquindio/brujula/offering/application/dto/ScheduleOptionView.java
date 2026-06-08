package co.edu.uniquindio.brujula.offering.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Read model DTO for a schedule option within an offering.
 *
 * @param id          the schedule option UUID
 * @param offeringId  the parent offering UUID (required for WU4 snapshot and INV-002)
 * @param label       display label (e.g. "Grupo A")
 * @param journeyCode journey identifier (e.g. "DIURNA", "NOCTURNA")
 * @param weeklyHours computed weekly hours
 * @param version     optimistic lock version for staleness detection
 * @param sessions    ordered list of time slots
 */
public record ScheduleOptionView(
    UUID id,
    UUID offeringId,
    String label,
    String journeyCode,
    double weeklyHours,
    Long version,
    List<ScheduleSessionView> sessions) {}
