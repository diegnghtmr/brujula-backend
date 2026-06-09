package co.edu.uniquindio.brujula.curriculum.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Command DTO for updating an existing Course (optimistic lock, NOT idempotent).
 *
 * <p>No framework imports.
 *
 * @param actorId UUID of the authenticated actor performing the operation
 * @param courseId the course to update
 * @param expectedVersion the client's known version (optimistic lock check)
 * @param name the new course name (must not be blank)
 * @param officialCode the new official code (nullable)
 * @param type the new course type string
 * @param credits the new credit count
 * @param curricularWeeklyHours the new weekly hours
 * @param recommendedSemesters the new per-journey recommendations
 */
public record UpdateCourseCommand(
    UUID actorId,
    UUID courseId,
    long expectedVersion,
    String name,
    String officialCode,
    String type,
    int credits,
    int curricularWeeklyHours,
    List<CourseView.RecommendedSemesterView> recommendedSemesters) {}
