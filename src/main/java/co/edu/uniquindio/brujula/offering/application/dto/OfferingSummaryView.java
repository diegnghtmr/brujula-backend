package co.edu.uniquindio.brujula.offering.application.dto;

import java.util.UUID;

/**
 * Read model DTO for listing offerings (student catalogue view).
 *
 * @param id the offering UUID
 * @param courseId opaque reference to curriculum::Course
 * @param courseName resolved course name ("N/A" until WU2 curriculum wiring)
 * @param programId opaque reference to programcatalog::Program
 * @param periodId opaque reference to the period
 * @param status offering status string (e.g. "PRELIMINARY", "FINAL")
 * @param notes optional staff notes
 * @param version optimistic lock version
 */
public record OfferingSummaryView(
    UUID id,
    UUID courseId,
    String courseName,
    UUID programId,
    UUID periodId,
    String status,
    String notes,
    Long version) {}
