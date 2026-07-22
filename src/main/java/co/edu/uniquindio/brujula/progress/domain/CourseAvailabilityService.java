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
 * performs in-memory computation. Mirrors {@code PrerequisiteAcyclicityService} purity. Enforced by
 * {@code ProgressDomainPurityTest}.
 *
 * <p>Algorithm:
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
 * <p>Handles HARD-prereq blocking, SOFT-prereq advisory warnings, and passed-course short-circuit.
 */
public class CourseAvailabilityService {

  /**
   * Compute per-course availability. Equivalent to {@link #compute(List, List, Map, Set)} with an
   * empty pool-member set — any CONFIGURABLE pool-gate edge is therefore always unmet (0 distinct
   * passed members). Preserved for callers that never deal with pool-gate requirements.
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
    return compute(planCourses, requirements, effectiveByCourse, Set.of());
  }

  /**
   * Compute per-course availability, including CONFIGURABLE pool-gate evaluation (PROP-001 design
   * D1 — e.g. the profundización thesis gate: "≥ N distinct passed pool electives").
   *
   * @param planCourses the full set of courses in the plan (bulk-loaded by the application layer)
   * @param requirements all requirement edges for this plan's courses (bulk-loaded by the
   *     application layer — no N+1)
   * @param effectiveByCourse effective declared status per courseId (from {@link
   *     EffectiveAttemptResolver})
   * @param poolMemberCourseIds the set of courseIds eligible to count toward a CONFIGURABLE
   *     pool-gate requirement (e.g. the ACTIVE DEEPENING electives, excluding legacy display-only
   *     slot anchors). Supplied by the application layer — this service never hardcodes specific
   *     course identities.
   * @return map of courseId → {@link CourseAvailability}; never null; one entry per plan course
   */
  public Map<UUID, CourseAvailability> compute(
      List<PlanCourse> planCourses,
      List<CourseRequirementEdge> requirements,
      Map<UUID, CourseProgressStatus> effectiveByCourse,
      Set<UUID> poolMemberCourseIds) {

    if (planCourses == null || planCourses.isEmpty()) {
      return Map.of();
    }

    Set<UUID> poolMembers = poolMemberCourseIds != null ? poolMemberCourseIds : Set.of();

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
          if ("CONFIGURABLE".equalsIgnoreCase(edge.type()) && edge.requiredCredits() != null) {
            // Pool gate (PROP-001 design D1): satisfied iff COUNT(DISTINCT passed pool-member
            // courseId) >= edge.requiredCredits(). Pool membership is supplied by the caller
            // (poolMembers) — this service never hardcodes specific course identities.
            long distinctPassedPoolMembers =
                poolMembers.stream()
                    .filter(id -> CourseProgressStatus.PASSED.equals(effectiveByCourse.get(id)))
                    .count();
            boolean gateSatisfied = distinctPassedPoolMembers >= edge.requiredCredits();
            if (!gateSatisfied) {
              unmet.add(new UnmetRequirement(null, edge.type(), false));
              blocked = true;
            }
            continue;
          }
          // TODO: MINIMUM_CREDITS edges (and CONFIGURABLE edges with no requiredCredits
          // threshold) are silently skipped. A future implementation should evaluate credit
          // totals against the threshold value stored in the edge.
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
