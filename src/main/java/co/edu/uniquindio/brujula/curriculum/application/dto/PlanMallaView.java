package co.edu.uniquindio.brujula.curriculum.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Application-layer DTO for the plan-level malla (curricular grid) view.
 *
 * <p>Returned by {@link co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery}.
 * Contains ACTIVE courses grouped by recommended semester for a given journey code.
 * Assembled in-memory (ADR-F2-001: one indexed query per plan + group by semester in Java).
 *
 * <p>No Spring/framework imports.
 *
 * @param planId the study plan UUID
 * @param journeyCode the journey code used for semester lookup (e.g. "DIURNA", "NOCTURNA")
 * @param semesterBuckets list of semester buckets ordered by recommendedSemester ascending
 * @param graduationRequirements static advisory text describing graduation gates for this plan
 *                               (e.g. language proficiency, interdisciplinary seminar). Informational
 *                               only — not a modelled entity (CUR-008 §3).
 */
public record PlanMallaView(
    UUID planId,
    String journeyCode,
    List<SemesterBucket> semesterBuckets,
    String graduationRequirements) {

  /**
   * A single semester bucket grouping all active courses recommended for that semester.
   *
   * @param recommendedSemester the semester number (1-based)
   * @param courses courses recommended in this semester for the requested journey
   */
  public record SemesterBucket(
      int recommendedSemester,
      List<CourseSummary> courses) {}

  /**
   * Summary of a course within a semester bucket, including its prerequisite/co-requisite edges.
   *
   * @param id the course UUID
   * @param officialCode the internal code (e.g. "ISC-S1-PROG1"), nullable
   * @param name the course name
   * @param type the course type string (REQUIRED, PERSONAL_ELECTIVE, etc.)
   * @param credits number of credits
   * @param requirements the list of requirement edges for this course (CUR-008 §2)
   */
  public record CourseSummary(
      UUID id,
      String officialCode,
      String name,
      String type,
      int credits,
      List<RequirementSummary> requirements) {}

  /**
   * Compact requirement edge included in each malla course summary (CUR-008 §2).
   *
   * @param type the requirement type (COURSE_PASSED, SEQUENCE, MINIMUM_CREDITS, CONFIGURABLE)
   * @param requiredCourseId the prerequisite course UUID (nullable for MINIMUM_CREDITS/CONFIGURABLE)
   * @param confidence confidence level of this edge (HIGH/MEDIUM/LOW; nullable)
   * @param inferred whether this edge was inferred rather than officially documented (nullable)
   * @param simultaneityAllowed whether simultaneous enrollment is allowed (nullable)
   */
  public record RequirementSummary(
      String type,
      UUID requiredCourseId,
      String confidence,
      Boolean inferred,
      Boolean simultaneityAllowed) {}
}
