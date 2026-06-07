package co.edu.uniquindio.brujula.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * WU4-T01 RED: unit tests for {@link CourseAvailabilityService}.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>AVL-1-HP: all hard prereqs met → AVAILABLE.
 *   <li>AVL-1-BLOCKED: official/HIGH prereq unmet → BLOCKED with reason.
 *   <li>AVL-1-SOFT-PREREQ: inferred MEDIUM prereq unmet → AVAILABLE + advisory in unmet list.
 *   <li>AVL-1-PASSED-COURSE: course with effective PASSED → PASSED status.
 *   <li>empty plan → empty result.
 * </ul>
 */
class CourseAvailabilityServiceTest {

  private final CourseAvailabilityService sut = new CourseAvailabilityService();

  // -----------------------------------------------------------------------
  // AVL-1-HP: all hard prereqs met → AVAILABLE
  // -----------------------------------------------------------------------

  @Test
  void course_with_all_hard_prereqs_passed_is_available() {
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();

    List<PlanCourse> courses = List.of(
        new PlanCourse(c1, "Course 1", 3, "REQUIRED", 1),
        new PlanCourse(c2, "Course 2", 3, "REQUIRED", 2));

    // c2 requires c1 with HIGH confidence (HARD edge)
    List<CourseRequirementEdge> requirements = List.of(
        new CourseRequirementEdge(c2, c1, "COURSE_PASSED", "HIGH", false));

    // c1 is PASSED → c2 should be AVAILABLE
    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of(c1, CourseProgressStatus.PASSED);

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result).containsKey(c2);
    assertThat(result.get(c2).status()).isEqualTo(CourseProgressStatus.AVAILABLE);
    assertThat(result.get(c2).unmetRequirements()).isEmpty();
  }

  // -----------------------------------------------------------------------
  // AVL-1-BLOCKED: official (inferred=false) or HIGH confidence prereq unmet → BLOCKED
  // -----------------------------------------------------------------------

  @Test
  void course_with_unmet_official_prereq_is_blocked() {
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();

    List<PlanCourse> courses = List.of(
        new PlanCourse(c1, "Course 1", 3, "REQUIRED", 1),
        new PlanCourse(c2, "Course 2", 3, "REQUIRED", 2));

    // c2 requires c1 with HIGH confidence (HARD edge) — c1 NOT passed
    List<CourseRequirementEdge> requirements = List.of(
        new CourseRequirementEdge(c2, c1, "COURSE_PASSED", "HIGH", false));

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

    List<PlanCourse> courses = List.of(
        new PlanCourse(c1, "C1", 3, "REQUIRED", 1),
        new PlanCourse(c2, "C2", 3, "REQUIRED", 2));

    // official (inferred=false) but MEDIUM confidence — still HARD because inferred=false
    List<CourseRequirementEdge> requirements = List.of(
        new CourseRequirementEdge(c2, c1, "COURSE_PASSED", "MEDIUM", false));

    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of();

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result.get(c2).status()).isEqualTo(CourseProgressStatus.BLOCKED);
    assertThat(result.get(c2).unmetRequirements().getFirst().advisory()).isFalse();
  }

  // -----------------------------------------------------------------------
  // AVL-1-SOFT-PREREQ: inferred=true + MEDIUM/LOW confidence → AVAILABLE + advisory
  // -----------------------------------------------------------------------

  @Test
  void course_with_unmet_inferred_medium_prereq_is_available_with_advisory() {
    UUID c1 = UUID.randomUUID();
    UUID c3 = UUID.randomUUID();

    List<PlanCourse> courses = List.of(
        new PlanCourse(c1, "C1", 3, "REQUIRED", 1),
        new PlanCourse(c3, "C3", 3, "REQUIRED", 2));

    // SOFT edge: inferred=true + MEDIUM confidence
    List<CourseRequirementEdge> requirements = List.of(
        new CourseRequirementEdge(c3, c1, "COURSE_PASSED", "MEDIUM", true));

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

    List<PlanCourse> courses = List.of(
        new PlanCourse(c1, "C1", 3, "REQUIRED", 1),
        new PlanCourse(c3, "C3", 3, "REQUIRED", 2));

    List<CourseRequirementEdge> requirements = List.of(
        new CourseRequirementEdge(c3, c1, "COURSE_PASSED", "LOW", true));

    Map<UUID, CourseProgressStatus> effectiveByCourse = Map.of();

    Map<UUID, CourseAvailability> result = sut.compute(courses, requirements, effectiveByCourse);

    assertThat(result.get(c3).status()).isEqualTo(CourseProgressStatus.AVAILABLE);
    assertThat(result.get(c3).unmetRequirements().getFirst().advisory()).isTrue();
  }

  // -----------------------------------------------------------------------
  // AVL-1-PASSED-COURSE: course with effective PASSED carries PASSED
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

    List<PlanCourse> courses = List.of(
        new PlanCourse(c1, "C1", 3, "REQUIRED", 1),
        new PlanCourse(c2, "C2", 3, "REQUIRED", 1),
        new PlanCourse(c3, "C3", 3, "REQUIRED", 2));

    List<CourseRequirementEdge> requirements = List.of(
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
