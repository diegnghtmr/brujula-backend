package co.edu.uniquindio.brujula.simulation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.simulation.application.dto.ScheduleOptionResolutionView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationDetailView;
import co.edu.uniquindio.brujula.simulation.application.port.in.GetSimulationQuery;
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
import co.edu.uniquindio.brujula.simulation.domain.SimulationStillBlockedException;
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
 * Batch-credits regression test for {@link ReviewSimulationService}.
 *
 * <p>Asserts that {@code coursesCredits(Set<UUID>)} is called <strong>exactly once</strong> for the
 * accumulated-passed-credits computation in the per-selection loop, rather than once per passed
 * course per selection (O(N×S) where S = number of active selections).
 *
 * <p>Also asserts credit-sum correctness: 30 passed courses × 3 credits = 90 accumulated credits.
 * The threshold-based assertion confirms the MIN_CREDITS gate fires correctly.
 */
class ReviewSimulationServiceBatchCreditsTest {

  private LoadSimulationPort loadSimulationPort;
  private LoadSimulationSelectionPort loadSimulationSelectionPort;
  private SaveSimulationSelectionPort saveSimulationSelectionPort;
  private OfferingReadPort offeringReadPort;
  private CreditLimitPort creditLimitPort;
  private ProgressReadPort progressReadPort;
  private CountingCurriculumReadPort curriculumReadPort;
  private ProgramNameReadPort programNameReadPort;
  private GetSimulationQuery getSimulationQuery;
  private ReviewSimulationService sut;

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
    getSimulationQuery = mock(GetSimulationQuery.class);

    sut =
        new ReviewSimulationService(
            loadSimulationPort,
            loadSimulationSelectionPort,
            saveSimulationSelectionPort,
            offeringReadPort,
            creditLimitPort,
            progressReadPort,
            curriculumReadPort,
            programNameReadPort,
            getSimulationQuery);
  }

  /**
   * 30 passed courses → batch credits method called exactly once.
   *
   * <p>The current O(N) loop calls {@code courseCredits(UUID)} once per passed course per active
   * selection. After refactor, {@code coursesCredits(Set<UUID>)} must be called ONCE (hoisted
   * before the selection loop), not {@code N_courses × N_selections} times.
   */
  @Test
  void reviewSimulation_withThirtyPassedCourses_callsBatchMethodExactlyOnce() {
    UUID simId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID offeringId = UUID.randomUUID();
    UUID scheduleOptionId = UUID.randomUUID();
    UUID selId = UUID.randomUUID();

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

    SimulationSelection selection =
        SimulationSelection.reconstitute(
            new SimulationSelectionId(selId),
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

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId))
        .thenReturn(List.of(selection));
    when(offeringReadPort.resolveScheduleOption(scheduleOptionId)).thenReturn(resolved);
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(passedCourseIds);
    when(creditLimitPort.creditLimit(any(), any())).thenReturn(23);
    when(programNameReadPort.programName(any())).thenReturn("Systems Engineering");
    when(saveSimulationSelectionPort.save(any())).thenReturn(selection);
    when(getSimulationQuery.getSimulation(simId, OWNER_ID))
        .thenReturn(mock(SimulationDetailView.class));

    sut.reviewSimulation(simId, OWNER_ID);

    assertThat(curriculumReadPort.coursesCreditsCallCount.get())
        .as(
            "coursesCredits(Set<UUID>) must be called exactly once for 30 passed courses "
                + "(hoisted before the selection loop), not once per selection per passed course. "
                + "Refactor the O(N) loop in ReviewSimulationService step 5.")
        .isEqualTo(1);
  }

  /**
   * Credit-value correctness — 30 × 3 = 90 accumulated credits.
   *
   * <p>Seeds 30 passed courses × 3 credits each. Asserts that:
   *
   * <ul>
   *   <li>required=80, accumulated=90 → review succeeds (no SIMULATION_MIN_CREDITS_NOT_MET blocker)
   *   <li>required=100, accumulated=90 → review throws {@code SimulationStillBlockedException}
   * </ul>
   *
   * <p>This locks the credit-sum correctness: a bug that sums UUIDs, drops courses, or returns zero
   * would make accumulated credits mismatch 90 and break this test.
   */
  @Test
  void reviewSimulation_creditSumDrivesMinCreditsGate_correctlyFlipsAroundThreshold() {
    Set<UUID> passedCourseIds = new HashSet<>();
    for (int i = 0; i < PASSED_COURSE_COUNT; i++) {
      passedCourseIds.add(UUID.randomUUID());
    }

    // --- Scenario A: required=80, accumulated=90 → review succeeds ---
    UUID simIdA = UUID.randomUUID();
    UUID courseIdA = UUID.randomUUID();
    UUID offeringIdA = UUID.randomUUID();
    UUID schedOptIdA = UUID.randomUUID();
    UUID selIdA = UUID.randomUUID();

    PrereqRequirement minCreditsReq80 =
        new PrereqRequirement(courseIdA, null, "MINIMUM_CREDITS", "HIGH", false, null, 80);

    curriculumReadPort.requirementsForCourseId = courseIdA;
    curriculumReadPort.requirementsForCourseIdResult = List.of(minCreditsReq80);

    Simulation simA =
        Simulation.reconstitute(
            new SimulationId(simIdA),
            OWNER_ID,
            "Test Sim A",
            PERIOD_ID,
            List.of(PROGRAM_ID),
            SimulationLifecycle.ACTIVE,
            Instant.now(),
            Instant.now(),
            0L);

    SimulationSelection selA =
        SimulationSelection.reconstitute(
            new SimulationSelectionId(selIdA),
            simIdA,
            offeringIdA,
            schedOptIdA,
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

    ScheduleOptionResolutionView resolvedA =
        new ScheduleOptionResolutionView(
            schedOptIdA,
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

    when(loadSimulationPort.loadById(simIdA)).thenReturn(Optional.of(simA));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simIdA)).thenReturn(List.of(selA));
    when(offeringReadPort.resolveScheduleOption(schedOptIdA)).thenReturn(resolvedA);
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(passedCourseIds);
    when(creditLimitPort.creditLimit(any(), any())).thenReturn(23);
    when(programNameReadPort.programName(any())).thenReturn("Systems Engineering");
    when(saveSimulationSelectionPort.save(any())).thenReturn(selA);
    when(getSimulationQuery.getSimulation(simIdA, OWNER_ID))
        .thenReturn(mock(SimulationDetailView.class));

    assertThatCode(() -> sut.reviewSimulation(simIdA, OWNER_ID))
        .as("30 × 3 = 90 accumulated; required=80 → no blocker → reviewSimulation succeeds")
        .doesNotThrowAnyException();

    // --- Scenario B: required=100, accumulated=90 → throws SimulationStillBlockedException ---
    UUID simIdB = UUID.randomUUID();
    UUID courseIdB = UUID.randomUUID();
    UUID offeringIdB = UUID.randomUUID();
    UUID schedOptIdB = UUID.randomUUID();
    UUID selIdB = UUID.randomUUID();

    PrereqRequirement minCreditsReq100 =
        new PrereqRequirement(courseIdB, null, "MINIMUM_CREDITS", "HIGH", false, null, 100);

    curriculumReadPort.requirementsForCourseId = courseIdB;
    curriculumReadPort.requirementsForCourseIdResult = List.of(minCreditsReq100);

    Simulation simB =
        Simulation.reconstitute(
            new SimulationId(simIdB),
            OWNER_ID,
            "Test Sim B",
            PERIOD_ID,
            List.of(PROGRAM_ID),
            SimulationLifecycle.ACTIVE,
            Instant.now(),
            Instant.now(),
            0L);

    SimulationSelection selB =
        SimulationSelection.reconstitute(
            new SimulationSelectionId(selIdB),
            simIdB,
            offeringIdB,
            schedOptIdB,
            courseIdB,
            "Test Course B",
            PROGRAM_ID,
            3.0,
            "DIURNA",
            "PRELIMINARY",
            1L,
            List.of(),
            "ACTIVE",
            0L);

    ScheduleOptionResolutionView resolvedB =
        new ScheduleOptionResolutionView(
            schedOptIdB,
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
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simIdB)).thenReturn(List.of(selB));
    when(offeringReadPort.resolveScheduleOption(schedOptIdB)).thenReturn(resolvedB);
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(passedCourseIds);

    assertThatThrownBy(() -> sut.reviewSimulation(simIdB, OWNER_ID))
        .as(
            "30 × 3 = 90 accumulated; required=100 → SIMULATION_MIN_CREDITS_NOT_MET blocker → throws")
        .isInstanceOf(SimulationStillBlockedException.class);
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
      // Single-course lookup — must NOT be called for passed-courses accumulation after refactor
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
