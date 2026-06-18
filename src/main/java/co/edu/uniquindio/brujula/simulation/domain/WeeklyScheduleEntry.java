package co.edu.uniquindio.brujula.simulation.domain;

import java.util.UUID;

/**
 * Domain value object: one session entry in the weekly schedule grid.
 *
 * <p>Produced by {@link WeeklyScheduleAssembler} from the set of active selections. Pure Java, no
 * Spring/JPA. Not the same as the generated API model — the REST mapper converts these to the
 * generated {@code WeeklyScheduleEntry}.
 *
 * @param day weekday string (e.g. "MONDAY", "WEDNESDAY")
 * @param startsAt session start time HH:mm
 * @param endsAt session end time HH:mm
 * @param courseName denormalized course name
 * @param programId program UUID
 * @param selectionId the selection UUID (optional in contract, always populated here)
 * @param inClash true if this session overlaps with any other session in the simulation
 */
public record WeeklyScheduleEntry(
    String day,
    String startsAt,
    String endsAt,
    String courseName,
    UUID programId,
    UUID selectionId,
    boolean inClash) {}
