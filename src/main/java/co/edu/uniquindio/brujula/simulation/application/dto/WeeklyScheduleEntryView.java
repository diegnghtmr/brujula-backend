package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.UUID;

/**
 * Application-layer view of a single weekly schedule entry (spec DET-001).
 *
 * <p>Produced by the REST mapper from {@link
 * co.edu.uniquindio.brujula.simulation.domain.WeeklyScheduleEntry}.
 *
 * <p>Plain String/UUID/primitive types only.
 *
 * @param day weekday string (e.g. "MONDAY")
 * @param startsAt HH:mm start time string
 * @param endsAt HH:mm end time string
 * @param courseName denormalized course name
 * @param programId program UUID
 * @param selectionId selection UUID (optional in contract)
 * @param inClash true if this session overlaps with any other in the simulation
 */
public record WeeklyScheduleEntryView(
    String day,
    String startsAt,
    String endsAt,
    String courseName,
    UUID programId,
    UUID selectionId,
    boolean inClash) {}
