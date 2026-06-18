package co.edu.uniquindio.brujula.curriculum.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Application-layer DTO for a Course read view.
 *
 * <p>Returned by query services to REST adapters — controllers do not touch domain. No
 * Spring/framework imports.
 *
 * @param id the course UUID
 * @param planId the study plan UUID this course belongs to
 * @param programId the program UUID
 * @param officialCode the minted internal code, e.g. {@code ISC-S2-PROG1} (nullable)
 * @param name the course name
 * @param type the course type (string representation of CourseType enum)
 * @param credits number of credits (≥ 0)
 * @param curricularWeeklyHours weekly hours in the curricular schedule
 * @param recommendedSemesters per-journey semester recommendations
 * @param status the course status string (ACTIVE, INACTIVE, ARCHIVED)
 * @param version the JPA optimistic-lock version; included so write operations can return the
 *     post-flush version to the caller (used by UpdateCourse and ArchiveCourse responses)
 */
public record CourseView(
    UUID id,
    UUID planId,
    UUID programId,
    String officialCode,
    String name,
    String type,
    int credits,
    int curricularWeeklyHours,
    List<RecommendedSemesterView> recommendedSemesters,
    String status,
    long version) {

  /** Nested DTO for a per-journey recommended semester. */
  public record RecommendedSemesterView(String journeyCode, int semester) {}
}
