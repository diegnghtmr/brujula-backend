package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * RED→GREEN unit tests for {@link DuplicateSelectionRule} (spec VAL-006).
 *
 * <p>Each test genuinely fires the rule path under test:
 * <ul>
 *   <li>Same courseId present → blocked.
 *   <li>Different courseId → allowed.
 *   <li>Same courseId via different scheduleOptionId → still blocked.
 *   <li>Empty existing list → allowed.
 * </ul>
 */
class DuplicateSelectionRuleTest {

  private DuplicateSelectionRule rule;

  private final UUID programId = UUID.randomUUID();
  private final UUID courseId = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

  @BeforeEach
  void setUp() {
    rule = new DuplicateSelectionRule();
  }

  // -------------------------------------------------------------------------
  // BLOCKING path — rule fires
  // -------------------------------------------------------------------------

  @Test
  void sameCourseId_present_returnsBlockingNotice() {
    // ARRANGE: existing selection already has courseId
    ExistingSelection existing = new ExistingSelection(
        UUID.randomUUID(), courseId, "Cálculo Diferencial", programId, 4.0, List.of());

    CandidateSelection candidate = buildCandidate(courseId);

    // ACT
    Optional<ValidationNotice> result = rule.evaluate(candidate, List.of(existing));

    // ASSERT: rule fires → BLOCKING notice
    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_DUPLICATE_SELECTION");
    assertThat(result.get().message()).contains("Cálculo Diferencial");
  }

  @Test
  void sameCourseId_viaDifferentScheduleOption_returnsBlockingNotice() {
    // Different selectionId (different scheduleOption variant), same courseId
    ExistingSelection existing = new ExistingSelection(
        UUID.randomUUID(), courseId, "Cálculo Diferencial", programId, 4.0, List.of());

    // Candidate has a different scheduleOptionId but same courseId
    CandidateSelection candidate = new CandidateSelection(
        UUID.randomUUID(), // different scheduleOptionId
        UUID.randomUUID(), courseId, "Cálculo Diferencial",
        programId, 4.0, "NOCTURNA", "FINAL", 1L, List.of());

    Optional<ValidationNotice> result = rule.evaluate(candidate, List.of(existing));

    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_DUPLICATE_SELECTION");
  }

  // -------------------------------------------------------------------------
  // ALLOWED path — rule does NOT fire
  // -------------------------------------------------------------------------

  @Test
  void differentCourseId_returnsEmpty() {
    UUID differentCourseId = UUID.randomUUID();
    ExistingSelection existing = new ExistingSelection(
        UUID.randomUUID(), differentCourseId, "Física I", programId, 3.0, List.of());

    CandidateSelection candidate = buildCandidate(courseId);

    Optional<ValidationNotice> result = rule.evaluate(candidate, List.of(existing));

    assertThat(result).isEmpty();
  }

  @Test
  void noExistingSelections_returnsEmpty() {
    CandidateSelection candidate = buildCandidate(courseId);

    Optional<ValidationNotice> result = rule.evaluate(candidate, List.of());

    assertThat(result).isEmpty();
  }

  @Test
  void multipleDifferentCourses_noneMatch_returnsEmpty() {
    ExistingSelection e1 = new ExistingSelection(UUID.randomUUID(), UUID.randomUUID(), "Física I", programId, 3.0, List.of());
    ExistingSelection e2 = new ExistingSelection(UUID.randomUUID(), UUID.randomUUID(), "Física II", programId, 3.0, List.of());

    CandidateSelection candidate = buildCandidate(courseId);

    Optional<ValidationNotice> result = rule.evaluate(candidate, List.of(e1, e2));

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CandidateSelection buildCandidate(UUID cId) {
    SimScheduleSession session = new SimScheduleSession(
        "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0));
    return new CandidateSelection(
        UUID.randomUUID(), UUID.randomUUID(), cId,
        "Cálculo Diferencial", programId, 4.0, "DIURNA", "FINAL", 1L, List.of(session));
  }
}
