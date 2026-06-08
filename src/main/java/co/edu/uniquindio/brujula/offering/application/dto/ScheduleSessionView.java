package co.edu.uniquindio.brujula.offering.application.dto;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Read model DTO for a single schedule session (time slot).
 *
 * @param id        the session UUID
 * @param day       weekday string (e.g. "MONDAY")
 * @param startsAt  session start time
 * @param endsAt    session end time
 */
public record ScheduleSessionView(
    UUID id,
    String day,
    LocalTime startsAt,
    LocalTime endsAt) {}
