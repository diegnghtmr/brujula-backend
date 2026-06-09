package co.edu.uniquindio.brujula.curriculum.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Command DTO for creating a new Course.
 *
 * <p>Carries all fields needed to create a Course aggregate. No framework imports.
 *
 * @param idempotencyKey unique per-request key for exactly-once semantics
 * @param requestHash hash of the canonical request body (for idempotency conflict detection)
 * @param actorId UUID of the authenticated actor performing the operation
 * @param planId the study plan this course belongs to
 * @param programId the program this course belongs to
 * @param name the course name (must not be blank)
 * @param officialCode optional official code (nullable; set later by minter if blank)
 * @param type the course type (string representation of CourseType enum)
 * @param credits number of credits (&gt;= 0)
 * @param curricularWeeklyHours weekly hours in the curricular schedule
 * @param recommendedSemesters per-journey semester recommendations
 */
public record CreateCourseCommand(
    UUID idempotencyKey,
    String requestHash,
    UUID actorId,
    UUID planId,
    UUID programId,
    String name,
    String officialCode,
    String type,
    int credits,
    int curricularWeeklyHours,
    List<CourseView.RecommendedSemesterView> recommendedSemesters) {}
