package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * RED→GREEN unit tests for {@link MinCreditsRule} (SIMULATION_MIN_CREDITS_NOT_MET).
 *
 * <p>Data-gap note: As of WU4b the seed data (V012/V013) has NO MINIMUM_CREDITS edges for
 * individual courses. This rule is wired and functionally correct but fires zero violations
 * against seed data. Tests below use synthetic MINIMUM_CREDITS requirement edges to prove
 * genuine rule firing. When real seed data is added the rule fires without code changes.
 *
 * <p>Firing evidence:
 * <ul>
 *   <li>MINIMUM_CREDITS edge present, student credits below threshold → BLOCKED (rule fires).
 *   <li>MINIMUM_CREDITS edge present, student credits meets threshold exactly → allowed.
 *   <li>No MINIMUM_CREDITS edges in requirements → allowed (rule not triggered).
 *   <li>COURSE_PASSED edges only → not evaluated by this rule → allowed.
 * </ul>
 */
class MinCreditsRuleTest {

  private MinCreditsRule rule;

  private final UUID courseId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    rule = new MinCreditsRule();
  }

  // -------------------------------------------------------------------------
  // BLOCKING path — rule fires (synthetic edge)
  // -------------------------------------------------------------------------

  @Test
  void minimumCreditsEdge_studentCreditsBelowThreshold_returnsBlockingNotice() {
    // ARRANGE: synthetic MINIMUM_CREDITS edge requires 60 accumulated credits; student has only 40
    PrereqRequirement minCreditsReq = new PrereqRequirement(
        courseId,
        null,          // no requiredCourseId for MINIMUM_CREDITS
        "MINIMUM_CREDITS",
        null,          // confidence not applicable
        null,          // inferred not applicable
        null,          // requiredCourseName not applicable
        60);           // minimumCredits = 60

    CandidateSelection candidate = buildCandidate();
    int studentCredits = 40; // below threshold

    // ACT
    Optional<ValidationNotice> result = rule.evaluate(candidate, List.of(minCreditsReq), studentCredits);

    // ASSERT: rule fires → BLOCKING notice
    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_MIN_CREDITS_NOT_MET");
    assertThat(result.get().message()).contains("60");
    assertThat(result.get().message()).contains("40");
  }

  // -------------------------------------------------------------------------
  // ALLOWED paths — rule does NOT fire
  // -------------------------------------------------------------------------

  @Test
  void minimumCreditsEdge_studentCreditsExactlyAtThreshold_returnsEmpty() {
    // ARRANGE: exactly at threshold → allowed (spec: minimum means ≥)
    PrereqRequirement minCreditsReq = new PrereqRequirement(
        courseId, null, "MINIMUM_CREDITS", null, null, null, 60);

    CandidateSelection candidate = buildCandidate();
    int studentCredits = 60; // exactly at threshold

    Optional<ValidationNotice> result = rule.evaluate(candidate, List.of(minCreditsReq), studentCredits);

    assertThat(result).isEmpty();
  }

  @Test
  void minimumCreditsEdge_studentCreditsAboveThreshold_returnsEmpty() {
    PrereqRequirement minCreditsReq = new PrereqRequirement(
        courseId, null, "MINIMUM_CREDITS", null, null, null, 40);

    CandidateSelection candidate = buildCandidate();

    Optional<ValidationNotice> result = rule.evaluate(candidate, List.of(minCreditsReq), 60);

    assertThat(result).isEmpty();
  }

  @Test
  void noRequirements_returnsEmpty() {
    // No requirements at all — rule not triggered
    CandidateSelection candidate = buildCandidate();

    Optional<ValidationNotice> result = rule.evaluate(candidate, List.of(), 0);

    assertThat(result).isEmpty();
  }

  @Test
  void onlyCoursePassed_edges_notEvaluated_returnsEmpty() {
    // COURSE_PASSED edges are not evaluated by MinCreditsRule
    UUID prereqId = UUID.randomUUID();
    PrereqRequirement coursePassedReq = new PrereqRequirement(
        courseId, prereqId, "COURSE_PASSED", "HIGH", false, "Some Course");

    CandidateSelection candidate = buildCandidate();

    Optional<ValidationNotice> result = rule.evaluate(candidate, List.of(coursePassedReq), 0);

    assertThat(result).isEmpty();
  }

  @Test
  void minimumCreditsEdge_nullMinimumCredits_ignored_returnsEmpty() {
    // Edge has null minimumCredits — treated as no constraint
    PrereqRequirement minCreditsReq = new PrereqRequirement(
        courseId, null, "MINIMUM_CREDITS", null, null, null, null);

    CandidateSelection candidate = buildCandidate();

    Optional<ValidationNotice> result = rule.evaluate(candidate, List.of(minCreditsReq), 0);

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CandidateSelection buildCandidate() {
    SimScheduleSession session = new SimScheduleSession(
        "MONDAY", LocalTime.of(14, 0), LocalTime.of(16, 0));
    return new CandidateSelection(
        UUID.randomUUID(), UUID.randomUUID(), courseId,
        "Advanced Elective Course", programId, 3.0, "DIURNA", "FINAL", 1L, List.of(session));
  }
}
