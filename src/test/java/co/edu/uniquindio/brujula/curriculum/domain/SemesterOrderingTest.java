package co.edu.uniquindio.brujula.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU1-3.3 RED: Semester ordering invariant tests.
 *
 * <p>CUR-004 scenarios:
 * <ul>
 *   <li>Forward dependency (prereq in later semester) → SemesterOrderingViolationException.
 *   <li>Backward (prereq in earlier semester) → accepted.
 *   <li>Same semester → accepted.
 *   <li>Cross-journey comparison NOT enforced.
 * </ul>
 */
class SemesterOrderingTest {

  private PrerequisiteAcyclicityService service;
  private UUID planId;
  private UUID programId;

  @BeforeEach
  void setUp() {
    service = new PrerequisiteAcyclicityService();
    planId = UUID.randomUUID();
    programId = UUID.randomUUID();
  }

  @Test
  void backwardRequirementIsAccepted() {
    // Course A in semester 3, Course B in semester 5 (same journey DIURNA)
    // B requires A (A prereq for B): A semester 3 ≤ B semester 5 → valid
    CourseId idA = CourseId.generate();
    CourseId idB = CourseId.generate();

    Course courseA = Course.create(idA, planId, programId, "Programación I", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("DIURNA", 3)));
    Course courseB = Course.create(idB, planId, programId, "Programación II", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("DIURNA", 5)));

    CourseRequirement req = CourseRequirement.coursePassed(
        CourseRequirementId.generate(), idB.value(), idA.value(), false, Confidence.HIGH, false);

    assertThatCode(() -> service.validateSemesterOrdering(List.of(req), List.of(courseA, courseB)))
        .doesNotThrowAnyException();
  }

  @Test
  void sameSemesterRequirementIsAccepted() {
    // Both in semester 4 → edge is same-semester (co-requisite) → valid
    CourseId idA = CourseId.generate();
    CourseId idB = CourseId.generate();

    Course courseA = Course.create(idA, planId, programId, "Álgebra Lineal", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("DIURNA", 4)));
    Course courseB = Course.create(idB, planId, programId, "Cálculo III", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("DIURNA", 4)));

    CourseRequirement req = CourseRequirement.sequence(
        CourseRequirementId.generate(), idB.value(), idA.value(), true);

    assertThatCode(() -> service.validateSemesterOrdering(List.of(req), List.of(courseA, courseB)))
        .doesNotThrowAnyException();
  }

  @Test
  void forwardRequirementIsRejected() {
    // Course A in semester 5, Course B in semester 7 (same journey DIURNA)
    // B requires A: A is in semester 5, B is in semester 7 → A should be prereq, valid
    // BUT: Course C in semester 7, Course D in semester 5 — D requires C (forward!)
    // D is in semester 5, C is in semester 7: C (sem 7) is prereq for D (sem 5) → IMPOSSIBLE
    CourseId idC = CourseId.generate();
    CourseId idD = CourseId.generate();

    Course courseC = Course.create(idC, planId, programId, "Ingeniería Económica", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("DIURNA", 7)));
    Course courseD = Course.create(idD, planId, programId, "Formulación de Proyectos", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("DIURNA", 5)));

    // D (sem 5) requires C (sem 7): C is in semester 7 > D's semester 5 → forward dependency!
    CourseRequirement req = CourseRequirement.coursePassed(
        CourseRequirementId.generate(), idD.value(), idC.value(), false, Confidence.MEDIUM, true);

    assertThatThrownBy(() -> service.validateSemesterOrdering(List.of(req), List.of(courseC, courseD)))
        .isInstanceOf(SemesterOrderingViolationException.class);
  }

  @Test
  void crossJourneyComparisonIsNotEnforced() {
    // Course A: DIURNA semester 3. Course B: NOCTURNA semester 2.
    // B requires A, but they are in different journeys → no ordering check across journeys
    CourseId idA = CourseId.generate();
    CourseId idB = CourseId.generate();

    Course courseA = Course.create(idA, planId, programId, "Matemáticas I", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("DIURNA", 3)));
    Course courseB = Course.create(idB, planId, programId, "Física I", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("NOCTURNA", 2)));

    // B requires A, but different journeys → NOT enforced
    CourseRequirement req = CourseRequirement.coursePassed(
        CourseRequirementId.generate(), idB.value(), idA.value(), false, Confidence.LOW, true);

    assertThatCode(() -> service.validateSemesterOrdering(List.of(req), List.of(courseA, courseB)))
        .doesNotThrowAnyException();
  }

  @Test
  @org.junit.jupiter.api.DisplayName(
      "FIX-4 (WARNING-4): target course missing the required journey entry → silent-skip (no exception)")
  void targetCourseMissingRequiredJourneySilentlySkips() {
    // Required course A has DIURNA sem 7.
    // Target course B has ONLY NOCTURNA entry (NO DIURNA entry).
    // Per CUR-004: cross-journey / missing-journey comparisons are NOT enforced.
    // The check silently skips → NO SemesterOrderingViolationException is thrown.
    // This test LOCKS IN the silent-skip contract so a future change that accidentally throws fails.
    CourseId idA = CourseId.generate();
    CourseId idB = CourseId.generate();

    Course courseA = Course.create(idA, planId, programId, "Ingeniería Económica", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("DIURNA", 7)));
    Course courseB = Course.create(idB, planId, programId, "Formulación de Proyectos", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("NOCTURNA", 2))); // NO DIURNA entry

    // B requires A (A sem 7 DIURNA, B has no DIURNA entry) → silently skips, no violation
    CourseRequirement req = CourseRequirement.coursePassed(
        CourseRequirementId.generate(), idB.value(), idA.value(), false, Confidence.HIGH, false);

    assertThatCode(() -> service.validateSemesterOrdering(List.of(req), List.of(courseA, courseB)))
        .doesNotThrowAnyException();
  }

  @Test
  void mergedValidatePassesForValidGraph() {
    CourseId idA = CourseId.generate();
    CourseId idB = CourseId.generate();

    Course courseA = Course.create(idA, planId, programId, "Lógica", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("DIURNA", 1)));
    Course courseB = Course.create(idB, planId, programId, "Programación I", CourseType.REQUIRED, 3, 4,
        List.of(new RecommendedSemester("DIURNA", 2)));

    CourseRequirement req = CourseRequirement.coursePassed(
        CourseRequirementId.generate(), idB.value(), idA.value(), false, Confidence.HIGH, false);

    PrerequisiteGraph graph = new PrerequisiteGraph();
    graph.addEdge(idA.value(), idB.value());

    assertThatCode(() -> service.validate(graph, List.of(req), List.of(courseA, courseB)))
        .doesNotThrowAnyException();
  }
}
