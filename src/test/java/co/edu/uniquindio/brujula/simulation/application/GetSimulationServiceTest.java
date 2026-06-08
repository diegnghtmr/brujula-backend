package co.edu.uniquindio.brujula.simulation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.simulation.application.dto.ProgramCreditSummaryView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationDetailView;
import co.edu.uniquindio.brujula.simulation.application.port.out.CreditLimitPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgressReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgramNameReadPort;
import co.edu.uniquindio.brujula.simulation.application.service.GetSimulationService;
import co.edu.uniquindio.brujula.simulation.domain.SimScheduleSession;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import co.edu.uniquindio.brujula.simulation.domain.PrereqRequirement;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GetSimulationService} (spec SIM-003, DET-001..003, STALE-001, X-003).
 *
 * <p>Strict TDD — stubs only for output ports. No Spring context.
 */
class GetSimulationServiceTest {

  private LoadSimulationPort loadSimulationPort;
  private LoadSimulationSelectionPort loadSimulationSelectionPort;
  private OfferingReadPort offeringReadPort;
  private CreditLimitPort creditLimitPort;
  private ProgressReadPort progressReadPort;
  private CurriculumReadPort curriculumReadPort;
  private ProgramNameReadPort programNameReadPort;
  private GetSimulationService sut;

  private static final UUID OWNER_ID = UUID.randomUUID();
  private static final UUID OTHER_ID = UUID.randomUUID();
  private static final UUID PROGRAM_A = UUID.randomUUID();
  private static final UUID PROGRAM_B = UUID.randomUUID();
  private static final UUID PERIOD_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSimulationPort = mock(LoadSimulationPort.class);
    loadSimulationSelectionPort = mock(LoadSimulationSelectionPort.class);
    offeringReadPort = mock(OfferingReadPort.class);
    creditLimitPort = mock(CreditLimitPort.class);
    progressReadPort = mock(ProgressReadPort.class);
    curriculumReadPort = mock(CurriculumReadPort.class);
    programNameReadPort = mock(ProgramNameReadPort.class);
    sut = new GetSimulationService(
        loadSimulationPort, loadSimulationSelectionPort,
        offeringReadPort, creditLimitPort, progressReadPort,
        curriculumReadPort, programNameReadPort);
  }

  // -------------------------------------------------------------------------
  // Helper builders
  // -------------------------------------------------------------------------

  private Simulation makeSimulation(UUID simId, List<UUID> programIds) {
    return Simulation.reconstitute(
        new SimulationId(simId), OWNER_ID, "Test Sim", PERIOD_ID,
        programIds, SimulationLifecycle.ACTIVE,
        Instant.now(), Instant.now(), 0L);
  }

  private SimulationSelection makeSelection(UUID selId, UUID simId, UUID offeringId,
      UUID courseId, UUID programId, double credits, long selectedVersion,
      List<SimScheduleSession> sessions) {
    return SimulationSelection.reconstitute(
        new SimulationSelectionId(selId), simId, offeringId, UUID.randomUUID(),
        courseId, "Test Course " + courseId, programId, credits,
        "DIURNA", "PRELIMINARY", selectedVersion, sessions, "ACTIVE", 0L);
  }

  // -------------------------------------------------------------------------
  // No selections → creditSummary has 0-credit entries, empty schedule, CURRENT
  // -------------------------------------------------------------------------

  @Test
  void getSimulation_noSelections_oneEntryPerProgramWithZeroCredits() {
    UUID simId = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId, List.of(PROGRAM_A, PROGRAM_B));

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId)).thenReturn(List.of());
    when(offeringReadPort.resolvePeriodName(PERIOD_ID)).thenReturn("2026-2");
    when(creditLimitPort.creditLimit(PROGRAM_A, PERIOD_ID)).thenReturn(23);
    when(creditLimitPort.creditLimit(PROGRAM_B, PERIOD_ID)).thenReturn(18);
    when(programNameReadPort.programName(PROGRAM_A)).thenReturn("Systems Engineering");
    when(programNameReadPort.programName(PROGRAM_B)).thenReturn("Languages");

    SimulationDetailView result = sut.getSimulation(simId, OWNER_ID);

    assertThat(result.statusName()).isEqualTo("CURRENT");
    assertThat(result.selections()).isEmpty();
    assertThat(result.weeklySchedule()).isEmpty();
    assertThat(result.creditSummary()).hasSize(2);

    ProgramCreditSummaryView csA = result.creditSummary().stream()
        .filter(cs -> PROGRAM_A.equals(cs.programId())).findFirst().orElseThrow();
    assertThat(csA.selectedCredits()).isEqualTo(0.0);
    assertThat(csA.creditLimit()).isEqualTo(23);
    assertThat(csA.programName()).isEqualTo("Systems Engineering");
    assertThat(csA.withinLimit()).isTrue();

    ProgramCreditSummaryView csB = result.creditSummary().stream()
        .filter(cs -> PROGRAM_B.equals(cs.programId())).findFirst().orElseThrow();
    assertThat(csB.selectedCredits()).isEqualTo(0.0);
    assertThat(csB.creditLimit()).isEqualTo(18);
  }

  // -------------------------------------------------------------------------
  // 2 selections same program → selectedCredits summed REAL
  // -------------------------------------------------------------------------

  @Test
  void getSimulation_twoSelectionsOneProgram_creditsSummedReal() {
    UUID simId = UUID.randomUUID();
    UUID offeringId1 = UUID.randomUUID();
    UUID offeringId2 = UUID.randomUUID();
    UUID courseId1 = UUID.randomUUID();
    UUID courseId2 = UUID.randomUUID();
    UUID selId1 = UUID.randomUUID();
    UUID selId2 = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId, List.of(PROGRAM_A));

    SimulationSelection sel1 = makeSelection(selId1, simId, offeringId1, courseId1, PROGRAM_A, 3.0, 1L, List.of());
    SimulationSelection sel2 = makeSelection(selId2, simId, offeringId2, courseId2, PROGRAM_A, 5.0, 1L, List.of());

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId))
        .thenReturn(List.of(sel1, sel2));
    when(offeringReadPort.resolvePeriodName(PERIOD_ID)).thenReturn("2026-2");
    when(offeringReadPort.currentOfferingVersion(offeringId1)).thenReturn(1L);
    when(offeringReadPort.currentOfferingVersion(offeringId2)).thenReturn(1L);
    when(creditLimitPort.creditLimit(PROGRAM_A, PERIOD_ID)).thenReturn(23);
    when(programNameReadPort.programName(PROGRAM_A)).thenReturn("Systems Engineering");
    when(progressReadPort.passedCourseIds(eq(OWNER_ID), eq(PROGRAM_A))).thenReturn(Set.of());
    when(curriculumReadPort.requirementsForCourse(any())).thenReturn(List.of());
    when(curriculumReadPort.sequentialPredecessor(any(), any())).thenReturn(null);

    SimulationDetailView result = sut.getSimulation(simId, OWNER_ID);

    assertThat(result.creditSummary()).hasSize(1);
    ProgramCreditSummaryView cs = result.creditSummary().getFirst();
    assertThat(cs.selectedCredits()).isEqualTo(8.0); // 3.0 + 5.0
    assertThat(cs.creditLimit()).isEqualTo(23);
    assertThat(cs.withinLimit()).isTrue();
    assertThat(cs.programName()).isEqualTo("Systems Engineering");
    assertThat(result.statusName()).isEqualTo("CURRENT");
  }

  // -------------------------------------------------------------------------
  // Offering version bumped → OUTDATED
  // -------------------------------------------------------------------------

  @Test
  void getSimulation_offeringVersionBumped_returnsOutdated() {
    UUID simId = UUID.randomUUID();
    UUID offeringId = UUID.randomUUID();
    UUID selId = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId, List.of(PROGRAM_A));
    // Selection snapshot version = 1, but current = 2
    SimulationSelection sel = makeSelection(selId, simId, offeringId, UUID.randomUUID(),
        PROGRAM_A, 3.0, 1L, List.of());

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId)).thenReturn(List.of(sel));
    when(offeringReadPort.resolvePeriodName(PERIOD_ID)).thenReturn("2026-2");
    when(offeringReadPort.currentOfferingVersion(offeringId)).thenReturn(2L); // bumped!
    when(creditLimitPort.creditLimit(PROGRAM_A, PERIOD_ID)).thenReturn(23);
    when(programNameReadPort.programName(PROGRAM_A)).thenReturn("Systems Engineering");
    when(progressReadPort.passedCourseIds(eq(OWNER_ID), eq(PROGRAM_A))).thenReturn(Set.of());
    when(curriculumReadPort.requirementsForCourse(any())).thenReturn(List.of());
    when(curriculumReadPort.sequentialPredecessor(any(), any())).thenReturn(null);

    SimulationDetailView result = sut.getSimulation(simId, OWNER_ID);

    assertThat(result.statusName()).isEqualTo("OUTDATED");
  }

  // -------------------------------------------------------------------------
  // Non-owner → 403
  // -------------------------------------------------------------------------

  @Test
  void getSimulation_nonOwner_throwsAccessDenied() {
    UUID simId = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId, List.of(PROGRAM_A));

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));

    assertThatThrownBy(() -> sut.getSimulation(simId, OTHER_ID))
        .isInstanceOf(SimulationAccessDeniedException.class);
  }

  // -------------------------------------------------------------------------
  // Not found → 404
  // -------------------------------------------------------------------------

  @Test
  void getSimulation_notFound_throwsNotFoundException() {
    UUID simId = UUID.randomUUID();
    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.getSimulation(simId, OWNER_ID))
        .isInstanceOf(SimulationNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // Weekly schedule: two overlapping sessions → both inClash=true
  // -------------------------------------------------------------------------

  @Test
  void getSimulation_twoOverlappingSessions_weeklyScheduleInClashTrue() {
    UUID simId = UUID.randomUUID();
    UUID offeringId1 = UUID.randomUUID();
    UUID offeringId2 = UUID.randomUUID();
    UUID courseId1 = UUID.randomUUID();
    UUID courseId2 = UUID.randomUUID();
    UUID selId1 = UUID.randomUUID();
    UUID selId2 = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId, List.of(PROGRAM_A));

    // Both on WEDNESDAY 18:00-20:00 → clash
    SimScheduleSession clashSession = new SimScheduleSession("WEDNESDAY",
        LocalTime.of(18, 0), LocalTime.of(20, 0));

    SimulationSelection sel1 = makeSelection(selId1, simId, offeringId1, courseId1, PROGRAM_A,
        3.0, 1L, List.of(clashSession));
    SimulationSelection sel2 = makeSelection(selId2, simId, offeringId2, courseId2, PROGRAM_A,
        3.0, 1L, List.of(clashSession));

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId))
        .thenReturn(List.of(sel1, sel2));
    when(offeringReadPort.resolvePeriodName(PERIOD_ID)).thenReturn("2026-2");
    when(offeringReadPort.currentOfferingVersion(any())).thenReturn(1L);
    when(creditLimitPort.creditLimit(PROGRAM_A, PERIOD_ID)).thenReturn(23);
    when(programNameReadPort.programName(PROGRAM_A)).thenReturn("Systems Engineering");
    when(progressReadPort.passedCourseIds(eq(OWNER_ID), eq(PROGRAM_A))).thenReturn(Set.of());
    when(curriculumReadPort.requirementsForCourse(any())).thenReturn(List.of());
    when(curriculumReadPort.sequentialPredecessor(any(), any())).thenReturn(null);

    SimulationDetailView result = sut.getSimulation(simId, OWNER_ID);

    assertThat(result.weeklySchedule()).hasSize(2);
    assertThat(result.weeklySchedule()).allMatch(e -> e.inClash());
  }

  // -------------------------------------------------------------------------
  // Validation snapshot: no selections → empty blocking[] and empty warnings[]
  // (FIX F4: renamed from misleading getSimulation_nowUnmetPrereq_validationSnapshotHasBlocker)
  // -------------------------------------------------------------------------

  @Test
  void getSimulation_noSelections_validationSnapshotEmpty() {
    UUID simId = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId, List.of(PROGRAM_A));

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId)).thenReturn(List.of());
    when(offeringReadPort.resolvePeriodName(PERIOD_ID)).thenReturn("2026-2");
    when(creditLimitPort.creditLimit(PROGRAM_A, PERIOD_ID)).thenReturn(23);
    when(programNameReadPort.programName(PROGRAM_A)).thenReturn("Systems Engineering");

    SimulationDetailView result = sut.getSimulation(simId, OWNER_ID);

    assertThat(result.validation().blocking()).isEmpty();
    assertThat(result.validation().warnings()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Validation snapshot: unmet HIGH prereq → blocking[] contains SIMULATION_PREREQUISITE_NOT_MET
  // (FIX F4: NEW test — proves DET-003 read-time validation actually surfaces blockers)
  // -------------------------------------------------------------------------

  @Test
  void getSimulation_unmetHardPrereq_validationSnapshotHasBlocker() {
    UUID simId = UUID.randomUUID();
    UUID offeringId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID prereqCourseId = UUID.randomUUID();
    UUID selId = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId, List.of(PROGRAM_A));

    SimulationSelection sel = makeSelection(
        selId, simId, offeringId, courseId, PROGRAM_A, 3.0, 1L, List.of());

    // Prereq: courseId requires prereqCourseId with HIGH confidence (isHard() == true)
    PrereqRequirement hardPrereq = new PrereqRequirement(
        courseId, prereqCourseId, "COURSE_PASSED", "HIGH", false, "Required Course");

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId)).thenReturn(List.of(sel));
    when(offeringReadPort.resolvePeriodName(PERIOD_ID)).thenReturn("2026-2");
    when(offeringReadPort.currentOfferingVersion(offeringId)).thenReturn(1L);
    when(creditLimitPort.creditLimit(PROGRAM_A, PERIOD_ID)).thenReturn(23);
    when(programNameReadPort.programName(PROGRAM_A)).thenReturn("Systems Engineering");
    // Student has NOT passed the prereq course → empty passedCourseIds
    when(progressReadPort.passedCourseIds(eq(OWNER_ID), eq(PROGRAM_A))).thenReturn(Set.of());
    when(curriculumReadPort.requirementsForCourse(courseId)).thenReturn(List.of(hardPrereq));
    when(curriculumReadPort.sequentialPredecessor(any(), any())).thenReturn(null);

    SimulationDetailView result = sut.getSimulation(simId, OWNER_ID);

    // DET-003: read-time validation must surface the unmet HARD prereq as a blocking notice
    assertThat(result.validation().blocking())
        .as("Unmet HARD prereq must appear in blocking[] (DET-003)")
        .isNotEmpty();
    assertThat(result.validation().blocking().getFirst().code())
        .isEqualTo("SIMULATION_PREREQUISITE_NOT_MET");
  }

  // -------------------------------------------------------------------------
  // FIX F2: missing/deleted offering → conservative OUTDATED (not CURRENT, not 500)
  // -------------------------------------------------------------------------

  @Test
  void getSimulation_offeringVersionThrows_returnsOutdatedConservatively() {
    UUID simId = UUID.randomUUID();
    UUID offeringId = UUID.randomUUID();
    UUID selId = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId, List.of(PROGRAM_A));
    // Selection with snapshot version 1
    SimulationSelection sel = makeSelection(
        selId, simId, offeringId, UUID.randomUUID(), PROGRAM_A, 3.0, 1L, List.of());

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId)).thenReturn(List.of(sel));
    when(offeringReadPort.resolvePeriodName(PERIOD_ID)).thenReturn("2026-2");
    // Simulate deleted/missing offering: currentOfferingVersion throws
    when(offeringReadPort.currentOfferingVersion(offeringId))
        .thenThrow(new RuntimeException("Offering not found: " + offeringId));
    when(creditLimitPort.creditLimit(PROGRAM_A, PERIOD_ID)).thenReturn(23);
    when(programNameReadPort.programName(PROGRAM_A)).thenReturn("Systems Engineering");
    when(progressReadPort.passedCourseIds(eq(OWNER_ID), eq(PROGRAM_A))).thenReturn(Set.of());
    when(curriculumReadPort.requirementsForCourse(any())).thenReturn(List.of());
    when(curriculumReadPort.sequentialPredecessor(any(), any())).thenReturn(null);

    // Must NOT throw — a valid simulation with a missing offering must still render
    SimulationDetailView result = sut.getSimulation(simId, OWNER_ID);

    // Conservative: unresolvable version == cannot validate snapshot == OUTDATED
    assertThat(result.statusName())
        .as("Missing/deleted offering: snapshot cannot be validated, must report OUTDATED (FIX F2)")
        .isEqualTo("OUTDATED");
  }
}
