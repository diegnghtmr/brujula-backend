package co.edu.uniquindio.brujula.offering.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Read model DTO for a single offering detail (full view).
 *
 * @param id the offering UUID
 * @param courseId opaque reference to curriculum::Course
 * @param courseName denormalized course name (point-in-time snapshot from course_offering)
 * @param programId opaque reference to programcatalog::Program
 * @param periodId opaque reference to the period
 * @param status offering status string
 * @param notes optional staff notes
 * @param version optimistic lock version for staleness detection
 * @param scheduleOptions the available schedule options for this offering
 * @param intensityWarning advisory flag set when any schedule option's weekly hours do not match
 *     the curricular weekly hours for the course (OFFW-005); null when the field was not computed
 *     (e.g. read-only queries)
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
    List<ScheduleOptionView> scheduleOptions,
    Boolean intensityWarning) {

  /**
   * Compact convenience constructor for callers that do not compute the intensity warning (pre-WU3
   * callers — create, update, read queries). Sets {@code intensityWarning} to null.
   */
  public OfferingDetailView(
      UUID id,
      UUID courseId,
      String courseName,
      UUID programId,
      UUID periodId,
      String status,
      String notes,
      Long version,
      List<ScheduleOptionView> scheduleOptions) {
    this(
        id,
        courseId,
        courseName,
        programId,
        periodId,
        status,
        notes,
        version,
        scheduleOptions,
        null);
  }
}
