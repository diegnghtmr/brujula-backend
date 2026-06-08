package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PrerequisiteEvaluator}.
 *
 * <p>Tests the Fase-3 hard-edge policy:
 * HARD = official (inferred=null/false) OR confidence=HIGH → blocks.
 * SOFT = inferred=true AND confidence in {MEDIUM, LOW, null} → NO notice in WU4a (TODO WU4b).
 */
class PrerequisiteEvaluatorTest {

  private PrerequisiteEvaluator evaluator;
  private final UUID courseId = UUID.randomUUID();
  private final UUID prereqId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    evaluator = new PrerequisiteEvaluator();
  }

  @Test
  void officialUnmetPrereqBlocks() {
    // inferred=false (official), HIGH confidence — unmet → BLOCKING
    PrereqRequirement req = new PrereqRequirement(
        courseId, prereqId, "COURSE_PASSED", "HIGH", false, "Cálculo I");
    CandidateSelection candidate = buildCandidate(courseId);
    PassedCourseSet passed = PassedCourseSet.empty();

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, List.of(req), passed);

    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_PREREQUISITE_NOT_MET");
    assertThat(result.get().metadata()).containsEntry("requiredCourseName", "Cálculo I");
  }

  @Test
  void highConfidenceInferredUnmetBlocks() {
    // inferred=true BUT confidence=HIGH → still HARD → BLOCKING
    PrereqRequirement req = new PrereqRequirement(
        courseId, prereqId, "COURSE_PASSED", "HIGH", true, "Álgebra Lineal");
    CandidateSelection candidate = buildCandidate(courseId);
    PassedCourseSet passed = PassedCourseSet.empty();

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, List.of(req), passed);

    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_PREREQUISITE_NOT_MET");
  }

  @Test
  void officialNullConfidenceUnmetBlocks() {
    // inferred=null (official) — should be treated as official (HARD) and block
    PrereqRequirement req = new PrereqRequirement(
        courseId, prereqId, "COURSE_PASSED", null, null, "Física I");
    CandidateSelection candidate = buildCandidate(courseId);
    PassedCourseSet passed = PassedCourseSet.empty();

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, List.of(req), passed);

    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_PREREQUISITE_NOT_MET");
  }

  @Test
  void studentPassedPrereqAllows() {
    PrereqRequirement req = new PrereqRequirement(
        courseId, prereqId, "COURSE_PASSED", "HIGH", false, "Cálculo I");
    CandidateSelection candidate = buildCandidate(courseId);
    PassedCourseSet passed = new PassedCourseSet(Set.of(prereqId)); // student passed it

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, List.of(req), passed);

    assertThat(result).isEmpty();
  }

  @Test
  void softInferredMediumConfidenceUnmetProducesNoNoticeInWU4a() {
    // inferred=true AND confidence=MEDIUM → SOFT edge → NO notice in WU4a (TODO WU4b)
    PrereqRequirement req = new PrereqRequirement(
        courseId, prereqId, "COURSE_PASSED", "MEDIUM", true, "Estadística");
    CandidateSelection candidate = buildCandidate(courseId);
    PassedCourseSet passed = PassedCourseSet.empty(); // not passed

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, List.of(req), passed);

    // WU4a: soft prereqs produce NO notice (not a block, not a warning yet)
    assertThat(result).isEmpty();
  }

  @Test
  void softInferredLowConfidenceUnmetProducesNoNoticeInWU4a() {
    // inferred=true AND confidence=LOW → SOFT → no notice in WU4a
    PrereqRequirement req = new PrereqRequirement(
        courseId, prereqId, "COURSE_PASSED", "LOW", true, "Química");
    CandidateSelection candidate = buildCandidate(courseId);
    PassedCourseSet passed = PassedCourseSet.empty();

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, List.of(req), passed);

    assertThat(result).isEmpty();
  }

  @Test
  void noRequirementsAllows() {
    CandidateSelection candidate = buildCandidate(courseId);
    Optional<ValidationNotice> result =
        evaluator.evaluate(candidate, List.of(), PassedCourseSet.empty());
    assertThat(result).isEmpty();
  }

  @Test
  void nonCoursePassedTypeSkipped() {
    // MINIMUM_CREDITS requirement — not evaluated in WU4a
    PrereqRequirement req = new PrereqRequirement(
        courseId, prereqId, "MINIMUM_CREDITS", "HIGH", false, "Something");
    CandidateSelection candidate = buildCandidate(courseId);
    PassedCourseSet passed = PassedCourseSet.empty();

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, List.of(req), passed);

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // evaluateAll() — returns ALL blocking notices (used in WU5 review re-validation)
  // -------------------------------------------------------------------------

  @Test
  void evaluateAll_oneUnmetHardPrereq_returnsOneNotice() {
    UUID prereq2 = UUID.randomUUID();
    PrereqRequirement req1 = new PrereqRequirement(
        courseId, prereqId, "COURSE_PASSED", "HIGH", false, "Cálculo I");
    PrereqRequirement req2 = new PrereqRequirement(
        courseId, prereq2, "COURSE_PASSED", null, null, "Álgebra");
    CandidateSelection candidate = buildCandidate(courseId);
    // Student passed prereq2 but NOT prereqId
    PassedCourseSet passed = new PassedCourseSet(Set.of(prereq2));

    List<ValidationNotice> notices =
        evaluator.evaluateAll(candidate, List.of(req1, req2), passed);

    assertThat(notices).hasSize(1);
    assertThat(notices.get(0).code()).isEqualTo("SIMULATION_PREREQUISITE_NOT_MET");
    assertThat(notices.get(0).metadata()).containsEntry("requiredCourseName", "Cálculo I");
  }

  @Test
  void evaluateAll_multipleUnmetHardPrereqs_returnsAllNotices() {
    UUID prereq2 = UUID.randomUUID();
    PrereqRequirement req1 = new PrereqRequirement(
        courseId, prereqId, "COURSE_PASSED", "HIGH", false, "Cálculo I");
    PrereqRequirement req2 = new PrereqRequirement(
        courseId, prereq2, "COURSE_PASSED", null, null, "Álgebra");
    CandidateSelection candidate = buildCandidate(courseId);
    PassedCourseSet passed = PassedCourseSet.empty(); // neither passed

    List<ValidationNotice> notices =
        evaluator.evaluateAll(candidate, List.of(req1, req2), passed);

    assertThat(notices).hasSize(2);
    assertThat(notices).allSatisfy(n ->
        assertThat(n.code()).isEqualTo("SIMULATION_PREREQUISITE_NOT_MET"));
  }

  @Test
  void evaluateAll_allPrereqsMet_returnsEmptyList() {
    UUID prereq2 = UUID.randomUUID();
    PrereqRequirement req1 = new PrereqRequirement(
        courseId, prereqId, "COURSE_PASSED", "HIGH", false, "Cálculo I");
    PrereqRequirement req2 = new PrereqRequirement(
        courseId, prereq2, "COURSE_PASSED", null, null, "Álgebra");
    CandidateSelection candidate = buildCandidate(courseId);
    PassedCourseSet passed = new PassedCourseSet(Set.of(prereqId, prereq2));

    List<ValidationNotice> notices =
        evaluator.evaluateAll(candidate, List.of(req1, req2), passed);

    assertThat(notices).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CandidateSelection buildCandidate(UUID courseId) {
    return new CandidateSelection(UUID.randomUUID(), UUID.randomUUID(), courseId,
        "Test Course", UUID.randomUUID(), 3.0, "DIURNA", "FINAL", 1L, List.of());
  }
}
