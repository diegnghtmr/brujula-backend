package co.edu.uniquindio.brujula.simulation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import co.edu.uniquindio.brujula.simulation.application.dto.AddSelectionCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.ScheduleOptionResolutionView;
import co.edu.uniquindio.brujula.simulation.application.port.out.CreditLimitPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgramNameReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgressReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.domain.PrereqRequirement;
import co.edu.uniquindio.brujula.simulation.domain.SequentialPredecessor;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * DEBT-F7-001 — Batch-credits regression test for {@link AddSimulationSelectionService}.
 *
 * <p>TDD RED: Asserts that {@code coursesCredits(Set<UUID>)} is called <strong>exactly
 * once</strong> for the accumulated-passed-credits computation instead of once-per-passed-course
 * (O(N) loop at step 10).
 *
 * <p>Also asserts credit-sum correctness: 30 passed courses × 3 credits = 90 accumulated credits. A
 * threshold-based assertion confirms the credit gate fires correctly above/below 90 — matching the
 * pattern established in {@link GetSimulationServiceQueryCountTest} for WU3b.
 */
class AddSimulationSelectionServiceBatchCreditsTest {

  private LoadSimulationPort loadSimulationPort;
  private LoadSimulationSelectionPort loadSimulationSelectionPort;
  private SaveSimulationSelectionPort saveSimulationSelectionPort;
  private OfferingReadPort offeringReadPort;
  private CreditLimitPort creditLimitPort;
  private ProgressReadPort progressReadPort;
  private CountingCurriculumReadPort curriculumReadPort;
  private ProgramNameReadPort programNameReadPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AddSimulationSelectionService sut;

  private static final UUID OWNER_ID = UUID.randomUUID();
  private static final UUID PROGRAM_ID = UUID.randomUUID();
  private static final UUID PERIOD_ID = UUID.randomUUID();
  private static final int PASSED_COURSE_COUNT = 30;

  @BeforeEach
  void setUp() {
    loadSimulationPort = mock(LoadSimulationPort.class);
    loadSimulationSelectionPort = mock(LoadSimulationSelectionPort.class);
    saveSimulationSelectionPort = mock(SaveSimulationSelectionPort.class);
    offeringReadPort = mock(OfferingReadPort.class);
    creditLimitPort = mock(CreditLimitPort.class);
    progressReadPort = mock(ProgressReadPort.class);
    curriculumReadPort = new CountingCurriculumReadPort();
    programNameReadPort = mock(ProgramNameReadPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);

    sut =
        new AddSimulationSelectionService(
            loadSimulationPort,
            loadSimulationSelectionPort,
            saveSimulationSelectionPort,
            offeringReadPort,
            creditLimitPort,
            progressReadPort,
            curriculumReadPort,
            programNameReadPort,
            idempotencyStorePort,
            jsonCodecPort);
  }

  /**
   * DEBT-F7-001-A1: 30 passed courses → batch credits method called exactly once.
   *
   * <p>This test will FAIL (RED) until {@code addSelection} step 10 is refactored to use {@code
   * coursesCredits(Set<UUID>)} instead of the per-course {@code courseCredits(UUID)} loop.
   */
  @Test
  void addSelection_withThirtyPassedCourses_callsBatchMethodExactlyOnce() {
    UUID simId = UUID.randomUUID();
    UUID scheduleOptionId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID offeringId = UUID.randomUUID();

    Simulation simulation =
        Simulation.reconstitute(
            new SimulationId(simId),
            OWNER_ID,
            "Test Simulation",
            PERIOD_ID,
            List.of(PROGRAM_ID),
            SimulationLifecycle.ACTIVE,
            Instant.now(),
            Instant.now(),
            0L);

    // 30 distinct passed course IDs
    Set<UUID> passedCourseIds = new HashSet<>();
    for (int i = 0; i < PASSED_COURSE_COUNT; i++) {
      passedCourseIds.add(UUID.randomUUID());
    }

    ScheduleOptionResolutionView resolved =
        new ScheduleOptionResolutionView(
            scheduleOptionId,
            offeringId,
            courseId,
            "Test Course",
            PROGRAM_ID,
            PERIOD_ID,
            "DIURNA",
            4.0,
            "PRELIMINARY",
            1L,
            List.of(
                new ScheduleOptionResolutionView.SessionSlot(
                    "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0))));

    SimulationSelection savedSelection =
        SimulationSelection.reconstitute(
            new SimulationSelectionId(UUID.randomUUID()),
            simId,
            offeringId,
            scheduleOptionId,
            courseId,
            "Test Course",
            PROGRAM_ID,
            3.0,
            "DIURNA",
            "PRELIMINARY",
            1L,
            List.of(),
            "ACTIVE",
            0L);

    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(offeringReadPort.resolveScheduleOption(scheduleOptionId)).thenReturn(resolved);
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId)).thenReturn(List.of());
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(passedCourseIds);
    when(creditLimitPort.creditLimit(any(), any())).thenReturn(23);
    when(programNameReadPort.programName(any())).thenReturn("Systems Engineering");
    when(saveSimulationSelectionPort.save(any())).thenReturn(savedSelection);
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    AddSelectionCommand command =
        new AddSelectionCommand(
            UUID.randomUUID(), simId, OWNER_ID, scheduleOptionId, false, "hash");

    sut.addSelection(command);

    assertThat(curriculumReadPort.coursesCreditsCallCount.get())
        .as(
            "coursesCredits(Set<UUID>) must be called exactly once for 30 passed courses, "
                + "not once-per-course. Refactor the O(N) loop in step 10.")
        .isEqualTo(1);
  }

  /**
   * DEBT-F7-001-A2: Credit-value correctness — 30 × 3 = 90 accumulated credits.
   *
   * <p>Seeds 30 passed courses × 3 credits each = 90 accumulated credits. Asserts that:
   *
   * <ul>
   *   <li>When required minimum = 80 and accumulated = 90 → NO SIMULATION_MIN_CREDITS_NOT_MET
   *       blocker (selection succeeds).
   *   <li>When required minimum = 100 and accumulated = 90 → SIMULATION_MIN_CREDITS_NOT_MET blocker
   *       IS present (throws {@code SimulationValidationBlockedException}).
   * </ul>
   *
   * <p>A count-only test cannot catch a bug that sums UUIDs instead of credit values, or drops
   * courses. This value-assertion test locks the correctness of the sum.
   */
  @Test
  void addSelection_creditSumDrivesMinCreditsGate_correctlyFlipsAroundThreshold() {
    Set<UUID> passedCourseIds = new HashSet<>();
    for (int i = 0; i < PASSED_COURSE_COUNT; i++) {
      passedCourseIds.add(UUID.randomUUID());
    }

    // --- Scenario: required=80, accumulated=90 → selection succeeds (no blocker) ---
    UUID simIdA = UUID.randomUUID();
    UUID scheduleOptionIdA = UUID.randomUUID();
    UUID courseIdA = UUID.randomUUID();
    UUID offeringIdA = UUID.randomUUID();

    PrereqRequirement minCreditsReq80 =
        new PrereqRequirement(courseIdA, null, "MINIMUM_CREDITS", "HIGH", false, null, 80);

    curriculumReadPort.requirementsForCourseId = courseIdA;
    curriculumReadPort.requirementsForCourseIdResult = List.of(minCreditsReq80);

    Simulation simA =
        Simulation.reconstitute(
            new SimulationId(simIdA),
            OWNER_ID,
            "Test Sim Credits A",
            PERIOD_ID,
            List.of(PROGRAM_ID),
            SimulationLifecycle.ACTIVE,
            Instant.now(),
            Instant.now(),
            0L);

    ScheduleOptionResolutionView resolvedA =
        new ScheduleOptionResolutionView(
            scheduleOptionIdA,
            offeringIdA,
            courseIdA,
            "Test Course A",
            PROGRAM_ID,
            PERIOD_ID,
            "DIURNA",
            4.0,
            "PRELIMINARY",
            1L,
            List.of(
                new ScheduleOptionResolutionView.SessionSlot(
                    "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0))));

    SimulationSelection savedSelectionA =
        SimulationSelection.reconstitute(
            new SimulationSelectionId(UUID.randomUUID()),
            simIdA,
            offeringIdA,
            scheduleOptionIdA,
            courseIdA,
            "Test Course A",
            PROGRAM_ID,
            3.0,
            "DIURNA",
            "PRELIMINARY",
            1L,
            List.of(),
            "ACTIVE",
            0L);

    when(idempotencyStorePort.find(any(), any())).thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simIdA)).thenReturn(Optional.of(simA));
    when(offeringReadPort.resolveScheduleOption(scheduleOptionIdA)).thenReturn(resolvedA);
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simIdA)).thenReturn(List.of());
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(passedCourseIds);
    when(creditLimitPort.creditLimit(any(), any())).thenReturn(23);
    when(programNameReadPort.programName(any())).thenReturn("Systems Engineering");
    when(saveSimulationSelectionPort.save(any())).thenReturn(savedSelectionA);
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    AddSelectionCommand commandA =
        new AddSelectionCommand(
            UUID.randomUUID(), simIdA, OWNER_ID, scheduleOptionIdA, false, "hashA");

    // Should NOT throw (no SIMULATION_MIN_CREDITS_NOT_MET blocker when accumulated=90 >
    // required=80)
    org.assertj.core.api.Assertions.assertThatCode(() -> sut.addSelection(commandA))
        .as("30 × 3 = 90 accumulated; required=80 → no MIN_CREDITS blocker → addSelection succeeds")
        .doesNotThrowAnyException();

    // --- Scenario: required=100, accumulated=90 → blocker present → throws ---
    UUID simIdB = UUID.randomUUID();
    UUID scheduleOptionIdB = UUID.randomUUID();
    UUID courseIdB = UUID.randomUUID();
    UUID offeringIdB = UUID.randomUUID();

    PrereqRequirement minCreditsReq100 =
        new PrereqRequirement(courseIdB, null, "MINIMUM_CREDITS", "HIGH", false, null, 100);

    curriculumReadPort.requirementsForCourseId = courseIdB;
    curriculumReadPort.requirementsForCourseIdResult = List.of(minCreditsReq100);

    Simulation simB =
        Simulation.reconstitute(
            new SimulationId(simIdB),
            OWNER_ID,
            "Test Sim Credits B",
            PERIOD_ID,
            List.of(PROGRAM_ID),
            SimulationLifecycle.ACTIVE,
            Instant.now(),
            Instant.now(),
            0L);

    ScheduleOptionResolutionView resolvedB =
        new ScheduleOptionResolutionView(
            scheduleOptionIdB,
            offeringIdB,
            courseIdB,
            "Test Course B",
            PROGRAM_ID,
            PERIOD_ID,
            "DIURNA",
            4.0,
            "PRELIMINARY",
            1L,
            List.of(
                new ScheduleOptionResolutionView.SessionSlot(
                    "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0))));

    when(loadSimulationPort.loadById(simIdB)).thenReturn(Optional.of(simB));
    when(offeringReadPort.resolveScheduleOption(scheduleOptionIdB)).thenReturn(resolvedB);
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simIdB)).thenReturn(List.of());
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(passedCourseIds);

    AddSelectionCommand commandB =
        new AddSelectionCommand(
            UUID.randomUUID(), simIdB, OWNER_ID, scheduleOptionIdB, false, "hashB");

    // Should throw SimulationValidationBlockedException (accumulated=90 < required=100)
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> sut.addSelection(commandB))
        .as(
            "30 × 3 = 90 accumulated; required=100 → SIMULATION_MIN_CREDITS_NOT_MET blocker → throws")
        .isInstanceOf(
            co.edu.uniquindio.brujula.simulation.domain.SimulationValidationBlockedException.class);
  }

  // -------------------------------------------------------------------------
  // In-memory counting stub for CurriculumReadPort
  // -------------------------------------------------------------------------

  /**
   * Counting stub — counts {@code coursesCredits(Set<UUID>)} invocations. Returns 3 credits per
   * course. Supports injecting a requirement list for one specific courseId (credit-value test).
   */
  private static class CountingCurriculumReadPort implements CurriculumReadPort {

    final AtomicInteger coursesCreditsCallCount = new AtomicInteger(0);

    UUID requirementsForCourseId = null;
    List<PrereqRequirement> requirementsForCourseIdResult = List.of();

    @Override
    public List<PrereqRequirement> requirementsForCourse(UUID courseId) {
      if (requirementsForCourseId != null && requirementsForCourseId.equals(courseId)) {
        return requirementsForCourseIdResult;
      }
      return List.of();
    }

    @Override
    public int courseCredits(UUID courseId) {
      // Single-course lookup — should NOT be called for passed-courses accumulation after refactor
      return 3;
    }

    @Override
    public Map<UUID, Integer> coursesCredits(Set<UUID> courseIds) {
      coursesCreditsCallCount.incrementAndGet();
      Map<UUID, Integer> credits = new HashMap<>();
      for (UUID id : courseIds) {
        credits.put(id, 3);
      }
      return credits;
    }

    @Override
    public SequentialPredecessor sequentialPredecessor(UUID courseId, UUID planId) {
      return new SequentialPredecessor(courseId, "REQUIRED", null, null);
    }
  }
}
