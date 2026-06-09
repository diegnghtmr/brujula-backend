package co.edu.uniquindio.brujula.offering.application.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Command DTO for the idempotent create-offering use case.
 *
 * <p>Carries inline schedule options (draft form) to be persisted atomically in the same
 * transaction as the parent offering.
 *
 * <p>{@code courseName} is NOT in the command — it is denormalized at service time via {@code
 * curriculum::api GetCourseQuery}.
 *
 * <p>Satisfies: OFFW-003.
 */
public record CreateOfferingCommand(
    UUID idempotencyKey,
    UUID actorId,
    UUID programId,
    UUID courseId,
    UUID periodId,
    String notes,
    List<ScheduleOptionDraft> scheduleOptions,
    String requestHash) {

  /**
   * Draft representation of a schedule option before it is assigned a persistent identity.
   *
   * @param label display label (e.g. "Grupo A")
   * @param journeyCode journey identifier (e.g. "DIURNA", "NOCTURNA")
   * @param sessions ordered list of time-slot drafts
   */
  public record ScheduleOptionDraft(
      String label, String journeyCode, List<SessionDraft> sessions) {}

  /**
   * Draft representation of a single time-slot within a schedule option.
   *
   * @param day weekday name (e.g. "MONDAY")
   * @param startsAt start time (inclusive)
   * @param endsAt end time (exclusive)
   */
  public record SessionDraft(String day, LocalTime startsAt, LocalTime endsAt) {}
}
