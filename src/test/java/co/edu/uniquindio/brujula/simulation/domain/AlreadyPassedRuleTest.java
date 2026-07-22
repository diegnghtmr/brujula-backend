package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AlreadyPassedRule} (spec VAL-005). */
class AlreadyPassedRuleTest {

  private AlreadyPassedRule rule;
  private final UUID passedCourseId = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

  @BeforeEach
  void setUp() {
    rule = new AlreadyPassedRule();
  }

  @Test
  void blocksWhenCourseAlreadyPassed() {
    CandidateSelection candidate = buildCandidate(passedCourseId, "Cálculo I");
    PassedCourseSet passed = new PassedCourseSet(Set.of(passedCourseId));

    Optional<ValidationNotice> result = rule.evaluate(candidate, passed);

    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_COURSE_ALREADY_PASSED");
    assertThat(result.get().message()).contains("Cálculo I");
  }

  @Test
  void allowedWhenCourseNotPassed() {
    UUID differentCourse = UUID.randomUUID();
    CandidateSelection candidate = buildCandidate(differentCourse, "Física II");
    PassedCourseSet passed = new PassedCourseSet(Set.of(passedCourseId));

    Optional<ValidationNotice> result = rule.evaluate(candidate, passed);

    assertThat(result).isEmpty();
  }

  @Test
  void allowedWhenPassedSetEmpty() {
    CandidateSelection candidate = buildCandidate(passedCourseId, "Álgebra");
    Optional<ValidationNotice> result = rule.evaluate(candidate, PassedCourseSet.empty());
    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // PROP-001 (REQ-5, design D4): profundización electives are distinct real courses (V028 pool
  // ids). Confirms this rule needs NO change: already-passed detection is courseId-based.
  // -------------------------------------------------------------------------

  private static final UUID ELECTIVE_DISENO_CENTRADO =
      UUID.fromString("c0dee000-0000-4000-a000-000000000001");
  private static final UUID ELECTIVE_COMPUTACION_NUBE =
      UUID.fromString("c0dee000-0000-4000-a000-000000000004");

  @Test
  void passedOneElective_selectingDistinctElective_allowed() {
    // Student passed elective A (Diseño Centrado en el Usuario)
    PassedCourseSet passed = new PassedCourseSet(Set.of(ELECTIVE_DISENO_CENTRADO));

    // Candidate is a DIFFERENT real elective (Computación en la Nube) — must be allowed
    CandidateSelection candidate =
        buildCandidate(ELECTIVE_COMPUTACION_NUBE, "Computación en la Nube");

    Optional<ValidationNotice> result = rule.evaluate(candidate, passed);

    assertThat(result).isEmpty();
  }

  @Test
  void passedOneElective_reselectingSameElective_blocked() {
    // Student passed elective A (Diseño Centrado en el Usuario)
    PassedCourseSet passed = new PassedCourseSet(Set.of(ELECTIVE_DISENO_CENTRADO));

    // Candidate resolves to the SAME real elective already passed — must be blocked
    CandidateSelection candidate =
        buildCandidate(ELECTIVE_DISENO_CENTRADO, "Diseño Centrado en el Usuario");

    Optional<ValidationNotice> result = rule.evaluate(candidate, passed);

    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_COURSE_ALREADY_PASSED");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CandidateSelection buildCandidate(UUID courseId, String courseName) {
    return new CandidateSelection(
        UUID.randomUUID(),
        UUID.randomUUID(),
        courseId,
        courseName,
        UUID.randomUUID(),
        3.0,
        "DIURNA",
        "FINAL",
        1L,
        List.of());
  }
}
