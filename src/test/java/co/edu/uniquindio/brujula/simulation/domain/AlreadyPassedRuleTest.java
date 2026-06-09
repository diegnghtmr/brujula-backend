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
