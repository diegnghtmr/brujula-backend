package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SimulationValidationService} — orchestration and partitioning.
 *
 * <p>WU4a tests use the deprecated 5-param overload. WU4b tests use the 7-param overload to
 * exercise the full rule set.
 */
class SimulationValidationServiceTest {

  private SimulationValidationService service;
  private final UUID programId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private final UUID courseId = UUID.fromString("cccccccc-0000-0000-0000-000000000002");
  private final ProgramCreditLimit creditLimit = new ProgramCreditLimit(programId, "Sistemas", 23);
  // Null-predecessor: REQUIRED course type, no sequential gate
  private final SequentialPredecessor noPredecessor =
      new SequentialPredecessor(
          UUID.fromString("cccccccc-0000-0000-0000-000000000002"), "REQUIRED", null, null);

  @BeforeEach
  void setUp() {
    service = new SimulationValidationService();
  }

  @Test
  void allClean_returnsAllowed() {
    CandidateSelection candidate =
        buildCandidate(courseId, programId, 3.0, "WEDNESDAY", 18, 0, 20, 0);
    SimulationValidationResult result =
        service.validate(candidate, List.of(), List.of(), PassedCourseSet.empty(), creditLimit);

    assertThat(result.hasBlockers()).isFalse();
    assertThat(result.blocking()).isEmpty();
    assertThat(result.warnings()).isEmpty();
    assertThat(result.toOutcome()).isInstanceOf(ValidationOutcome.Allowed.class);
  }

  @Test
  void scheduleClash_returnsBlocked() {
    SimScheduleSession clashSession =
        new SimScheduleSession("WEDNESDAY", LocalTime.of(18, 0), LocalTime.of(20, 0));
    ExistingSelection existing =
        new ExistingSelection(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Cálculo I",
            programId,
            3.0,
            List.of(clashSession));

    CandidateSelection candidate =
        buildCandidate(courseId, programId, 3.0, "WEDNESDAY", 18, 0, 20, 0);

    SimulationValidationResult result =
        service.validate(
            candidate, List.of(existing), List.of(), PassedCourseSet.empty(), creditLimit);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.blocking()).anyMatch(n -> "SIMULATION_SCHEDULE_CLASH".equals(n.code()));
    assertThat(result.toOutcome()).isInstanceOf(ValidationOutcome.Blocked.class);
  }

  @Test
  void creditLimitExceeded_returnsBlocked() {
    ExistingSelection existing =
        new ExistingSelection(
            UUID.randomUUID(), UUID.randomUUID(), "Física I", programId, 21.0, List.of());

    // 21 + 3 = 24 > 23
    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);

    SimulationValidationResult result =
        service.validate(
            candidate, List.of(existing), List.of(), PassedCourseSet.empty(), creditLimit);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.blocking())
        .anyMatch(n -> "SIMULATION_CREDIT_LIMIT_EXCEEDED".equals(n.code()));
  }

  @Test
  void alreadyPassed_returnsBlocked() {
    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);
    PassedCourseSet passed = new PassedCourseSet(Set.of(courseId));

    SimulationValidationResult result =
        service.validate(candidate, List.of(), List.of(), passed, creditLimit);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.blocking())
        .anyMatch(n -> "SIMULATION_COURSE_ALREADY_PASSED".equals(n.code()));
  }

  @Test
  void prereqNotMet_returnsBlocked() {
    UUID prereqId = UUID.randomUUID();
    PrereqRequirement req =
        new PrereqRequirement(courseId, prereqId, "COURSE_PASSED", "HIGH", false, "Cálculo I");
    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);

    SimulationValidationResult result =
        service.validate(candidate, List.of(), List.of(req), PassedCourseSet.empty(), creditLimit);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.blocking()).anyMatch(n -> "SIMULATION_PREREQUISITE_NOT_MET".equals(n.code()));
  }

  @Test
  void blockingPartitionedCorrectly() {
    // Trigger two rules simultaneously: alreadyPassed + scheduleClash
    SimScheduleSession clashSession =
        new SimScheduleSession("WEDNESDAY", LocalTime.of(18, 0), LocalTime.of(20, 0));
    ExistingSelection existing =
        new ExistingSelection(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Física I",
            programId,
            3.0,
            List.of(clashSession));

    PassedCourseSet passed = new PassedCourseSet(Set.of(courseId));
    CandidateSelection candidate =
        buildCandidate(courseId, programId, 3.0, "WEDNESDAY", 18, 0, 20, 0);

    SimulationValidationResult result =
        service.validate(candidate, List.of(existing), List.of(), passed, creditLimit);

    assertThat(result.hasBlockers()).isTrue();
    // Both AlreadyPassed AND clash detected
    assertThat(result.blocking().stream().map(ValidationNotice::code).toList())
        .contains("SIMULATION_COURSE_ALREADY_PASSED", "SIMULATION_SCHEDULE_CLASH");
    assertThat(result.warnings()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // WU4b: Duplicate selection (SIMULATION_DUPLICATE_SELECTION)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("WU4b: same course already in simulation → SIMULATION_DUPLICATE_SELECTION blocked")
  void duplicateCourseInSimulation_returnsBlocked() {
    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);
    // Existing selection with the SAME courseId
    ExistingSelection duplicate =
        new ExistingSelection(UUID.randomUUID(), courseId, "New Course", programId, 3.0, List.of());

    SimulationValidationResult result =
        service.validate(
            candidate,
            List.of(duplicate),
            List.of(),
            PassedCourseSet.empty(),
            creditLimit,
            noPredecessor,
            0);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.blocking()).anyMatch(n -> "SIMULATION_DUPLICATE_SELECTION".equals(n.code()));
    assertThat(result.toOutcome()).isInstanceOf(ValidationOutcome.Blocked.class);
  }

  // -------------------------------------------------------------------------
  // WU4b: Archived offering (SIMULATION_ARCHIVED_OFFERING)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("WU4b: archived offering status → SIMULATION_ARCHIVED_OFFERING blocked")
  void archivedOffering_returnsBlocked() {
    // Build candidate with ARCHIVED offering status
    SimScheduleSession session =
        new SimScheduleSession("MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0));
    CandidateSelection candidate =
        new CandidateSelection(
            UUID.randomUUID(),
            UUID.randomUUID(),
            courseId,
            "New Course",
            programId,
            3.0,
            "DIURNA",
            "ARCHIVED",
            1L,
            List.of(session));

    SimulationValidationResult result =
        service.validate(
            candidate,
            List.of(),
            List.of(),
            PassedCourseSet.empty(),
            creditLimit,
            noPredecessor,
            0);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.blocking()).anyMatch(n -> "SIMULATION_ARCHIVED_OFFERING".equals(n.code()));
  }

  // -------------------------------------------------------------------------
  // WU4b: Sequential track ordering (SIMULATION_SEQUENTIAL_ORDER)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("WU4b: DEEPENING course with unmet gatekeeper → SIMULATION_SEQUENTIAL_ORDER blocked")
  void sequentialOrderNotMet_returnsBlocked() {
    UUID gatekeeperId = UUID.randomUUID();
    // DEEPENING course with a gatekeeper — student has NOT passed the gatekeeper
    SequentialPredecessor predecessor =
        new SequentialPredecessor(courseId, "DEEPENING", gatekeeperId, "Intro to Algorithms");

    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);
    PassedCourseSet passed = PassedCourseSet.empty(); // gatekeeper not passed

    SimulationValidationResult result =
        service.validate(candidate, List.of(), List.of(), passed, creditLimit, predecessor, 0);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.blocking()).anyMatch(n -> "SIMULATION_SEQUENTIAL_ORDER".equals(n.code()));
  }

  @Test
  @DisplayName("WU4b: DEEPENING course with gatekeeper passed → allowed")
  void sequentialOrderMet_allowed() {
    UUID gatekeeperId = UUID.randomUUID();
    SequentialPredecessor predecessor =
        new SequentialPredecessor(courseId, "DEEPENING", gatekeeperId, "Intro to Algorithms");

    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);
    PassedCourseSet passed = new PassedCourseSet(Set.of(gatekeeperId)); // gatekeeper passed

    SimulationValidationResult result =
        service.validate(candidate, List.of(), List.of(), passed, creditLimit, predecessor, 0);

    assertThat(result.hasBlockers()).isFalse();
  }

  // -------------------------------------------------------------------------
  // WU4b: Soft-prereq warning (SIMULATION_SOFT_PREREQ_WARNING)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("WU4b: unmet inferred MEDIUM prereq → warning only, selection allowed")
  void softPrereqWarning_allowedWithWarning() {
    UUID prereqId = UUID.randomUUID();
    // inferred=true, MEDIUM confidence → SOFT edge
    PrereqRequirement softReq =
        new PrereqRequirement(courseId, prereqId, "COURSE_PASSED", "MEDIUM", true, "Estadística");

    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);
    PassedCourseSet passed = PassedCourseSet.empty(); // soft prereq not passed

    SimulationValidationResult result =
        service.validate(
            candidate, List.of(), List.of(softReq), passed, creditLimit, noPredecessor, 0);

    // Not blocked — warnings only
    assertThat(result.hasBlockers()).isFalse();
    assertThat(result.warnings()).isNotEmpty();
    assertThat(result.warnings()).anyMatch(n -> "SIMULATION_SOFT_PREREQ_WARNING".equals(n.code()));
    assertThat(result.toOutcome()).isInstanceOf(ValidationOutcome.Allowed.class);
  }

  @Test
  @DisplayName("WU4b: unmet inferred LOW prereq → warning only, selection allowed")
  void softPrereqWarningLow_allowedWithWarning() {
    UUID prereqId = UUID.randomUUID();
    PrereqRequirement softReq =
        new PrereqRequirement(courseId, prereqId, "COURSE_PASSED", "LOW", true, "Química General");

    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);

    SimulationValidationResult result =
        service.validate(
            candidate,
            List.of(),
            List.of(softReq),
            PassedCourseSet.empty(),
            creditLimit,
            noPredecessor,
            0);

    assertThat(result.hasBlockers()).isFalse();
    assertThat(result.warnings()).anyMatch(n -> "SIMULATION_SOFT_PREREQ_WARNING".equals(n.code()));
  }

  @Test
  @DisplayName("WU4b: met soft prereq → no warning emitted")
  void softPrereqMet_noWarning() {
    UUID prereqId = UUID.randomUUID();
    PrereqRequirement softReq =
        new PrereqRequirement(courseId, prereqId, "COURSE_PASSED", "MEDIUM", true, "Estadística");

    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);
    PassedCourseSet passed = new PassedCourseSet(Set.of(prereqId)); // passed → no warning

    SimulationValidationResult result =
        service.validate(
            candidate, List.of(), List.of(softReq), passed, creditLimit, noPredecessor, 0);

    assertThat(result.hasBlockers()).isFalse();
    assertThat(result.warnings()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // WU4b: Multiple blockers collected (no short-circuit)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("WU4b: duplicate + archived → both blockers collected, full array")
  void multipleBlockers_allCollected() {
    // Candidate is ARCHIVED and courseId is already in simulation
    ExistingSelection duplicate =
        new ExistingSelection(UUID.randomUUID(), courseId, "New Course", programId, 3.0, List.of());
    SimScheduleSession session =
        new SimScheduleSession("MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0));
    CandidateSelection candidate =
        new CandidateSelection(
            UUID.randomUUID(),
            UUID.randomUUID(),
            courseId,
            "New Course",
            programId,
            3.0,
            "DIURNA",
            "ARCHIVED",
            1L,
            List.of(session));

    SimulationValidationResult result =
        service.validate(
            candidate,
            List.of(duplicate),
            List.of(),
            PassedCourseSet.empty(),
            creditLimit,
            noPredecessor,
            0);

    assertThat(result.hasBlockers()).isTrue();
    List<String> codes = result.blocking().stream().map(ValidationNotice::code).toList();
    assertThat(codes).contains("SIMULATION_DUPLICATE_SELECTION", "SIMULATION_ARCHIVED_OFFERING");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CandidateSelection buildCandidate(
      UUID cId, UUID pId, double credits, String day, int startH, int startM, int endH, int endM) {
    SimScheduleSession session =
        new SimScheduleSession(day, LocalTime.of(startH, startM), LocalTime.of(endH, endM));
    return new CandidateSelection(
        UUID.randomUUID(),
        UUID.randomUUID(),
        cId,
        "New Course",
        pId,
        credits,
        "DIURNA",
        "FINAL",
        1L,
        List.of(session));
  }
}
