package co.edu.uniquindio.brujula.offering.application.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command DTO for the optimistic-lock {@code updatePeriod} use case.
 *
 * <p>Plain types only (no domain VOs). NOT idempotent.
 *
 * @param periodId the target period UUID
 * @param actorId authenticated ADMIN user ID
 * @param startDate new start date (nullable — keeps current when null)
 * @param endDate new end date (nullable — keeps current when null)
 * @param status new status string (nullable — keeps current when null)
 * @param expectedVersion optimistic lock version from the client
 */
public record UpdatePeriodCommand(
    UUID periodId,
    UUID actorId,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    long expectedVersion) {}
