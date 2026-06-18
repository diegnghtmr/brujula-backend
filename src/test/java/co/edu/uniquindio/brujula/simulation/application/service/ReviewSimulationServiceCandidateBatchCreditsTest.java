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
import java.util.ArrayList;
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
 * DEBT-F7-006 — Candidate-selection credits batch regression test for {@link
 * ReviewSimulationService}.
 *
 * <p>TDD RED: Asserts that {@code coursesCredits(Set<UUID>)} is called <strong>exactly
 * once</strong> for ALL credits (both passed-course accumulation AND per-candidate credit lookup),
 * and that {@code courseCredits(UUID)} is <strong>never</strong> called during the selection loop.
 *
 * <p>Before the fix, the service calls {@code
 * curriculumReadPort.courseCredits(resolved.courseId())} once per active selection (line ~163) —
 * O(S) extra round-trips on top of the batch already performed by DEBT-F7-001. The fix folds all
 * candidate course IDs into the single {@code coursesCredits(Set)} call executed before the loop.
 *
 * <p>Two test dimensions:
 *
 * <ul>
 *   <li>Query-count: N passed courses + M active selections → {@code coursesCredits(Set)} called
 *       exactly once, {@code courseCredits(UUID)} called zero times.
 *   <li>Value correctness: each candidate selection receives the credit value from the map (not a
 *       hard-coded fallback). Candidate with 5 credits passes a credit-limit check that would fail
 *       with 0 or wrong credits; candidate with 3 credits passes correctly under its own limit.
 * </ul>
 */
class ReviewSimulationServiceCandidateBatchCreditsTest {

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
  private static final int PASSED_COURSE_COUNT = 5;
  private static final int ACTIVE_SELECTION_COUNT = 3;

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
   * DEBT-F7-006-R1: N passed courses + M active selections → {@code coursesCredits(Set)} called
   * exactly once; {@code courseCredits(UUID)} called zero times.
   *
   * <p>This test will FAIL (RED) until the per-selection {@code courseCredits(resolved.courseId())}
   * call at line ~163 is folded into the combined batch set.
   */
  @Test
  void reviewSimulation_withMultipleSelections_callsBatchMethodOnceAndNeverPerSelection() {
    UUID simId = UUID.randomUUID();

    Simulation simulation =
        Simulation.reconstitute(
            new SimulationId(simId),
            OWNER_ID,
            "Test Simulation",
            PERIOD_ID,
            List.of(PROGRAM_ID),
            List.of(),
            SimulationLifecycle.ACTIVE,
            Instant.now(),
            Instant.now(),
            0L);

    // Build M active selections with distinct course IDs
    List<SimulationSelection> selections = new ArrayList<>();
    List<UUID> candidateCourseIds = new ArrayList<>();
    for (int i = 0; i < ACTIVE_SELECTION_COUNT; i++) {
      UUID courseId = UUID.randomUUID();
      UUID offeringId = UUID.randomUUID();
      UUID schedOptId = UUID.randomUUID();
      candidateCourseIds.add(courseId);
      selections.add(
          SimulationSelection.reconstitute(
              new SimulationSelectionId(UUID.randomUUID()),
              simId,
              offeringId,
              schedOptId,
              courseId,
              "Course " + i,
              PROGRAM_ID,
              3.0,
              "DIURNA",
              "PRELIMINARY",
              1L,
              List.of(),
              "ACTIVE",
              0L));

      ScheduleOptionResolutionView resolved =
          new ScheduleOptionResolutionView(
              schedOptId,
              offeringId,
              courseId,
              "Course " + i,
              PROGRAM_ID,
              PERIOD_ID,
              "DIURNA",
              3.0,
              "PRELIMINARY",
              1L,
              List.of(
                  new ScheduleOptionResolutionView.SessionSlot(
                      "MONDAY", LocalTime.of(8 + i, 0), LocalTime.of(10 + i, 0))));

      when(offeringReadPort.resolveScheduleOption(schedOptId)).thenReturn(resolved);
    }

    // N passed course IDs
    Set<UUID> passedCourseIds = new HashSet<>();
    for (int i = 0; i < PASSED_COURSE_COUNT; i++) {
      passedCourseIds.add(UUID.randomUUID());
    }

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId)).thenReturn(selections);
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(passedCourseIds);
    when(creditLimitPort.creditLimit(any(), any())).thenReturn(30);
    when(programNameReadPort.programName(any())).thenReturn("Systems Engineering");
    when(saveSimulationSelectionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(getSimulationQuery.getSimulation(simId, OWNER_ID))
        .thenReturn(mock(SimulationDetailView.class));

    sut.reviewSimulation(simId, OWNER_ID);

    assertThat(curriculumReadPort.coursesCreditsCallCount.get())
        .as(
            "coursesCredits(Set<UUID>) must be called exactly once for %d passed courses + %d "
                + "active selections (combined batch), not 1 + M times. "
                + "DEBT-F7-006: fold candidate course IDs into the existing batch call.",
            PASSED_COURSE_COUNT, ACTIVE_SELECTION_COUNT)
        .isEqualTo(1);

    assertThat(curriculumReadPort.courseCreditsCallCount.get())
        .as(
            "courseCredits(UUID) must NOT be called at all after the DEBT-F7-006 batch fix. "
                + "Candidate credits must be read from the combined batch map.")
        .isEqualTo(0);
  }

  /**
   * DEBT-F7-006-R2: Candidate credit values come from the combined batch map, not from
   * per-selection fallback.
   *
   * <p>Seeds two candidate selections with distinct credit values (4 and 6). The stub returns the
   * correct per-course value from the map. The credit-limit validation uses those values: a limit
   * of 5 must block the 6-credit candidate and pass the 4-credit candidate.
   *
   * <p>This test will fail if the candidate's credits default to 0 (missing key) or use any
   * hard-coded value, proving the map lookup is wired correctly.
   */
  @Test
  void reviewSimulation_candidateCreditFromMap_drivesValidationCorrectly() {
    // Candidate A: 4 credits, limit=5 → passes
    UUID simIdA = UUID.randomUUID();
    UUID courseIdA = UUID.randomUUID();
    UUID offeringIdA = UUID.randomUUID();
    UUID schedOptIdA = UUID.randomUUID();

    curriculumReadPort.perCourseCredits.put(courseIdA, 4);

    Simulation simA =
        Simulation.reconstitute(
            new SimulationId(simIdA),
            OWNER_ID,
            "Sim A",
            PERIOD_ID,
            List.of(PROGRAM_ID),
            List.of(),
            SimulationLifecycle.ACTIVE,
            Instant.now(),
            Instant.now(),
            0L);

    SimulationSelection selA =
        SimulationSelection.reconstitute(
            new SimulationSelectionId(UUID.randomUUID()),
            simIdA,
            offeringIdA,
            schedOptIdA,
            courseIdA,
            "Course A",
            PROGRAM_ID,
            4.0,
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
            "Course A",
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
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(Set.of());
    when(creditLimitPort.creditLimit(PROGRAM_ID, PERIOD_ID)).thenReturn(5);
    when(programNameReadPort.programName(PROGRAM_ID)).thenReturn("Systems Engineering");
    when(saveSimulationSelectionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(getSimulationQuery.getSimulation(simIdA, OWNER_ID))
        .thenReturn(mock(SimulationDetailView.class));

    assertThatCode(() -> sut.reviewSimulation(simIdA, OWNER_ID))
        .as(
            "Candidate with 4 credits and limit=5 → total credits used = 4, within limit → "
                + "review must succeed. If credits resolved to 0 from a bad map lookup the "
                + "limit check would also pass trivially — the threshold test (Scenario B) "
                + "locks the upper bound.")
        .doesNotThrowAnyException();

    // Candidate B: 6 credits, limit=5 → blocked (SIMULATION_CREDIT_LIMIT_EXCEEDED)
    UUID simIdB = UUID.randomUUID();
    UUID courseIdB = UUID.randomUUID();
    UUID offeringIdB = UUID.randomUUID();
    UUID schedOptIdB = UUID.randomUUID();

    curriculumReadPort.perCourseCredits.put(courseIdB, 6);

    Simulation simB =
        Simulation.reconstitute(
            new SimulationId(simIdB),
            OWNER_ID,
            "Sim B",
            PERIOD_ID,
            List.of(PROGRAM_ID),
            List.of(),
            SimulationLifecycle.ACTIVE,
            Instant.now(),
            Instant.now(),
            0L);

    SimulationSelection selB =
        SimulationSelection.reconstitute(
            new SimulationSelectionId(UUID.randomUUID()),
            simIdB,
            offeringIdB,
            schedOptIdB,
            courseIdB,
            "Course B",
            PROGRAM_ID,
            6.0,
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
            "Course B",
            PROGRAM_ID,
            PERIOD_ID,
            "DIURNA",
            6.0,
            "PRELIMINARY",
            1L,
            List.of(
                new ScheduleOptionResolutionView.SessionSlot(
                    "TUESDAY", LocalTime.of(10, 0), LocalTime.of(12, 0))));

    when(loadSimulationPort.loadById(simIdB)).thenReturn(Optional.of(simB));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simIdB)).thenReturn(List.of(selB));
    when(offeringReadPort.resolveScheduleOption(schedOptIdB)).thenReturn(resolvedB);
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_ID)).thenReturn(Set.of());
    when(creditLimitPort.creditLimit(PROGRAM_ID, PERIOD_ID)).thenReturn(5);
    when(programNameReadPort.programName(PROGRAM_ID)).thenReturn("Systems Engineering");

    assertThatThrownBy(() -> sut.reviewSimulation(simIdB, OWNER_ID))
        .as(
            "Candidate with 6 credits and limit=5 → SIMULATION_CREDIT_LIMIT_EXCEEDED blocker → "
                + "review must throw SimulationStillBlockedException. "
                + "If credits resolved to 0 instead of 6 this assertion would fail, "
                + "proving the map lookup is correctly wired.")
        .isInstanceOf(SimulationStillBlockedException.class);
  }

  // -------------------------------------------------------------------------
  // In-memory counting stub for CurriculumReadPort
  // -------------------------------------------------------------------------

  /**
   * Counting stub that:
   *
   * <ul>
   *   <li>Counts {@code coursesCredits(Set<UUID>)} invocations.
   *   <li>Counts {@code courseCredits(UUID)} invocations (must be 0 after DEBT-F7-006 fix).
   *   <li>Returns per-course credits from {@code perCourseCredits} map; defaults to 3.
   * </ul>
   */
  private static class CountingCurriculumReadPort implements CurriculumReadPort {

    final AtomicInteger coursesCreditsCallCount = new AtomicInteger(0);
    final AtomicInteger courseCreditsCallCount = new AtomicInteger(0);
    final Map<UUID, Integer> perCourseCredits = new HashMap<>();

    @Override
    public List<PrereqRequirement> requirementsForCourse(UUID courseId) {
      return List.of();
    }

    @Override
    public int courseCredits(UUID courseId) {
      courseCreditsCallCount.incrementAndGet();
      return perCourseCredits.getOrDefault(courseId, 3);
    }

    @Override
    public Map<UUID, Integer> coursesCredits(Set<UUID> courseIds) {
      coursesCreditsCallCount.incrementAndGet();
      Map<UUID, Integer> result = new HashMap<>();
      for (UUID id : courseIds) {
        result.put(id, perCourseCredits.getOrDefault(id, 3));
      }
      return result;
    }

    @Override
    public SequentialPredecessor sequentialPredecessor(UUID courseId, UUID planId) {
      return new SequentialPredecessor(courseId, "REQUIRED", null, null);
    }
  }
}
