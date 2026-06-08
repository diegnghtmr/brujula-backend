package co.edu.uniquindio.brujula.simulation.application.service;

import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import co.edu.uniquindio.brujula.simulation.application.dto.AddSelectionCommand;
import co.edu.uniquindio.brujula.simulation.application.dto.ScheduleOptionResolutionView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationSelectionView;
import co.edu.uniquindio.brujula.simulation.application.port.in.AddSimulationSelectionUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.out.CreditLimitPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgressReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgramNameReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.SaveSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.domain.CandidateSelection;
import co.edu.uniquindio.brujula.simulation.domain.ExistingSelection;
import co.edu.uniquindio.brujula.simulation.domain.PassedCourseSet;
import co.edu.uniquindio.brujula.simulation.domain.PrereqRequirement;
import co.edu.uniquindio.brujula.simulation.domain.ProgramCreditLimit;
import co.edu.uniquindio.brujula.simulation.domain.SequentialPredecessor;
import co.edu.uniquindio.brujula.simulation.domain.SimScheduleSession;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import co.edu.uniquindio.brujula.simulation.domain.SimulationValidationBlockedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationValidationResult;
import co.edu.uniquindio.brujula.simulation.domain.SimulationValidationService;
import co.edu.uniquindio.brujula.simulation.domain.ValidationNotice;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Application service: add a schedule option to a simulation (spec VAL-001, WU4a).
 *
 * <p>Flow:
 * <ol>
 *   <li>Idempotency check — replay stored response if same key+hash.
 *   <li>Load simulation — verify ownership (403 if non-owner).
 *   <li>Resolve schedule option via {@link OfferingReadPort#resolveScheduleOption}.
 *   <li>Gather prerequisite requirements, passed courses, credit limit.
 *   <li>Map existing selections to sim-local {@link ExistingSelection} records.
 *   <li>Run {@link SimulationValidationService#validate} — if blocked → throw exception.
 *   <li>Persist new {@link SimulationSelection} snapshot.
 *   <li>Store idempotency record and return view.
 * </ol>
 *
 * <p>Framework-free — wired by {@code SimulationConfiguration}.
 */
public class AddSimulationSelectionService implements AddSimulationSelectionUseCase {

  public static final String OPERATION = "ADD_SIMULATION_SELECTION";

  private final LoadSimulationPort loadSimulationPort;
  private final LoadSimulationSelectionPort loadSimulationSelectionPort;
  private final SaveSimulationSelectionPort saveSimulationSelectionPort;
  private final OfferingReadPort offeringReadPort;
  private final CreditLimitPort creditLimitPort;
  private final ProgressReadPort progressReadPort;
  private final CurriculumReadPort curriculumReadPort;
  private final ProgramNameReadPort programNameReadPort;
  private final SimulationValidationService validationService;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;

  public AddSimulationSelectionService(
      LoadSimulationPort loadSimulationPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort,
      SaveSimulationSelectionPort saveSimulationSelectionPort,
      OfferingReadPort offeringReadPort,
      CreditLimitPort creditLimitPort,
      ProgressReadPort progressReadPort,
      CurriculumReadPort curriculumReadPort,
      ProgramNameReadPort programNameReadPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort) {
    this.loadSimulationPort = loadSimulationPort;
    this.loadSimulationSelectionPort = loadSimulationSelectionPort;
    this.saveSimulationSelectionPort = saveSimulationSelectionPort;
    this.offeringReadPort = offeringReadPort;
    this.creditLimitPort = creditLimitPort;
    this.progressReadPort = progressReadPort;
    this.curriculumReadPort = curriculumReadPort;
    this.programNameReadPort = programNameReadPort;
    this.validationService = new SimulationValidationService();
    this.idempotencyStorePort = idempotencyStorePort;
    this.jsonCodecPort = jsonCodecPort;
  }

  @Override
  public SimulationSelectionView addSelection(AddSelectionCommand command) {
    // 1. Idempotency check
    Optional<IdempotencyStorePort.StoredRecord> existing =
        idempotencyStorePort.find(command.idempotencyKey(), OPERATION);
    if (existing.isPresent()) {
      IdempotencyStorePort.StoredRecord stored = existing.get();
      if (!stored.requestHash().equals(command.requestHash())) {
        throw new IdempotencyKeyConflictException(command.idempotencyKey(), OPERATION);
      }
      return jsonCodecPort.fromJson(stored.responseBodyJson(), SimulationSelectionView.class);
    }

    // 2. Load simulation + ownership check
    Simulation simulation = loadSimulationPort.loadById(command.simulationId())
        .orElseThrow(() -> new SimulationNotFoundException(command.simulationId()));
    if (!simulation.getOwnerId().equals(command.ownerId())) {
      throw new SimulationAccessDeniedException(command.simulationId());
    }

    // 3. Resolve schedule option (offering::api via adapter)
    ScheduleOptionResolutionView resolved = offeringReadPort.resolveScheduleOption(command.scheduleOptionId());

    // 4. Resolve real credits from curriculum (VAL-004 — credit rule must fire with actual load)
    double credits = curriculumReadPort.courseCredits(resolved.courseId());

    // 5. Build candidate selection (map offering sessions → sim-local SimScheduleSession)
    List<SimScheduleSession> candidateSessions = resolved.sessions().stream()
        .map(s -> new SimScheduleSession(s.day(), s.startsAt(), s.endsAt()))
        .toList();

    CandidateSelection candidate = new CandidateSelection(
        resolved.scheduleOptionId(),
        resolved.offeringId(),
        resolved.courseId(),
        resolved.courseName(),
        resolved.programId(),
        credits,
        resolved.journeyCode(),
        resolved.offeringStatus(),
        resolved.offeringVersion(),
        candidateSessions);

    // 5. Gather existing selections for this simulation
    List<SimulationSelection> existingDomain =
        loadSimulationSelectionPort.loadActiveBySimulationId(command.simulationId());
    List<ExistingSelection> existingSelections = existingDomain.stream()
        .map(s -> new ExistingSelection(
            s.getId().value(),
            s.getCourseId(),
            s.getCourseName(),
            s.getProgramId(),
            s.getCredits(),
            s.getSessions()))
        .toList();

    // 6. Gather passed courses (union across all program subscriptions of the simulation)
    Set<UUID> allPassedIds = new HashSet<>();
    for (UUID programId : simulation.getProgramIds()) {
      allPassedIds.addAll(progressReadPort.passedCourseIds(command.ownerId(), programId));
    }
    PassedCourseSet passedCourseSet = new PassedCourseSet(allPassedIds);

    // 7. Gather prerequisites for the candidate course
    List<PrereqRequirement> requirements =
        curriculumReadPort.requirementsForCourse(resolved.courseId());

    // 8. Determine credit limit for the candidate's program (fail-loud — no silent fallback)
    int limit = creditLimitPort.creditLimit(resolved.programId(), simulation.getPeriodId());
    String resolvedProgramName = programNameReadPort.programName(resolved.programId());
    ProgramCreditLimit creditLimit =
        new ProgramCreditLimit(resolved.programId(), resolvedProgramName, limit);

    // 9. Resolve sequential-track predecessor info (WU4b — SIMULATION_SEQUENTIAL_ORDER)
    SequentialPredecessor sequentialPredecessor =
        curriculumReadPort.sequentialPredecessor(resolved.courseId(), null);

    // 10. Compute accumulated passed credits for MINIMUM_CREDITS rule (VAL-009 / WU4b)
    // Sum the real credits for each passed course via CurriculumReadPort.courseCredits().
    // This feeds MinCreditsRule with the actual credit total, not a course-count proxy.
    int accumulatedPassedCredits = allPassedIds.stream()
        .mapToInt(id -> curriculumReadPort.courseCredits(id))
        .sum();

    // 11. Run validation engine (WU4a + WU4b rules)
    SimulationValidationResult validationResult = validationService.validate(
        candidate, existingSelections, requirements, passedCourseSet, creditLimit,
        sequentialPredecessor, accumulatedPassedCredits);

    if (validationResult.hasBlockers()) {
      throw new SimulationValidationBlockedException(validationResult.blocking());
    }

    // 12. Persist selection with snapshot
    SimulationSelectionId selectionId = SimulationSelectionId.generate();
    SimulationSelection selection = SimulationSelection.create(
        selectionId, command.simulationId(), candidate);
    SimulationSelection saved = saveSimulationSelectionPort.save(selection);

    // 13. Build view + store idempotency record
    List<ValidationNotice> warnings = validationResult.warnings();
    SimulationSelectionView view = toView(saved, warnings);
    String responseJson = jsonCodecPort.toJson(view);
    idempotencyStorePort.save(
        command.idempotencyKey(), OPERATION, command.requestHash(), 201, responseJson);

    return view;
  }

  // -------------------------------------------------------------------------
  // View mapping
  // -------------------------------------------------------------------------

  private SimulationSelectionView toView(SimulationSelection selection,
      List<ValidationNotice> warnings) {
    List<SimulationSelectionView.SessionView> sessionViews = selection.getSessions().stream()
        .map(s -> new SimulationSelectionView.SessionView(
            s.day(), s.startsAt().toString(), s.endsAt().toString()))
        .toList();

    List<SimulationSelectionView.ValidationNoticeView> warningViews = warnings.stream()
        .map(n -> new SimulationSelectionView.ValidationNoticeView(n.code(), n.message(), n.metadata()))
        .toList();

    return new SimulationSelectionView(
        selection.getId().value(),
        selection.getOfferingId(),
        selection.getScheduleOptionId(),
        selection.getCourseId(),
        selection.getCourseName(),
        selection.getProgramId(),
        selection.getCredits(),
        selection.getJourneyCode(),
        selection.getOfferingStatus(),
        selection.getSelectedOfferingVersion(),
        sessionViews,
        warningViews);
  }
}
