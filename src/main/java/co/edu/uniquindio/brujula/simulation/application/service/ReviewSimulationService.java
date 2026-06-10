package co.edu.uniquindio.brujula.simulation.application.service;

import co.edu.uniquindio.brujula.simulation.application.dto.ScheduleOptionResolutionView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationDetailView;
import co.edu.uniquindio.brujula.simulation.application.port.in.GetSimulationQuery;
import co.edu.uniquindio.brujula.simulation.application.port.in.ReviewSimulationUseCase;
import co.edu.uniquindio.brujula.simulation.application.port.out.CreditLimitPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgramNameReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgressReadPort;
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
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationStillBlockedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationValidationResult;
import co.edu.uniquindio.brujula.simulation.domain.SimulationValidationService;
import co.edu.uniquindio.brujula.simulation.domain.ValidationNotice;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Application service: re-validate and optionally refresh a simulation (spec STALE-002).
 *
 * <p>Flow:
 *
 * <ol>
 *   <li>Load simulation + ownership check (403 if non-owner).
 *   <li>Load all active selections.
 *   <li>For each selection, re-resolve the current offering snapshot via {@link
 *       OfferingReadPort#resolveScheduleOption(UUID)}.
 *   <li>Re-run {@link SimulationValidationService} over the WHOLE simulation with refreshed data.
 *   <li>If blocking[] non-empty → throw {@link SimulationStillBlockedException} → HTTP 409.
 *   <li>Else: REFRESH each selection's {@code selectedOfferingVersion} snapshot to current + update
 *       sessions + persist. Bump {@code updatedAt} via the save operation.
 *   <li>Return refreshed {@link SimulationDetailView} with {@code statusName=CURRENT}.
 * </ol>
 *
 * <p>Framework-free — wired by {@code SimulationConfiguration}.
 */
public class ReviewSimulationService implements ReviewSimulationUseCase {

  private final LoadSimulationPort loadSimulationPort;
  private final LoadSimulationSelectionPort loadSimulationSelectionPort;
  private final SaveSimulationSelectionPort saveSimulationSelectionPort;
  private final OfferingReadPort offeringReadPort;
  private final CreditLimitPort creditLimitPort;
  private final ProgressReadPort progressReadPort;
  private final CurriculumReadPort curriculumReadPort;
  private final ProgramNameReadPort programNameReadPort;
  private final SimulationValidationService validationService;
  private final GetSimulationQuery getSimulationQuery;

  public ReviewSimulationService(
      LoadSimulationPort loadSimulationPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort,
      SaveSimulationSelectionPort saveSimulationSelectionPort,
      OfferingReadPort offeringReadPort,
      CreditLimitPort creditLimitPort,
      ProgressReadPort progressReadPort,
      CurriculumReadPort curriculumReadPort,
      ProgramNameReadPort programNameReadPort,
      GetSimulationQuery getSimulationQuery) {
    this.loadSimulationPort = loadSimulationPort;
    this.loadSimulationSelectionPort = loadSimulationSelectionPort;
    this.saveSimulationSelectionPort = saveSimulationSelectionPort;
    this.offeringReadPort = offeringReadPort;
    this.creditLimitPort = creditLimitPort;
    this.progressReadPort = progressReadPort;
    this.curriculumReadPort = curriculumReadPort;
    this.programNameReadPort = programNameReadPort;
    this.validationService = new SimulationValidationService();
    this.getSimulationQuery = getSimulationQuery;
  }

  @Override
  public SimulationDetailView reviewSimulation(UUID simulationId, UUID callerId) {
    // 1. Load simulation + ownership check
    Simulation simulation =
        loadSimulationPort
            .loadById(simulationId)
            .filter(s -> s.getLifecycleStatus() == SimulationLifecycle.ACTIVE)
            .orElseThrow(() -> new SimulationNotFoundException(simulationId));

    if (!simulation.getOwnerId().equals(callerId)) {
      throw new SimulationAccessDeniedException(simulationId);
    }

    // 2. Load active selections
    List<SimulationSelection> activeSelections =
        loadSimulationSelectionPort.loadActiveBySimulationId(simulationId);

    if (activeSelections.isEmpty()) {
      // No selections — nothing to validate or refresh. Return fresh detail.
      return getSimulationQuery.getSimulation(simulationId, callerId);
    }

    // 3. Re-resolve each selection's current offering snapshot
    record SelectionWithResolved(
        SimulationSelection selection, ScheduleOptionResolutionView resolved) {}
    List<SelectionWithResolved> selWithResolved = new ArrayList<>();
    for (SimulationSelection sel : activeSelections) {
      ScheduleOptionResolutionView resolved =
          offeringReadPort.resolveScheduleOption(sel.getScheduleOptionId());
      selWithResolved.add(new SelectionWithResolved(sel, resolved));
    }

    // 4. Gather passed courses for validation
    Set<UUID> allPassedIds = new HashSet<>();
    for (UUID programId : simulation.getProgramIds()) {
      try {
        allPassedIds.addAll(progressReadPort.passedCourseIds(callerId, programId));
      } catch (Exception ignored) {
        // if progress unavailable, conservative: assume no passed courses
      }
    }
    PassedCourseSet passedCourseSet = new PassedCourseSet(allPassedIds);

    // DEBT-F7-001 fix: batch-fetch all passed-course credits once before the selection loop.
    // Replaces the O(N×S) per-course loop (one courseCredits(UUID) call per passed course per
    // selection). coursesCredits(Set) issues a single round-trip; absent entries treated as 0.
    Map<UUID, Integer> passedCreditsMap = curriculumReadPort.coursesCredits(allPassedIds);
    int accumulatedPassedCredits =
        passedCreditsMap.values().stream().mapToInt(Integer::intValue).sum();

    // 5. Re-run validation for each selection with FRESH offering data
    List<ValidationNotice> allBlocking = new ArrayList<>();
    List<ValidationNotice> allWarnings = new ArrayList<>();

    for (SelectionWithResolved swr : selWithResolved) {
      SimulationSelection sel = swr.selection();
      ScheduleOptionResolutionView resolved = swr.resolved();

      // Build candidate from FRESH resolved data
      List<SimScheduleSession> freshSessions =
          resolved.sessions().stream()
              .map(s -> new SimScheduleSession(s.day(), s.startsAt(), s.endsAt()))
              .toList();

      CandidateSelection candidate =
          new CandidateSelection(
              resolved.scheduleOptionId(),
              resolved.offeringId(),
              resolved.courseId(),
              resolved.courseName(),
              resolved.programId(),
              curriculumReadPort.courseCredits(resolved.courseId()),
              resolved.journeyCode(),
              resolved.offeringStatus(),
              resolved.offeringVersion(),
              freshSessions);

      // Other selections (as ExistingSelection from their CURRENT snapshots)
      List<ExistingSelection> others =
          selWithResolved.stream()
              .filter(o -> !o.selection().getId().value().equals(sel.getId().value()))
              .map(
                  o ->
                      new ExistingSelection(
                          o.selection().getId().value(),
                          o.selection().getCourseId(),
                          o.selection().getCourseName(),
                          o.selection().getProgramId(),
                          o.selection().getCredits(),
                          o.selection().getSessions()))
              .toList();

      // Prerequisites
      List<PrereqRequirement> requirements;
      try {
        requirements = curriculumReadPort.requirementsForCourse(sel.getCourseId());
      } catch (Exception e) {
        requirements = List.of();
      }

      // Credit limit
      ProgramCreditLimit creditLimit;
      try {
        int limit = creditLimitPort.creditLimit(sel.getProgramId(), simulation.getPeriodId());
        String programName = programNameReadPort.programName(sel.getProgramId());
        creditLimit = new ProgramCreditLimit(sel.getProgramId(), programName, limit);
      } catch (Exception e) {
        creditLimit = null;
      }

      // Sequential predecessor
      SequentialPredecessor seqPredecessor;
      try {
        seqPredecessor = curriculumReadPort.sequentialPredecessor(sel.getCourseId(), null);
      } catch (Exception e) {
        seqPredecessor = null;
      }

      // Accumulated passed credits — uses pre-fetched batch map (DEBT-F7-001)

      SimulationValidationResult result =
          validationService.validate(
              candidate,
              others,
              requirements,
              passedCourseSet,
              creditLimit,
              seqPredecessor,
              accumulatedPassedCredits);

      allBlocking.addAll(result.blocking());
      allWarnings.addAll(result.warnings());
    }

    // 6. If blockers persist → 409 (STALE-002)
    if (!allBlocking.isEmpty()) {
      throw new SimulationStillBlockedException(allBlocking);
    }

    // 7. No blockers: refresh snapshots for each selection
    for (SelectionWithResolved swr : selWithResolved) {
      SimulationSelection sel = swr.selection();
      ScheduleOptionResolutionView resolved = swr.resolved();

      List<SimScheduleSession> freshSessions =
          resolved.sessions().stream()
              .map(s -> new SimScheduleSession(s.day(), s.startsAt(), s.endsAt()))
              .toList();

      SimulationSelection refreshed =
          sel.withRefreshedSnapshot(
              resolved.offeringVersion(), freshSessions, resolved.offeringStatus());

      saveSimulationSelectionPort.save(refreshed);
    }

    // 8. Return refreshed detail (now CURRENT because snapshots updated)
    return getSimulationQuery.getSimulation(simulationId, callerId);
  }
}
