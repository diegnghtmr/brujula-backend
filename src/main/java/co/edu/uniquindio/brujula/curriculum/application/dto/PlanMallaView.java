package co.edu.uniquindio.brujula.curriculum.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Application-layer DTO for the plan-level malla (curricular grid) view.
 *
 * <p>Returned by {@link
 * co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery}. Contains ACTIVE
 * courses grouped by recommended semester for a given journey code. Assembled in-memory (one
 * indexed query per plan + group by semester in Java).
 *
 * <p>No Spring/framework imports.
 *
 * @param planId the study plan UUID
 * @param journeyCode the journey code used for semester lookup (e.g. "DIURNA", "NOCTURNA")
 * @param semesterBuckets list of semester buckets ordered by recommendedSemester ascending
 * @param graduationRequirements static advisory text describing graduation gates for this plan
 *     (e.g. language proficiency, interdisciplinary seminar). Informational only — not a modelled
 *     entity.
 * @param poolCourses ACTIVE courses that belong to a pool-membership requirement (PROP-001 design
 *     D1) but carry NO recommended-semester row for any journey, so they never appear in {@code
 *     semesterBuckets} (e.g. the profundización electives, as opposed to the 4 legacy slot anchors
 *     which DO have a semester row and therefore show up in the buckets). Never null; may be empty.
 */
public record PlanMallaView(
    UUID planId,
    String journeyCode,
    List<SemesterBucket> semesterBuckets,
    String graduationRequirements,
    List<CourseSummary> poolCourses) {

  /**
   * Convenience constructor preserving the original 4-arg shape (no pool courses). Backward
   * compatible with all pre-PROP-001 call sites.
   */
  public PlanMallaView(
      UUID planId,
      String journeyCode,
      List<SemesterBucket> semesterBuckets,
      String graduationRequirements) {
    this(planId, journeyCode, semesterBuckets, graduationRequirements, List.of());
  }

  /**
   * A single semester bucket grouping all active courses recommended for that semester.
   *
   * @param recommendedSemester the semester number (1-based)
   * @param courses courses recommended in this semester for the requested journey
   */
  public record SemesterBucket(int recommendedSemester, List<CourseSummary> courses) {}

  /**
   * Summary of a course within a semester bucket, including its prerequisite/co-requisite edges.
   *
   * @param id the course UUID
   * @param officialCode the internal code (e.g. "ISC-S1-PROG1"), nullable
   * @param name the course name
   * @param type the course type string (REQUIRED, PERSONAL_ELECTIVE, etc.)
   * @param credits number of credits
   * @param requirements the list of requirement edges for this course
   */
  public record CourseSummary(
      UUID id,
      String officialCode,
      String name,
      String type,
      int credits,
      List<RequirementSummary> requirements) {}

  /**
   * Compact requirement edge included in each malla course summary.
   *
   * @param type the requirement type (COURSE_PASSED, SEQUENCE, MINIMUM_CREDITS, CONFIGURABLE)
   * @param requiredCourseId the prerequisite course UUID (nullable for
   *     MINIMUM_CREDITS/CONFIGURABLE)
   * @param confidence confidence level of this edge (HIGH/MEDIUM/LOW; nullable)
   * @param inferred whether this edge was inferred rather than officially documented (nullable)
   * @param simultaneityAllowed whether simultaneous enrollment is allowed (nullable)
   * @param requiredCredits threshold value for CONFIGURABLE/MINIMUM_CREDITS edges (nullable). For a
   *     CONFIGURABLE pool-gate edge (PROP-001 design D1), reinterpreted as "distinct completions
   *     required".
   */
  public record RequirementSummary(
      String type,
      UUID requiredCourseId,
      String confidence,
      Boolean inferred,
      Boolean simultaneityAllowed,
      Integer requiredCredits) {

    /**
     * Convenience constructor preserving the original 5-arg shape (no {@code requiredCredits}).
     * Backward compatible with all pre-PROP-001 call sites.
     */
    public RequirementSummary(
        String type,
        UUID requiredCourseId,
        String confidence,
        Boolean inferred,
        Boolean simultaneityAllowed) {
      this(type, requiredCourseId, confidence, inferred, simultaneityAllowed, null);
    }
  }
}
