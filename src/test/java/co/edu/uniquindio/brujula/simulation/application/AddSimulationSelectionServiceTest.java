package co.edu.uniquindio.brujula.simulation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import co.edu.uniquindio.brujula.simulation.application.dto.AddSelectionCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.ScheduleOptionResolutionView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationSelectionView;
import co.edu.uniquindio.brujula.simulation.application.port.out.CreditLimitPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgressReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgramNameReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.service.AddSimulationSelectionService;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationValidationBlockedException;
import co.edu.uniquindio.brujula.simulation.domain.ValidationNotice;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU4a application unit tests for {@link AddSimulationSelectionService}.
 *
 * <p>Covers: happy path (201), idempotency replay, hash conflict → 409, non-owner → 403,
 * simulation not found → 404, blocking validation → SimulationValidationBlockedException.
 */
class AddSimulationSelectionServiceTest {

  private LoadSimulationPort loadSimulationPort;
  private LoadSimulationSelectionPort loadSimulationSelectionPort;
  private SaveSimulationSelectionPort saveSimulationSelectionPort;
  private OfferingReadPort offeringReadPort;
  private CreditLimitPort creditLimitPort;
  private ProgressReadPort progressReadPort;
  private CurriculumReadPort curriculumReadPort;
  private ProgramNameReadPort programNameReadPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AddSimulationSelectionService sut;

  private final UUID ownerId = UUID.randomUUID();
  private final UUID simulationId = UUID.randomUUID();
  private final UUID scheduleOptionId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID courseId = UUID.randomUUID();

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
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);

    sut = new AddSimulationSelectionService(
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

  // -------------------------------------------------------------------------
  // Happy path
  // -------------------------------------------------------------------------

  @Test
  void addSelection_clean_returnsView() {
    UUID idempotencyKey = UUID.randomUUID();
    String requestHash = "hash123";
    AddSelectionCommand command = new AddSelectionCommand(
        idempotencyKey, simulationId, ownerId, scheduleOptionId, false, requestHash);

    Simulation simulation = buildSimulation();
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));
    when(idempotencyStorePort.find(idempotencyKey, AddSimulationSelectionService.OPERATION))
        .thenReturn(Optional.empty());
    when(offeringReadPort.resolveScheduleOption(scheduleOptionId))
        .thenReturn(buildResolutionView());
    when(curriculumReadPort.courseCredits(courseId)).thenReturn(3);
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simulationId))
        .thenReturn(List.of());
    when(progressReadPort.passedCourseIds(ownerId, programId)).thenReturn(Set.of());
    when(curriculumReadPort.requirementsForCourse(courseId)).thenReturn(List.of());
    when(creditLimitPort.creditLimit(programId, periodId)).thenReturn(30);
    when(programNameReadPort.programName(programId)).thenReturn("Systems Engineering");
    when(saveSimulationSelectionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

    SimulationSelectionView view = sut.addSelection(command);

    assertThat(view).isNotNull();
    assertThat(view.courseId()).isEqualTo(courseId);
    verify(saveSimulationSelectionPort).save(any(SimulationSelection.class));
    verify(idempotencyStorePort).save(
        eq(idempotencyKey), eq(AddSimulationSelectionService.OPERATION),
        eq(requestHash), eq(201), any());
  }

  // -------------------------------------------------------------------------
  // Idempotency replay
  // -------------------------------------------------------------------------

  @Test
  void addSelection_sameKeyAndHash_replaysStoredResult() {
    UUID idempotencyKey = UUID.randomUUID();
    String requestHash = "same-hash";
    AddSelectionCommand command = new AddSelectionCommand(
        idempotencyKey, simulationId, ownerId, scheduleOptionId, false, requestHash);

    SimulationSelectionView storedView = buildMinimalView();
    IdempotencyStorePort.StoredRecord stored =
        new IdempotencyStorePort.StoredRecord(requestHash, 201, "{\"id\":\"...\"}");
    when(idempotencyStorePort.find(idempotencyKey, AddSimulationSelectionService.OPERATION))
        .thenReturn(Optional.of(stored));
    when(jsonCodecPort.fromJson("{\"id\":\"...\"}", SimulationSelectionView.class))
        .thenReturn(storedView);

    SimulationSelectionView view = sut.addSelection(command);

    assertThat(view).isSameAs(storedView);
    verify(loadSimulationPort, never()).loadById(any());
    verify(saveSimulationSelectionPort, never()).save(any());
  }

  @Test
  void addSelection_sameKeyDifferentHash_throwsConflict() {
    UUID idempotencyKey = UUID.randomUUID();
    AddSelectionCommand command = new AddSelectionCommand(
        idempotencyKey, simulationId, ownerId, scheduleOptionId, false, "new-hash");

    IdempotencyStorePort.StoredRecord stored =
        new IdempotencyStorePort.StoredRecord("original-hash", 201, "{}");
    when(idempotencyStorePort.find(idempotencyKey, AddSimulationSelectionService.OPERATION))
        .thenReturn(Optional.of(stored));

    assertThatThrownBy(() -> sut.addSelection(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);
  }

  // -------------------------------------------------------------------------
  // Authorization
  // -------------------------------------------------------------------------

  @Test
  void addSelection_nonOwner_throws403() {
    UUID idempotencyKey = UUID.randomUUID();
    UUID intruder = UUID.randomUUID();
    AddSelectionCommand command = new AddSelectionCommand(
        idempotencyKey, simulationId, intruder, scheduleOptionId, false, "h");

    Simulation simulation = buildSimulation(); // ownerId is the real owner, not intruder
    when(idempotencyStorePort.find(idempotencyKey, AddSimulationSelectionService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));

    assertThatThrownBy(() -> sut.addSelection(command))
        .isInstanceOf(SimulationAccessDeniedException.class);

    verify(saveSimulationSelectionPort, never()).save(any());
  }

  @Test
  void addSelection_simulationNotFound_throws404() {
    UUID idempotencyKey = UUID.randomUUID();
    AddSelectionCommand command = new AddSelectionCommand(
        idempotencyKey, simulationId, ownerId, scheduleOptionId, false, "h");

    when(idempotencyStorePort.find(idempotencyKey, AddSimulationSelectionService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.addSelection(command))
        .isInstanceOf(SimulationNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // Blocking validation
  // -------------------------------------------------------------------------

  @Test
  void addSelection_alreadyPassed_throwsBlockedException() {
    UUID idempotencyKey = UUID.randomUUID();
    AddSelectionCommand command = new AddSelectionCommand(
        idempotencyKey, simulationId, ownerId, scheduleOptionId, false, "h");

    Simulation simulation = buildSimulation();
    when(idempotencyStorePort.find(idempotencyKey, AddSimulationSelectionService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));
    when(offeringReadPort.resolveScheduleOption(scheduleOptionId))
        .thenReturn(buildResolutionView());
    when(curriculumReadPort.courseCredits(courseId)).thenReturn(3);
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simulationId))
        .thenReturn(List.of());
    // Student has already passed the course
    when(progressReadPort.passedCourseIds(ownerId, programId)).thenReturn(Set.of(courseId));
    when(curriculumReadPort.requirementsForCourse(courseId)).thenReturn(List.of());
    when(creditLimitPort.creditLimit(programId, periodId)).thenReturn(30);
    when(programNameReadPort.programName(programId)).thenReturn("Systems Engineering");

    assertThatThrownBy(() -> sut.addSelection(command))
        .isInstanceOf(SimulationValidationBlockedException.class)
        .satisfies(ex -> {
          SimulationValidationBlockedException blocked = (SimulationValidationBlockedException) ex;
          assertThat(blocked.primaryNotice().code()).isEqualTo("SIMULATION_COURSE_ALREADY_PASSED");
        });

    verify(saveSimulationSelectionPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // Credit-limit rule — RED→GREEN proof (CRITICAL-1 fix)
  // -------------------------------------------------------------------------

  /**
   * RED→GREEN proof: candidate course has 3 credits; existing selections already sum to 21
   * for the same program; credit limit is 23. Attempting to add 3 more (21+3=24) exceeds
   * the limit → SIMULATION_CREDIT_LIMIT_EXCEEDED must be thrown.
   *
   * <p>Before CRITICAL-1 fix this test would FAIL (credits=0 → 21+0=21 ≤ 23 → no block).
   * After fix it PASSES because real credits (3) are used.
   */
  @Test
  void addSelection_creditLimitExceeded_throwsBlockedException() {
    UUID idempotencyKey = UUID.randomUUID();
    AddSelectionCommand command = new AddSelectionCommand(
        idempotencyKey, simulationId, ownerId, scheduleOptionId, false, "h");

    // Existing selections: two courses of 10 and 11 credits = 21 total for the same program
    UUID existingCourseId1 = UUID.randomUUID();
    UUID existingCourseId2 = UUID.randomUUID();
    SimulationSelection sel1 = buildPersistedSelection(existingCourseId1, programId, 10.0);
    SimulationSelection sel2 = buildPersistedSelection(existingCourseId2, programId, 11.0);

    Simulation simulation = buildSimulation();
    when(idempotencyStorePort.find(idempotencyKey, AddSimulationSelectionService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));
    when(offeringReadPort.resolveScheduleOption(scheduleOptionId))
        .thenReturn(buildResolutionView());
    // CRITICAL: courseCredits returns 3 (real value) — not 0
    when(curriculumReadPort.courseCredits(courseId)).thenReturn(3);
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simulationId))
        .thenReturn(List.of(sel1, sel2));
    when(progressReadPort.passedCourseIds(ownerId, programId)).thenReturn(Set.of());
    when(curriculumReadPort.requirementsForCourse(courseId)).thenReturn(List.of());
    // Credit limit = 23; existing 21 + candidate 3 = 24 → EXCEEDS limit
    when(creditLimitPort.creditLimit(programId, periodId)).thenReturn(23);
    when(programNameReadPort.programName(programId)).thenReturn("Ingeniería de Sistemas");

    assertThatThrownBy(() -> sut.addSelection(command))
        .isInstanceOf(SimulationValidationBlockedException.class)
        .satisfies(ex -> {
          SimulationValidationBlockedException blocked = (SimulationValidationBlockedException) ex;
          assertThat(blocked.primaryNotice().code()).isEqualTo("SIMULATION_CREDIT_LIMIT_EXCEEDED");
          assertThat(blocked.primaryNotice().metadata()).containsEntry("programName",
              "Ingeniería de Sistemas");
          assertThat((Integer) blocked.primaryNotice().metadata().get("creditLimit")).isEqualTo(23);
          assertThat((Double) blocked.primaryNotice().metadata().get("attemptedCredits"))
              .isEqualTo(24.0);
        });

    verify(saveSimulationSelectionPort, never()).save(any());
  }

  /**
   * Boundary case: existing credits = 20, candidate = 3, limit = 23. Exactly at limit → ALLOWED
   * (no exception). Spec rule: exactly-at-limit is not exceeded.
   */
  @Test
  void addSelection_creditLimitExactlyAtLimit_allowed() {
    UUID idempotencyKey = UUID.randomUUID();
    AddSelectionCommand command = new AddSelectionCommand(
        idempotencyKey, simulationId, ownerId, scheduleOptionId, false, "h2");

    SimulationSelection sel1 = buildPersistedSelection(UUID.randomUUID(), programId, 20.0);

    Simulation simulation = buildSimulation();
    when(idempotencyStorePort.find(idempotencyKey, AddSimulationSelectionService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));
    when(offeringReadPort.resolveScheduleOption(scheduleOptionId))
        .thenReturn(buildResolutionView());
    when(curriculumReadPort.courseCredits(courseId)).thenReturn(3);
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simulationId))
        .thenReturn(List.of(sel1));
    when(progressReadPort.passedCourseIds(ownerId, programId)).thenReturn(Set.of());
    when(curriculumReadPort.requirementsForCourse(courseId)).thenReturn(List.of());
    // 20 + 3 = 23 = limit → exactly-at-limit → ALLOWED
    when(creditLimitPort.creditLimit(programId, periodId)).thenReturn(23);
    when(programNameReadPort.programName(programId)).thenReturn("Ingeniería de Sistemas");
    when(saveSimulationSelectionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Should NOT throw — exactly at limit is allowed
    SimulationSelectionView view = sut.addSelection(command);
    assertThat(view).isNotNull();
    verify(saveSimulationSelectionPort).save(any(SimulationSelection.class));
  }

  /**
   * Asserts that when the credit limit is exceeded, the metadata.programName contains
   * the real program name resolved via ProgramNameReadPort (CRITICAL-2 fix).
   */
  @Test
  void addSelection_creditLimitExceeded_metadataContainsRealProgramName() {
    UUID idempotencyKey = UUID.randomUUID();
    AddSelectionCommand command = new AddSelectionCommand(
        idempotencyKey, simulationId, ownerId, scheduleOptionId, false, "h3");

    SimulationSelection sel1 = buildPersistedSelection(UUID.randomUUID(), programId, 21.0);

    Simulation simulation = buildSimulation();
    when(idempotencyStorePort.find(idempotencyKey, AddSimulationSelectionService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSimulationPort.loadById(simulationId)).thenReturn(Optional.of(simulation));
    when(offeringReadPort.resolveScheduleOption(scheduleOptionId))
        .thenReturn(buildResolutionView());
    when(curriculumReadPort.courseCredits(courseId)).thenReturn(3);
    when(loadSimulationSelectionPort.loadActiveBySimulationId(simulationId))
        .thenReturn(List.of(sel1));
    when(progressReadPort.passedCourseIds(ownerId, programId)).thenReturn(Set.of());
    when(curriculumReadPort.requirementsForCourse(courseId)).thenReturn(List.of());
    when(creditLimitPort.creditLimit(programId, periodId)).thenReturn(23);
    // Real program name — NOT the placeholder "Program"
    when(programNameReadPort.programName(programId)).thenReturn("Industrial Engineering");

    assertThatThrownBy(() -> sut.addSelection(command))
        .isInstanceOf(SimulationValidationBlockedException.class)
        .satisfies(ex -> {
          SimulationValidationBlockedException blocked = (SimulationValidationBlockedException) ex;
          assertThat(blocked.primaryNotice().metadata())
              .containsEntry("programName", "Industrial Engineering");
        });
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private SimulationSelection buildPersistedSelection(UUID existingCourseId,
      UUID programIdForSelection, double credits) {
    return SimulationSelection.reconstitute(
        SimulationSelectionId.generate(),
        simulationId,
        UUID.randomUUID(), // offeringId
        UUID.randomUUID(), // scheduleOptionId
        existingCourseId,
        "Some Course",
        programIdForSelection,
        credits,
        "A",
        "PUBLISHED",
        1L,
        List.of(),
        "ACTIVE",
        0L);
  }

  private Simulation buildSimulation() {
    return Simulation.reconstitute(
        SimulationId.of(simulationId),
        ownerId,
        "Test Sim",
        periodId,
        List.of(programId),
        SimulationLifecycle.ACTIVE,
        Instant.now(),
        Instant.now(),
        0L);
  }

  private ScheduleOptionResolutionView buildResolutionView() {
    ScheduleOptionResolutionView.SessionSlot slot =
        new ScheduleOptionResolutionView.SessionSlot(
            "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0));
    return new ScheduleOptionResolutionView(
        scheduleOptionId,
        UUID.randomUUID(), // offeringId
        courseId,
        "Introduction to Algorithms",
        programId,
        periodId,
        "A",
        3.0,
        "PUBLISHED",
        1L,
        List.of(slot));
  }

  private SimulationSelectionView buildMinimalView() {
    return new SimulationSelectionView(
        UUID.randomUUID(),
        UUID.randomUUID(),
        scheduleOptionId,
        courseId,
        "Introduction to Algorithms",
        programId,
        3.0,
        "A",
        "PUBLISHED",
        1L,
        List.of(),
        List.of());
  }
}
