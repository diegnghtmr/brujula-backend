package co.edu.uniquindio.brujula.progress.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable view of a requirement edge between two courses in the curriculum graph.
 *
 * <p>Input to {@link CourseAvailabilityService}. Produced by the application layer from the
 * curriculum published port ({@code PlanMallaView.RequirementSummary}). No Spring/JPA imports.
 *
 * <p>Edge classification:
 *
 * <ul>
 *   <li>HARD edge = official ({@code inferred=false}) OR {@code confidence=HIGH}. An unmet HARD
 *       edge BLOCKS the target course.
 *   <li>SOFT edge = {@code inferred=true} AND {@code confidence ∈ {MEDIUM, LOW, null}}. An unmet
 *       SOFT edge produces an advisory warning but NEVER blocks.
 * </ul>
 *
 * @param targetCourseId the course that has this requirement
 * @param requiredCourseId the prerequisite course UUID (nullable for MINIMUM_CREDITS/CONFIGURABLE)
 * @param type the requirement type (COURSE_PASSED, SEQUENCE, MINIMUM_CREDITS, CONFIGURABLE)
 * @param confidence confidence level (HIGH/MEDIUM/LOW; nullable)
 * @param inferred whether this edge was inferred rather than officially documented (nullable; null
 *     treated as false = official)
 * @param requiredCredits threshold value for CONFIGURABLE/MINIMUM_CREDITS edges (nullable). For a
 *     CONFIGURABLE pool-gate edge (PROP-001 design D1), reinterpreted as "distinct completions
 *     required" — see {@link CourseAvailabilityService}.
 */
public record CourseRequirementEdge(
    UUID targetCourseId,
    UUID requiredCourseId,
    String type,
    String confidence,
    Boolean inferred,
    Integer requiredCredits) {

  public CourseRequirementEdge {
    Objects.requireNonNull(targetCourseId, "targetCourseId must not be null");
    Objects.requireNonNull(type, "type must not be null");
  }

  /**
   * Convenience constructor preserving the original 5-arg shape (no {@code requiredCredits}).
   * Backward-compatible with all pre-PROP-001 call sites.
   */
  public CourseRequirementEdge(
      UUID targetCourseId,
      UUID requiredCourseId,
      String type,
      String confidence,
      Boolean inferred) {
    this(targetCourseId, requiredCourseId, type, confidence, inferred, null);
  }

  /**
   * Returns true when this edge is HARD — i.e., an unmet prerequisite blocks the target course.
   *
   * <p>HARD = official ({@code inferred == false} or null) OR {@code confidence == HIGH}.
   */
  public boolean isHard() {
    boolean official = inferred == null || !inferred;
    boolean highConfidence = "HIGH".equalsIgnoreCase(confidence);
    return official || highConfidence;
  }
}
