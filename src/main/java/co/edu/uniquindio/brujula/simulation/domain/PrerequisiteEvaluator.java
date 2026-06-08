package co.edu.uniquindio.brujula.simulation.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure validation rule: checks that the student has passed all official / HIGH-confidence
 * prerequisite courses for the candidate course (spec VAL-002).
 *
 * <p>Hard-edge policy (identical to Fase-3 {@code CourseAvailabilityService}):
 * <ul>
 *   <li>HARD edge (official OR confidence=HIGH): if not passed → {@code SIMULATION_PREREQUISITE_NOT_MET} (BLOCKING).
 *   <li>SOFT edge (inferred=true AND confidence ∈ MEDIUM/LOW/null): NOT evaluated in WU4a.
 *       TODO(WU4b): emit {@code SIMULATION_SOFT_PREREQ_WARNING} for unmet soft prereqs.
 * </ul>
 *
 * <p>Package-private — only used by {@link SimulationValidationService}.
 * Pure Java, no Spring/JPA (ARQ-002, ADR-F4-004).
 */
class PrerequisiteEvaluator {

  /**
   * Returns the first blocking prerequisite notice found, if any.
   *
   * @param candidate    the selection being added
   * @param requirements list of requirement edges for the candidate's course
   * @param passed       the set of courses the student has effectively passed
   * @return optional blocking notice with code {@code SIMULATION_PREREQUISITE_NOT_MET}
   */
  Optional<ValidationNotice> evaluate(
      CandidateSelection candidate,
      List<PrereqRequirement> requirements,
      PassedCourseSet passed) {

    for (PrereqRequirement req : requirements) {
      // Only evaluate COURSE_PASSED requirements (skip MINIMUM_CREDITS, SEQUENCE, CONFIGURABLE)
      if (!"COURSE_PASSED".equalsIgnoreCase(req.type())) {
        continue;
      }
      if (req.requiredCourseId() == null) {
        continue;
      }

      boolean prereqPassed = passed.hasPassed(req.requiredCourseId());
      if (prereqPassed) {
        continue;
      }

      if (req.isHard()) {
        // Unmet HARD prerequisite → BLOCKING
        String requiredName = req.requiredCourseName() != null
            ? req.requiredCourseName()
            : req.requiredCourseId().toString();
        return Optional.of(new ValidationNotice(
            "SIMULATION_PREREQUISITE_NOT_MET",
            "Prerequisite not met: " + requiredName,
            Map.of("requiredCourseName", requiredName)));
      }
      // SOFT edge (inferred=true AND confidence ∈ MEDIUM/LOW/null):
      // TODO(WU4b): emit SIMULATION_SOFT_PREREQ_WARNING for unmet soft prerequisites.
      // In WU4a, soft prereqs are silently skipped — the selection proceeds without notice.
    }
    return Optional.empty();
  }

  /**
   * Collect ALL blocking prerequisite notices (first-wins policy in addSelection, but
   * full-list used in review re-validation in WU5).
   */
  List<ValidationNotice> evaluateAll(
      CandidateSelection candidate,
      List<PrereqRequirement> requirements,
      PassedCourseSet passed) {

    List<ValidationNotice> notices = new ArrayList<>();
    for (PrereqRequirement req : requirements) {
      if (!"COURSE_PASSED".equalsIgnoreCase(req.type())) {
        continue;
      }
      if (req.requiredCourseId() == null) {
        continue;
      }
      if (passed.hasPassed(req.requiredCourseId())) {
        continue;
      }
      if (req.isHard()) {
        String requiredName = req.requiredCourseName() != null
            ? req.requiredCourseName()
            : req.requiredCourseId().toString();
        notices.add(new ValidationNotice(
            "SIMULATION_PREREQUISITE_NOT_MET",
            "Prerequisite not met: " + requiredName,
            Map.of("requiredCourseName", requiredName)));
      }
      // TODO(WU4b): collect SIMULATION_SOFT_PREREQ_WARNING for soft prereqs here.
    }
    return notices;
  }
}
