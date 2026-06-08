package co.edu.uniquindio.brujula.offering.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Read model DTO for a single offering detail (full view).
 *
 * @param id              the offering UUID
 * @param courseId        opaque reference to curriculum::Course
 * @param courseName      denormalized course name (point-in-time snapshot from course_offering)
 * @param programId       opaque reference to programcatalog::Program
 * @param periodId        opaque reference to the period
 * @param status          offering status string
 * @param notes           optional staff notes
 * @param version         optimistic lock version for staleness detection
 * @param scheduleOptions the available schedule options for this offering
 */
public record OfferingDetailView(
    UUID id,
    UUID courseId,
    String courseName,
    UUID programId,
    UUID periodId,
    String status,
    String notes,
    Long version,
    List<ScheduleOptionView> scheduleOptions) {}
