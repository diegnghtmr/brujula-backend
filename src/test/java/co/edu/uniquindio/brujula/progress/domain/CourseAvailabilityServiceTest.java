package co.edu.uniquindio.brujula.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CourseAvailabilityService}.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>All hard prereqs met → AVAILABLE.
 *   <li>Official/HIGH prereq unmet → BLOCKED with reason.
 *   <li>Inferred MEDIUM prereq unmet → AVAILABLE + advisory in unmet list.
 *   <li>Course with effective PASSED → PASSED status.
 *   <li>Empty plan → empty result.
 * </ul>
 */
class CourseAvailabilityServiceTest {

  private final CourseAvailabilityService sut = new CourseAvailabilityService();

  // -----------------------------------------------------------------------
  // All hard prereqs met → AVAILABLE
  // -----------------------------------------------------------------------

  @Test
  void course_with_all_hard_prereqs_passed_is_available() {
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();

    List<PlanCourse> courses =
        List.of(
            new PlanCourse(c1, "Course 1", 3, "REQUIRED", 1),
            new PlanCourse(c2, "Course 2", 3, "REQUIRED", 2));

    // c2 requires c1 with HIGH confidence (HARD edge)
    List<CourseRequirementEdge> requirements =
        List.of(new CourseRequirementEdge(c2, c1, "COURSE_PASSED", "HIGH", false));

    // c1 is PASSED → c2 should be AVAILABLE
    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of(c1, CourseProgressStatus.PASSED);

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result).containsKey(c2);
    assertThat(result.get(c2).status()).isEqualTo(CourseProgressStatus.AVAILABLE);
    assertThat(result.get(c2).unmetRequirements()).isEmpty();
  }

  // -----------------------------------------------------------------------
  // Official (inferred=false) or HIGH confidence prereq unmet → BLOCKED
  // -----------------------------------------------------------------------

  @Test
  void course_with_unmet_official_prereq_is_blocked() {
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();

    List<PlanCourse> courses =
        List.of(
            new PlanCourse(c1, "Course 1", 3, "REQUIRED", 1),
            new PlanCourse(c2, "Course 2", 3, "REQUIRED", 2));

    // c2 requires c1 with HIGH confidence (HARD edge) — c1 NOT passed
    List<CourseRequirementEdge> requirements =
        List.of(new CourseRequirementEdge(c2, c1, "COURSE_PASSED", "HIGH", false));

    // c1 has no attempt — not in effectiveByCourse
    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of();

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result).containsKey(c2);
    assertThat(result.get(c2).status()).isEqualTo(CourseProgressStatus.BLOCKED);
    assertThat(result.get(c2).unmetRequirements())
        .hasSize(1)
        .first()
        .extracting(UnmetRequirement::requiredCourseId)
        .isEqualTo(c1);
    assertThat(result.get(c2).unmetRequirements().getFirst().advisory()).isFalse();
  }

  @Test
  void course_with_unmet_inferred_false_prereq_of_medium_confidence_is_blocked() {
    // inferred=false means official regardless of confidence — HARD
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();

    List<PlanCourse> courses =
        List.of(
            new PlanCourse(c1, "C1", 3, "REQUIRED", 1), new PlanCourse(c2, "C2", 3, "REQUIRED", 2));

    // official (inferred=false) but MEDIUM confidence — still HARD because inferred=false
    List<CourseRequirementEdge> requirements =
        List.of(new CourseRequirementEdge(c2, c1, "COURSE_PASSED", "MEDIUM", false));

    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of();

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result.get(c2).status()).isEqualTo(CourseProgressStatus.BLOCKED);
    assertThat(result.get(c2).unmetRequirements().getFirst().advisory()).isFalse();
  }

  // -----------------------------------------------------------------------
  // Inferred=true + MEDIUM/LOW confidence → AVAILABLE + advisory
  // -----------------------------------------------------------------------

  @Test
  void course_with_unmet_inferred_medium_prereq_is_available_with_advisory() {
    UUID c1 = UUID.randomUUID();
    UUID c3 = UUID.randomUUID();

    List<PlanCourse> courses =
        List.of(
            new PlanCourse(c1, "C1", 3, "REQUIRED", 1), new PlanCourse(c3, "C3", 3, "REQUIRED", 2));

    // SOFT edge: inferred=true + MEDIUM confidence
    List<CourseRequirementEdge> requirements =
        List.of(new CourseRequirementEdge(c3, c1, "COURSE_PASSED", "MEDIUM", true));

    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of();

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result.get(c3).status()).isEqualTo(CourseProgressStatus.AVAILABLE);
    assertThat(result.get(c3).unmetRequirements())
        .hasSize(1)
        .first()
        .extracting(UnmetRequirement::advisory)
        .isEqualTo(true);
  }

  @Test
  void course_with_unmet_inferred_low_confidence_prereq_is_available_with_advisory() {
    UUID c1 = UUID.randomUUID();
    UUID c3 = UUID.randomUUID();

    List<PlanCourse> courses =
        List.of(
            new PlanCourse(c1, "C1", 3, "REQUIRED", 1), new PlanCourse(c3, "C3", 3, "REQUIRED", 2));

    List<CourseRequirementEdge> requirements =
        List.of(new CourseRequirementEdge(c3, c1, "COURSE_PASSED", "LOW", true));

    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of();

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result.get(c3).status()).isEqualTo(CourseProgressStatus.AVAILABLE);
    assertThat(result.get(c3).unmetRequirements().getFirst().advisory()).isTrue();
  }

  // -----------------------------------------------------------------------
  // Course with effective PASSED carries PASSED
  // -----------------------------------------------------------------------

  @Test
  void passed_course_carries_passed_status() {
    UUID c1 = UUID.randomUUID();

    List<PlanCourse> courses = List.of(new PlanCourse(c1, "C1", 3, "REQUIRED", 1));
    List<CourseRequirementEdge> requirements = List.of();
    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of(c1, CourseProgressStatus.PASSED);

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result.get(c1).status()).isEqualTo(CourseProgressStatus.PASSED);
    assertThat(result.get(c1).unmetRequirements()).isEmpty();
  }

  @Test
  void failed_course_carries_failed_status() {
    UUID c1 = UUID.randomUUID();

    List<PlanCourse> courses = List.of(new PlanCourse(c1, "C1", 3, "REQUIRED", 1));
    List<CourseRequirementEdge> requirements = List.of();
    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of(c1, CourseProgressStatus.FAILED);

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result.get(c1).status()).isEqualTo(CourseProgressStatus.FAILED);
  }

  @Test
  void cancelled_course_carries_cancelled_status() {
    UUID c1 = UUID.randomUUID();

    List<PlanCourse> courses = List.of(new PlanCourse(c1, "C1", 3, "REQUIRED", 1));
    List<CourseRequirementEdge> requirements = List.of();
    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of(c1, CourseProgressStatus.CANCELLED);

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result.get(c1).status()).isEqualTo(CourseProgressStatus.CANCELLED);
  }

  // -----------------------------------------------------------------------
  // Empty plan → empty result
  // -----------------------------------------------------------------------

  @Test
  void empty_plan_returns_empty_result() {
    Map<UUID, CourseAvailability> result = sut.compute(List.of(), List.of(), Map.of());
    assertThat(result).isEmpty();
  }

  // -----------------------------------------------------------------------
  // PROP-001 (REQ-8, design D1): CONFIGURABLE pool gate on Trabajo de Grado.
  // Pool members = ACTIVE DEEPENING electives supplied explicitly by the application layer
  // (poolMemberCourseIds) — the 4 legacy slot anchors are simply never part of that set.
  // Gate satisfied iff COUNT(DISTINCT passed pool-member courseId) >= edge.requiredCredits().
  // -----------------------------------------------------------------------

  @Test
  void thesisGate_blockedWithFewerThanRequiredDistinctPassedPoolMembers() {
    UUID tdg = UUID.randomUUID();
    UUID elective1 = UUID.randomUUID();
    UUID elective2 = UUID.randomUUID();
    UUID elective3 = UUID.randomUUID();
    UUID elective4 = UUID.randomUUID();

    List<PlanCourse> courses =
        List.of(new PlanCourse(tdg, "Trabajo de Grado", 6, "DEGREE_WORK", 10));

    // CONFIGURABLE pool gate: no single requiredCourseId, threshold = 4 distinct passed pool
    // members
    List<CourseRequirementEdge> requirements =
        List.of(new CourseRequirementEdge(tdg, null, "CONFIGURABLE", null, null, 4));

    // Only 3 of the 4 pool electives are PASSED — below threshold
    Map<UUID, CourseProgressStatus> effectiveByCourse =
        Map.of(
            elective1, CourseProgressStatus.PASSED,
            elective2, CourseProgressStatus.PASSED,
            elective3, CourseProgressStatus.PASSED);
    Set<UUID> poolMemberCourseIds = Set.of(elective1, elective2, elective3, elective4);

    Map<UUID, CourseAvailability> result =
        sut.compute(courses, requirements, effectiveByCourse, poolMemberCourseIds);

    assertThat(result.get(tdg).status()).isEqualTo(CourseProgressStatus.BLOCKED);
  }

  @Test
  void thesisGate_availableWithAtLeastRequiredDistinctPassedPoolMembers() {
    UUID tdg = UUID.randomUUID();
    UUID elective1 = UUID.randomUUID();
    UUID elective2 = UUID.randomUUID();
    UUID elective3 = UUID.randomUUID();
    UUID elective4 = UUID.randomUUID();

    List<PlanCourse> courses =
        List.of(new PlanCourse(tdg, "Trabajo de Grado", 6, "DEGREE_WORK", 10));

    List<CourseRequirementEdge> requirements =
        List.of(new CourseRequirementEdge(tdg, null, "CONFIGURABLE", null, null, 4));

    // All 4 pool electives PASSED — meets threshold exactly
    Map<UUID, CourseProgressStatus> effectiveByCourse =
        Map.of(
            elective1, CourseProgressStatus.PASSED,
            elective2, CourseProgressStatus.PASSED,
            elective3, CourseProgressStatus.PASSED,
            elective4, CourseProgressStatus.PASSED);
    Set<UUID> poolMemberCourseIds = Set.of(elective1, elective2, elective3, elective4);

    Map<UUID, CourseAvailability> result =
        sut.compute(courses, requirements, effectiveByCourse, poolMemberCourseIds);

    assertThat(result.get(tdg).status()).isEqualTo(CourseProgressStatus.AVAILABLE);
    assertThat(result.get(tdg).unmetRequirements()).isEmpty();
  }

  @Test
  void thesisGate_passedElectivesOutsidePoolSet_doNotCountTowardThreshold() {
    UUID tdg = UUID.randomUUID();
    UUID poolElective = UUID.randomUUID();
    // A slot-anchor-like course that is PASSED but NOT in the supplied pool set — must not count
    UUID nonPoolCourse = UUID.randomUUID();

    List<PlanCourse> courses =
        List.of(new PlanCourse(tdg, "Trabajo de Grado", 6, "DEGREE_WORK", 10));
    List<CourseRequirementEdge> requirements =
        List.of(new CourseRequirementEdge(tdg, null, "CONFIGURABLE", null, null, 4));

    Map<UUID, CourseProgressStatus> effectiveByCourse =
        Map.of(
            poolElective, CourseProgressStatus.PASSED,
            nonPoolCourse, CourseProgressStatus.PASSED);
    // Only poolElective is a real pool member; nonPoolCourse is excluded (e.g. a slot anchor)
    Set<UUID> poolMemberCourseIds = Set.of(poolElective);

    Map<UUID, CourseAvailability> result =
        sut.compute(courses, requirements, effectiveByCourse, poolMemberCourseIds);

    // 1 distinct passed pool member < 4 → still BLOCKED, even though 2 courses are PASSED overall
    assertThat(result.get(tdg).status()).isEqualTo(CourseProgressStatus.BLOCKED);
  }

  @Test
  void thesisGate_withoutPoolMemberIds_threeArgOverload_backwardCompatibleAndBlocked() {
    // The original 3-arg compute() must keep working for all existing (non-CONFIGURABLE) callers.
    // For a CONFIGURABLE gate specifically, no pool set means 0 distinct passed → always BLOCKED.
    UUID tdg = UUID.randomUUID();
    List<PlanCourse> courses =
        List.of(new PlanCourse(tdg, "Trabajo de Grado", 6, "DEGREE_WORK", 10));
    List<CourseRequirementEdge> requirements =
        List.of(new CourseRequirementEdge(tdg, null, "CONFIGURABLE", null, null, 4));

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, Map.of());

    assertThat(result.get(tdg).status()).isEqualTo(CourseProgressStatus.BLOCKED);
  }

  // -----------------------------------------------------------------------
  // Course with no prereqs and no attempts → AVAILABLE
  // -----------------------------------------------------------------------

  @Test
  void course_with_no_prereqs_and_no_attempts_is_available() {
    UUID c1 = UUID.randomUUID();

    List<PlanCourse> courses = List.of(new PlanCourse(c1, "C1", 3, "REQUIRED", 1));
    List<CourseRequirementEdge> requirements = List.of();
    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of();

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result.get(c1).status()).isEqualTo(CourseProgressStatus.AVAILABLE);
    assertThat(result.get(c1).unmetRequirements()).isEmpty();
  }

  // -----------------------------------------------------------------------
  // Mixed: one HARD unmet + one SOFT unmet → BLOCKED, both in unmetRequirements
  // -----------------------------------------------------------------------

  @Test
  void course_with_mixed_hard_and_soft_unmet_prereqs_is_blocked_with_both_in_list() {
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();
    UUID c3 = UUID.randomUUID();

    List<PlanCourse> courses =
        List.of(
            new PlanCourse(c1, "C1", 3, "REQUIRED", 1),
            new PlanCourse(c2, "C2", 3, "REQUIRED", 1),
            new PlanCourse(c3, "C3", 3, "REQUIRED", 2));

    List<CourseRequirementEdge> requirements =
        List.of(
            // HARD edge for c1
            new CourseRequirementEdge(c3, c1, "COURSE_PASSED", "HIGH", false),
            // SOFT edge for c2
            new CourseRequirementEdge(c3, c2, "COURSE_PASSED", "MEDIUM", true));

    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of(); // none passed

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result.get(c3).status()).isEqualTo(CourseProgressStatus.BLOCKED);
    assertThat(result.get(c3).unmetRequirements()).hasSize(2);
    // blocking entry is non-advisory
    assertThat(result.get(c3).unmetRequirements())
        .anyMatch(u -> u.requiredCourseId().equals(c1) && !u.advisory())
        .anyMatch(u -> u.requiredCourseId().equals(c2) && u.advisory());
  }
}
