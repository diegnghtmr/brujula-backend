package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * RED→GREEN unit tests for {@link SequentialTrackEvaluator} (spec VAL-008).
 *
 * <p>Firing evidence:
 * <ul>
 *   <li>DEEPENING course with unmet gatekeeper → BLOCKED (rule genuinely fires).
 *   <li>PERSONAL_ELECTIVE course with unmet gatekeeper → BLOCKED.
 *   <li>DEEPENING course with gatekeeper passed → allowed.
 *   <li>DEEPENING course with no gatekeeper defined → allowed (track not enforced).
 *   <li>REQUIRED course → rule is not evaluated → always allowed.
 * </ul>
 */
class SequentialTrackEvaluatorTest {

  private SequentialTrackEvaluator evaluator;

  private final UUID courseId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID gatekeeperCourseId = UUID.fromString("c0050000-0000-4000-a000-000000000005");

  @BeforeEach
  void setUp() {
    evaluator = new SequentialTrackEvaluator();
  }

  // -------------------------------------------------------------------------
  // BLOCKING paths — rule fires
  // -------------------------------------------------------------------------

  @Test
  void deepeningCourse_gatekeeperNotPassed_returnsBlockingNotice() {
    // ARRANGE: DEEPENING course, gatekeeper NOT passed
    SequentialPredecessor predecessor = new SequentialPredecessor(
        courseId, "DEEPENING", gatekeeperCourseId, "Ingeniería de Software I");
    CandidateSelection candidate = buildCandidate(courseId, "FINAL");
    PassedCourseSet passed = PassedCourseSet.empty(); // gatekeeper NOT passed

    // ACT
    Optional<ValidationNotice> result = evaluator.evaluate(candidate, predecessor, passed);

    // ASSERT: rule fires → BLOCKING notice
    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_SEQUENTIAL_ORDER");
    assertThat(result.get().message()).contains("Ingeniería de Software I");
  }

  @Test
  void personalElectiveCourse_gatekeeperNotPassed_returnsBlockingNotice() {
    // ARRANGE: PERSONAL_ELECTIVE course, gatekeeper NOT passed
    UUID electiveGatekeeperId = UUID.randomUUID();
    SequentialPredecessor predecessor = new SequentialPredecessor(
        courseId, "PERSONAL_ELECTIVE", electiveGatekeeperId, "Electiva Prereq I");
    CandidateSelection candidate = buildCandidate(courseId, "FINAL");
    PassedCourseSet passed = PassedCourseSet.empty();

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, predecessor, passed);

    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_SEQUENTIAL_ORDER");
    assertThat(result.get().message()).contains("Electiva Prereq I");
  }

  // -------------------------------------------------------------------------
  // ALLOWED paths — rule does NOT fire
  // -------------------------------------------------------------------------

  @Test
  void deepeningCourse_gatekeeperPassed_returnsEmpty() {
    // ARRANGE: DEEPENING course, gatekeeper HAS been passed
    SequentialPredecessor predecessor = new SequentialPredecessor(
        courseId, "DEEPENING", gatekeeperCourseId, "Ingeniería de Software I");
    CandidateSelection candidate = buildCandidate(courseId, "FINAL");
    PassedCourseSet passed = new PassedCourseSet(Set.of(gatekeeperCourseId)); // gatekeeper PASSED

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, predecessor, passed);

    assertThat(result).isEmpty();
  }

  @Test
  void deepeningCourse_noGatekeeperDefined_returnsEmpty() {
    // ARRANGE: DEEPENING course but track has no gatekeeper (predecessorCourseId = null)
    SequentialPredecessor predecessor = new SequentialPredecessor(
        courseId, "DEEPENING", null, null);
    CandidateSelection candidate = buildCandidate(courseId, "FINAL");
    PassedCourseSet passed = PassedCourseSet.empty();

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, predecessor, passed);

    assertThat(result).isEmpty();
  }

  @Test
  void requiredCourse_notEvaluated_returnsEmpty() {
    // ARRANGE: REQUIRED course — sequential rule does not apply
    SequentialPredecessor predecessor = new SequentialPredecessor(
        courseId, "REQUIRED", gatekeeperCourseId, "Some Prereq");
    CandidateSelection candidate = buildCandidate(courseId, "FINAL");
    PassedCourseSet passed = PassedCourseSet.empty(); // gatekeeper not passed, but irrelevant

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, predecessor, passed);

    // REQUIRED course type → rule does not evaluate → allowed
    assertThat(result).isEmpty();
  }

  @Test
  void institutionalCourse_notEvaluated_returnsEmpty() {
    SequentialPredecessor predecessor = new SequentialPredecessor(
        courseId, "INSTITUTIONAL", gatekeeperCourseId, "Some Course");
    CandidateSelection candidate = buildCandidate(courseId, "FINAL");
    PassedCourseSet passed = PassedCourseSet.empty();

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, predecessor, passed);

    assertThat(result).isEmpty();
  }

  @Test
  void deepeningCourse_noNameForGatekeeper_usesUuidInMessage() {
    // Predecessor name null → UUID shown in message
    SequentialPredecessor predecessor = new SequentialPredecessor(
        courseId, "DEEPENING", gatekeeperCourseId, null);
    CandidateSelection candidate = buildCandidate(courseId, "FINAL");
    PassedCourseSet passed = PassedCourseSet.empty();

    Optional<ValidationNotice> result = evaluator.evaluate(candidate, predecessor, passed);

    assertThat(result).isPresent();
    assertThat(result.get().message()).contains(gatekeeperCourseId.toString());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CandidateSelection buildCandidate(UUID cId, String offeringStatus) {
    SimScheduleSession session = new SimScheduleSession(
        "TUESDAY", LocalTime.of(14, 0), LocalTime.of(16, 0));
    return new CandidateSelection(
        UUID.randomUUID(), UUID.randomUUID(), cId,
        "Profundización en Ingeniería de Software I", programId,
        3.0, "DIURNA", offeringStatus, 1L, List.of(session));
  }
}
