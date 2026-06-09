package co.edu.uniquindio.brujula.progress.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Domain service: computes the {@link CourseAvailability} for each course in a plan.
 *
 * <p>Pure function — no Spring beans, no ports, no I/O. All inputs are passed in; this service only
 * performs in-memory computation (ADR-F3-004). Mirrors {@code PrerequisiteAcyclicityService}
 * purity. Enforced by {@code ProgressDomainPurityTest}.
 *
 * <p>Algorithm per ADR-F3-004:
 *
 * <ol>
 *   <li>If the course already has an effective declared status (PASSED/FAILED/CANCELLED), that
 *       status is returned as-is (the student's recorded outcome wins).
 *   <li>For courses with no attempts (status not in effectiveByCourse), evaluate requirement edges:
 *       <ul>
 *         <li>HARD edge (official or HIGH confidence): if the required course is NOT effectively
 *             PASSED → blocking {@link UnmetRequirement} (advisory=false).
 *         <li>SOFT edge (inferred=true AND confidence ∈ MEDIUM/LOW/null): if unmet → advisory
 *             {@link UnmetRequirement} (advisory=true). NEVER blocks.
 *       </ul>
 *   <li>Any blocking unmet requirement → BLOCKED; else → AVAILABLE. Both blocking and advisory
 *       entries are included in the {@code unmetRequirements} list for UI rendering.
 * </ol>
 *
 * <p>Satisfies: AVL-1-HP, AVL-1-BLOCKED, AVL-1-SOFT-PREREQ, AVL-1-PASSED-COURSE.
 */
public class CourseAvailabilityService {

  /**
   * Compute per-course availability.
   *
   * @param planCourses the full set of courses in the plan (bulk-loaded by the application layer)
   * @param requirements all requirement edges for this plan's courses (bulk-loaded by the
   *     application layer — no N+1)
   * @param effectiveByCourse effective declared status per courseId (from {@link
   *     EffectiveAttemptResolver})
   * @return map of courseId → {@link CourseAvailability}; never null; one entry per plan course
   */
  public Map<UUID, CourseAvailability> compute(
      List<PlanCourse> planCourses,
      List<CourseRequirementEdge> requirements,
      Map<UUID, CourseProgressStatus> effectiveByCourse) {

    if (planCourses == null || planCourses.isEmpty()) {
      return Map.of();
    }

    // Build a lookup: targetCourseId → list of edges targeting that course
    Map<UUID, List<CourseRequirementEdge>> edgesByTarget = new HashMap<>();
    if (requirements != null) {
      for (CourseRequirementEdge edge : requirements) {
        edgesByTarget.computeIfAbsent(edge.targetCourseId(), k -> new ArrayList<>()).add(edge);
      }
    }

    // Set of courseIds in the plan (for off-plan detection later if needed)
    Set<UUID> planCourseIds =
        planCourses.stream().map(PlanCourse::courseId).collect(Collectors.toSet());

    Map<UUID, CourseAvailability> result = new HashMap<>();

    for (PlanCourse course : planCourses) {
      UUID courseId = course.courseId();

      // If the student has a declared effective status for this course, use it directly
      CourseProgressStatus effectiveStatus = effectiveByCourse.get(courseId);
      if (effectiveStatus != null) {
        result.put(courseId, new CourseAvailability(effectiveStatus, List.of()));
        continue;
      }

      // No attempts — evaluate prerequisites
      List<CourseRequirementEdge> courseEdges = edgesByTarget.getOrDefault(courseId, List.of());
      List<UnmetRequirement> unmet = new ArrayList<>();
      boolean blocked = false;

      for (CourseRequirementEdge edge : courseEdges) {
        UUID requiredCourseId = edge.requiredCourseId();
        if (requiredCourseId == null) {
          // TODO (FIX-S2): MINIMUM_CREDITS and CONFIGURABLE edges with no specific requiredCourseId
          // are silently skipped here. A future implementation should evaluate credit totals
          // against
          // the threshold value stored in the edge (out of WU4 scope; ADR-F3 deferred).
          continue;
        }

        // Check if the required course is effectively PASSED
        boolean prereqPassed =
            CourseProgressStatus.PASSED.equals(effectiveByCourse.get(requiredCourseId));
        if (prereqPassed) {
          continue; // prereq satisfied
        }

        // Prereq NOT passed — classify edge as HARD or SOFT
        if (edge.isHard()) {
          unmet.add(new UnmetRequirement(requiredCourseId, edge.type(), false));
          blocked = true;
        } else {
          // SOFT edge: advisory only
          unmet.add(new UnmetRequirement(requiredCourseId, edge.type(), true));
        }
      }

      CourseProgressStatus computedStatus =
          blocked ? CourseProgressStatus.BLOCKED : CourseProgressStatus.AVAILABLE;
      result.put(courseId, new CourseAvailability(computedStatus, List.copyOf(unmet)));
    }

    return result;
  }
}
