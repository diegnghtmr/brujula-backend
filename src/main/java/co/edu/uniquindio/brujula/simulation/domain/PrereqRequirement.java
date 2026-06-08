package co.edu.uniquindio.brujula.simulation.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Simulation-local equivalent of a prerequisite requirement edge.
 *
 * <p>Mirrors the structure of {@code CourseRequirementView} from curriculum::api but kept in the
 * simulation domain to preserve boundary purity. Mapped from {@code CurriculumReadPort} results
 * at the adapter edge (ARQ-002, INV-001).
 *
 * <p>Hard-edge policy (identical to Fase-3 {@code CourseRequirementEdge.isHard()}):
 * HARD = official ({@code inferred == false} or null) OR {@code confidence == HIGH}.
 * SOFT = {@code inferred == true} AND confidence ∈ {MEDIUM, LOW, null}.
 *
 * @param targetCourseId   the course that requires the prerequisite
 * @param requiredCourseId the prerequisite course UUID
 * @param type             requirement type string (e.g. "COURSE_PASSED")
 * @param confidence       confidence string (HIGH/MEDIUM/LOW; nullable)
 * @param inferred         whether inferred rather than officially documented (nullable; null → false)
 * @param requiredCourseName human-readable name for the required course (for metadata)
 */
public record PrereqRequirement(
    UUID targetCourseId,
    UUID requiredCourseId,
    String type,
    String confidence,
    Boolean inferred,
    String requiredCourseName) {

  public PrereqRequirement {
    Objects.requireNonNull(targetCourseId, "targetCourseId required");
    Objects.requireNonNull(requiredCourseId, "requiredCourseId required");
    Objects.requireNonNull(type, "type required");
  }

  /**
   * Returns true when this edge is HARD — an unmet prerequisite BLOCKS the selection.
   *
   * <p>HARD = official (inferred == false or null) OR confidence == HIGH.
   * Mirrors Fase-3 {@code CourseRequirementEdge.isHard()}.
   */
  public boolean isHard() {
    boolean official = inferred == null || !inferred;
    boolean highConfidence = "HIGH".equalsIgnoreCase(confidence);
    return official || highConfidence;
  }
}
