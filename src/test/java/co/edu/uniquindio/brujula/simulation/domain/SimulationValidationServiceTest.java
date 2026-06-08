package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SimulationValidationService} — orchestration and partitioning.
 */
class SimulationValidationServiceTest {

  private SimulationValidationService service;
  private final UUID programId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private final UUID courseId = UUID.fromString("cccccccc-0000-0000-0000-000000000002");
  private final ProgramCreditLimit creditLimit = new ProgramCreditLimit(programId, "Sistemas", 23);

  @BeforeEach
  void setUp() {
    service = new SimulationValidationService();
  }

  @Test
  void allClean_returnsAllowed() {
    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "WEDNESDAY", 18, 0, 20, 0);
    SimulationValidationResult result = service.validate(
        candidate, List.of(), List.of(), PassedCourseSet.empty(), creditLimit);

    assertThat(result.hasBlockers()).isFalse();
    assertThat(result.blocking()).isEmpty();
    assertThat(result.warnings()).isEmpty();
    assertThat(result.toOutcome()).isInstanceOf(ValidationOutcome.Allowed.class);
  }

  @Test
  void scheduleClash_returnsBlocked() {
    SimScheduleSession clashSession = new SimScheduleSession("WEDNESDAY",
        LocalTime.of(18, 0), LocalTime.of(20, 0));
    ExistingSelection existing = new ExistingSelection(
        UUID.randomUUID(), UUID.randomUUID(), "Cálculo I", programId, 3.0, List.of(clashSession));

    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "WEDNESDAY", 18, 0, 20, 0);

    SimulationValidationResult result = service.validate(
        candidate, List.of(existing), List.of(), PassedCourseSet.empty(), creditLimit);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.blocking()).anyMatch(n -> "SIMULATION_SCHEDULE_CLASH".equals(n.code()));
    assertThat(result.toOutcome()).isInstanceOf(ValidationOutcome.Blocked.class);
  }

  @Test
  void creditLimitExceeded_returnsBlocked() {
    ExistingSelection existing = new ExistingSelection(
        UUID.randomUUID(), UUID.randomUUID(), "Física I", programId, 21.0, List.of());

    // 21 + 3 = 24 > 23
    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);

    SimulationValidationResult result = service.validate(
        candidate, List.of(existing), List.of(), PassedCourseSet.empty(), creditLimit);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.blocking()).anyMatch(n -> "SIMULATION_CREDIT_LIMIT_EXCEEDED".equals(n.code()));
  }

  @Test
  void alreadyPassed_returnsBlocked() {
    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);
    PassedCourseSet passed = new PassedCourseSet(Set.of(courseId));

    SimulationValidationResult result = service.validate(
        candidate, List.of(), List.of(), passed, creditLimit);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.blocking()).anyMatch(n -> "SIMULATION_COURSE_ALREADY_PASSED".equals(n.code()));
  }

  @Test
  void prereqNotMet_returnsBlocked() {
    UUID prereqId = UUID.randomUUID();
    PrereqRequirement req = new PrereqRequirement(
        courseId, prereqId, "COURSE_PASSED", "HIGH", false, "Cálculo I");
    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "MONDAY", 8, 0, 10, 0);

    SimulationValidationResult result = service.validate(
        candidate, List.of(), List.of(req), PassedCourseSet.empty(), creditLimit);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.blocking()).anyMatch(n -> "SIMULATION_PREREQUISITE_NOT_MET".equals(n.code()));
  }

  @Test
  void blockingPartitionedCorrectly() {
    // Trigger two rules simultaneously: alreadyPassed + scheduleClash
    SimScheduleSession clashSession = new SimScheduleSession("WEDNESDAY",
        LocalTime.of(18, 0), LocalTime.of(20, 0));
    ExistingSelection existing = new ExistingSelection(
        UUID.randomUUID(), UUID.randomUUID(), "Física I", programId, 3.0, List.of(clashSession));

    PassedCourseSet passed = new PassedCourseSet(Set.of(courseId));
    CandidateSelection candidate = buildCandidate(courseId, programId, 3.0, "WEDNESDAY", 18, 0, 20, 0);

    SimulationValidationResult result = service.validate(
        candidate, List.of(existing), List.of(), passed, creditLimit);

    assertThat(result.hasBlockers()).isTrue();
    // Both AlreadyPassed AND clash detected
    assertThat(result.blocking().stream().map(ValidationNotice::code).toList())
        .contains("SIMULATION_COURSE_ALREADY_PASSED", "SIMULATION_SCHEDULE_CLASH");
    assertThat(result.warnings()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CandidateSelection buildCandidate(UUID cId, UUID pId, double credits,
      String day, int startH, int startM, int endH, int endM) {
    SimScheduleSession session = new SimScheduleSession(day,
        LocalTime.of(startH, startM), LocalTime.of(endH, endM));
    return new CandidateSelection(UUID.randomUUID(), UUID.randomUUID(), cId,
        "New Course", pId, credits, "DIURNA", "FINAL", 1L, List.of(session));
  }
}
