package co.edu.uniquindio.brujula.offering.application.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read model DTO for an academic period.
 *
 * @param id        the period UUID
 * @param name      human-readable name, e.g. "2026-2"
 * @param startDate period start date (inclusive)
 * @param endDate   period end date (inclusive)
 * @param status    period lifecycle status string (e.g. "ACTIVE")
 * @param version   optimistic lock version
 */
public record AcademicPeriodView(
    UUID id,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    Long version) {}
