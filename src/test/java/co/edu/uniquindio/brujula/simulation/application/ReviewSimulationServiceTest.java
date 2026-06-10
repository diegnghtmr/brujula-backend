package co.edu.uniquindio.brujula.simulation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.simulation.application.dto.ProgramCreditSummaryView;
import co.edu.uniquindio.brujula.simulation.application.dto.ScheduleOptionResolutionView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationDetailView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationValidationView;
import co.edu.uniquindio.brujula.simulation.application.port.in.GetSimulationQuery;
import co.edu.uniquindio.brujula.simulation.application.port.out.CreditLimitPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgramNameReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgressReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.service.ReviewSimulationService;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationStillBlockedException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ReviewSimulationService} (spec STALE-002).
 *
 * <p>Strict TDD — stubs only for output ports. No Spring context.
 */
class ReviewSimulationServiceTest {

  private LoadSimulationPort loadSimulationPort;
  private LoadSimulationSelectionPort loadSimulationSelectionPort;
  private SaveSimulationSelectionPort saveSimulationSelectionPort;
  private OfferingReadPort offeringReadPort;
  private CreditLimitPort creditLimitPort;
  private ProgressReadPort progressReadPort;
  private CurriculumReadPort curriculumReadPort;
  private ProgramNameReadPort programNameReadPort;
  private GetSimulationQuery getSimulationQuery;
  private ReviewSimulationService sut;

  private static final UUID OWNER_ID = UUID.randomUUID();
  private static final UUID OTHER_ID = UUID.randomUUID();
  private static final UUID PROGRAM_A = UUID.randomUUID();
  private static final UUID PERIOD_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSimulationPort = mock(LoadSimulationPort.class);
    loadSimulationSelectionPort = mock(LoadSimulationSelectionPort.class);
    saveSimulationSelectionPort = mock(SaveSimulationSelectionPort.class);
    offeringReadPort = mock(OfferingReadPort.class);
    creditLimitPort = mock(CreditLimitPort.class);
    progressReadPort = mock(ProgressReadPort.class);
    curriculumReadPort = mock(CurriculumReadPort.class);
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

  // -------------------------------------------------------------------------
  // Helper builders
  // -------------------------------------------------------------------------

  private Simulation makeSimulation(UUID simId) {
    return Simulation.reconstitute(
        new SimulationId(simId),
        OWNER_ID,
        "Test Sim",
        PERIOD_ID,
        List.of(PROGRAM_A),
        SimulationLifecycle.ACTIVE,
        Instant.now(),
        Instant.now(),
        0L);
  }

  private SimulationSelection makeSelection(
      UUID simId, UUID offeringId, UUID schedOptId, UUID courseId, long selectedVersion) {
    return SimulationSelection.reconstitute(
        new SimulationSelectionId(UUID.randomUUID()),
        simId,
        offeringId,
        schedOptId,
        courseId,
        "Test Course",
        PROGRAM_A,
        3.0,
        "DIURNA",
        "PRELIMINARY",
        selectedVersion,
        List.of(),
        "ACTIVE",
        0L);
  }

  private ScheduleOptionResolutionView makeResolution(
      UUID schedOptId, UUID offeringId, UUID courseId, long newVersion) {
    return new ScheduleOptionResolutionView(
        schedOptId,
        offeringId,
        courseId,
        "Test Course",
        PROGRAM_A,
        PERIOD_ID,
        "DIURNA",
        0.0,
        "PRELIMINARY",
        newVersion,
        List.of());
  }

  private SimulationDetailView makeCurrentDetail(UUID simId) {
    return new SimulationDetailView(
        simId,
        "Test Sim",
        PERIOD_ID,
        "2026-2",
        List.of(PROGRAM_A),
        "CURRENT",
        Instant.now(),
        Instant.now(),
        1L,
        List.of(),
        List.of(new ProgramCreditSummaryView(PROGRAM_A, "Systems Eng", 0.0, 23, true)),
        List.of(),
        0.0,
        new SimulationValidationView(List.of(), List.of()));
  }

  // -------------------------------------------------------------------------
  // Clean review → CURRENT + snapshots refreshed
  // -------------------------------------------------------------------------

  @Test
  void reviewSimulation_clean_returnsCurrent() {
    UUID simId = UUID.randomUUID();
    UUID offeringId = UUID.randomUUID();
    UUID schedOptId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId);
    SimulationSelection sel = makeSelection(simId, offeringId, schedOptId, courseId, 1L);
    ScheduleOptionResolutionView resolved = makeResolution(schedOptId, offeringId, courseId, 2L);
    SimulationDetailView expectedDetail = makeCurrentDetail(simId);

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId)).thenReturn(List.of(sel));
    when(offeringReadPort.resolveScheduleOption(schedOptId)).thenReturn(resolved);
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_A)).thenReturn(Set.of());
    when(curriculumReadPort.requirementsForCourse(courseId)).thenReturn(List.of());
    when(curriculumReadPort.coursesCredits(any())).thenReturn(Map.of(courseId, 3));
    when(curriculumReadPort.sequentialPredecessor(eq(courseId), any())).thenReturn(null);
    when(creditLimitPort.creditLimit(PROGRAM_A, PERIOD_ID)).thenReturn(23);
    when(programNameReadPort.programName(PROGRAM_A)).thenReturn("Systems Eng");
    when(getSimulationQuery.getSimulation(simId, OWNER_ID)).thenReturn(expectedDetail);

    SimulationDetailView result = sut.reviewSimulation(simId, OWNER_ID);

    assertThat(result.statusName()).isEqualTo("CURRENT");
  }

  // -------------------------------------------------------------------------
  // Clean review → snapshots saved with refreshed version
  // -------------------------------------------------------------------------

  @Test
  void reviewSimulation_clean_snapshotsRefreshedAndSaved() {
    UUID simId = UUID.randomUUID();
    UUID offeringId = UUID.randomUUID();
    UUID schedOptId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId);
    SimulationSelection sel = makeSelection(simId, offeringId, schedOptId, courseId, 1L);
    long newVersion = 5L;
    ScheduleOptionResolutionView resolved =
        makeResolution(schedOptId, offeringId, courseId, newVersion);
    SimulationDetailView expectedDetail = makeCurrentDetail(simId);

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId)).thenReturn(List.of(sel));
    when(offeringReadPort.resolveScheduleOption(schedOptId)).thenReturn(resolved);
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_A)).thenReturn(Set.of());
    when(curriculumReadPort.requirementsForCourse(courseId)).thenReturn(List.of());
    when(curriculumReadPort.coursesCredits(any())).thenReturn(Map.of(courseId, 3));
    when(curriculumReadPort.sequentialPredecessor(eq(courseId), any())).thenReturn(null);
    when(creditLimitPort.creditLimit(PROGRAM_A, PERIOD_ID)).thenReturn(23);
    when(programNameReadPort.programName(PROGRAM_A)).thenReturn("Systems Eng");
    when(getSimulationQuery.getSimulation(simId, OWNER_ID)).thenReturn(expectedDetail);

    sut.reviewSimulation(simId, OWNER_ID);

    // Verify save was called with the refreshed snapshot (new version = 5)
    verify(saveSimulationSelectionPort)
        .save(argThat(s -> s.getSelectedOfferingVersion() == newVersion));
  }

  // -------------------------------------------------------------------------
  // Persistent blocker → SimulationStillBlockedException (409)
  // -------------------------------------------------------------------------

  @Test
  void reviewSimulation_persistentBlocker_throwsStillBlockedException() {
    UUID simId = UUID.randomUUID();
    UUID offeringId = UUID.randomUUID();
    UUID schedOptId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID prereqCourseId = UUID.randomUUID(); // required prereq not passed
    Simulation simulation = makeSimulation(simId);
    SimulationSelection sel = makeSelection(simId, offeringId, schedOptId, courseId, 1L);
    ScheduleOptionResolutionView resolved = makeResolution(schedOptId, offeringId, courseId, 2L);

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId)).thenReturn(List.of(sel));
    when(offeringReadPort.resolveScheduleOption(schedOptId)).thenReturn(resolved);
    when(progressReadPort.passedCourseIds(OWNER_ID, PROGRAM_A)).thenReturn(Set.of());
    // Prereq requirement that the student has NOT passed
    when(curriculumReadPort.requirementsForCourse(courseId))
        .thenReturn(
            List.of(
                new co.edu.uniquindio.brujula.simulation.domain.PrereqRequirement(
                    courseId, prereqCourseId, "COURSE_PASSED", "HIGH", false, "Pre-course")));
    when(curriculumReadPort.coursesCredits(any())).thenReturn(Map.of(courseId, 3));
    when(curriculumReadPort.sequentialPredecessor(eq(courseId), any())).thenReturn(null);
    when(creditLimitPort.creditLimit(PROGRAM_A, PERIOD_ID)).thenReturn(23);
    when(programNameReadPort.programName(PROGRAM_A)).thenReturn("Systems Eng");

    assertThatThrownBy(() -> sut.reviewSimulation(simId, OWNER_ID))
        .isInstanceOf(SimulationStillBlockedException.class);

    // No save should happen when blocked
    verify(saveSimulationSelectionPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // Non-owner → 403
  // -------------------------------------------------------------------------

  @Test
  void reviewSimulation_nonOwner_throwsAccessDenied() {
    UUID simId = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId);

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));

    assertThatThrownBy(() -> sut.reviewSimulation(simId, OTHER_ID))
        .isInstanceOf(SimulationAccessDeniedException.class);
  }

  // -------------------------------------------------------------------------
  // Not found → 404
  // -------------------------------------------------------------------------

  @Test
  void reviewSimulation_notFound_throwsNotFoundException() {
    UUID simId = UUID.randomUUID();
    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.reviewSimulation(simId, OWNER_ID))
        .isInstanceOf(SimulationNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // No selections → empty detail returned immediately (no save)
  // -------------------------------------------------------------------------

  @Test
  void reviewSimulation_noSelections_returnsFreshDetailWithoutSaving() {
    UUID simId = UUID.randomUUID();
    Simulation simulation = makeSimulation(simId);
    SimulationDetailView expectedDetail = makeCurrentDetail(simId);

    when(loadSimulationPort.loadById(simId)).thenReturn(Optional.of(simulation));
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simId)).thenReturn(List.of());
    when(getSimulationQuery.getSimulation(simId, OWNER_ID)).thenReturn(expectedDetail);

    SimulationDetailView result = sut.reviewSimulation(simId, OWNER_ID);

    assertThat(result).isEqualTo(expectedDetail);
    verify(saveSimulationSelectionPort, never()).save(any());
  }
}
