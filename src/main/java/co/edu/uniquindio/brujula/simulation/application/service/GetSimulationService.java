package co.edu.uniquindio.brujula.simulation.application.service;

import co.edu.uniquindio.brujula.simulation.application.dto.ProgramCreditSummaryView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationDetailView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationSelectionView;
import co.edu.uniquindio.brujula.simulation.application.dto.SimulationValidationView;
import co.edu.uniquindio.brujula.simulation.application.dto.ValidationNoticeView;
import co.edu.uniquindio.brujula.simulation.application.dto.WeeklyScheduleEntryView;
import co.edu.uniquindio.brujula.simulation.application.port.in.GetSimulationQuery;
import co.edu.uniquindio.brujula.simulation.application.port.out.CreditLimitPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.LoadSimulationSelectionPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgramNameReadPort;
import co.edu.uniquindio.brujula.simulation.application.port.out.ProgressReadPort;
import co.edu.uniquindio.brujula.simulation.domain.CandidateSelection;
import co.edu.uniquindio.brujula.simulation.domain.CreditSummaryCalculator;
import co.edu.uniquindio.brujula.simulation.domain.ExistingSelection;
import co.edu.uniquindio.brujula.simulation.domain.PassedCourseSet;
import co.edu.uniquindio.brujula.simulation.domain.PrereqRequirement;
import co.edu.uniquindio.brujula.simulation.domain.ProgramCreditLimit;
import co.edu.uniquindio.brujula.simulation.domain.SequentialPredecessor;
import co.edu.uniquindio.brujula.simulation.domain.Simulation;
import co.edu.uniquindio.brujula.simulation.domain.SimulationAccessDeniedException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationLifecycle;
import co.edu.uniquindio.brujula.simulation.domain.SimulationNotFoundException;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationStatus;
import co.edu.uniquindio.brujula.simulation.domain.SimulationValidationResult;
import co.edu.uniquindio.brujula.simulation.domain.SimulationValidationService;
import co.edu.uniquindio.brujula.simulation.domain.StalenessEvaluator;
import co.edu.uniquindio.brujula.simulation.domain.ValidationNotice;
import co.edu.uniquindio.brujula.simulation.domain.WeeklyScheduleAssembler;
import co.edu.uniquindio.brujula.simulation.domain.WeeklyScheduleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Application service: get full simulation detail (spec SIM-003, DET-001..003, STALE-001, X-003).
 *
 * <p>Assembles:
 *
 * <ol>
 *   <li>Selections — from persistence, converted to {@link SimulationSelectionView}.
 *   <li>CreditSummary — EXACTLY one entry per programId in Simulation.programIds (DET-002),
 *       including programs with 0 selected credits. Credits from real selection data. Credit limits
 *       from {@link CreditLimitPort} (real, default 23). Names from {@link ProgramNameReadPort}
 *       (real, no placeholder).
 *   <li>WeeklySchedule — {@link WeeklyScheduleAssembler} over all selections, with real inClash
 *       computation.
 *   <li>Validation — {@link SimulationValidationService} re-run read-time snapshot (DET-003). Does
 *       NOT mutate stored state.
 *   <li>StatusName — {@link StalenessEvaluator} comparing selectedOfferingVersion snapshots against
 *       current offering versions (STALE-001). NEVER persisted (INV-004).
 *   <li>TotalWeeklyHours — sum of all session durations in hours (real).
 * </ol>
 *
 * <p>Ownership: non-owner → 403 (X-003).
 *
 * <p>Framework-free — wired by {@code SimulationConfiguration}.
 */
public class GetSimulationService implements GetSimulationQuery {

  private final LoadSimulationPort loadSimulationPort;
  private final LoadSimulationSelectionPort loadSimulationSelectionPort;
  private final OfferingReadPort offeringReadPort;
  private final CreditLimitPort creditLimitPort;
  private final ProgressReadPort progressReadPort;
  private final CurriculumReadPort curriculumReadPort;
  private final ProgramNameReadPort programNameReadPort;
  private final SimulationValidationService validationService;
  private final StalenessEvaluator stalenessEvaluator;
  private final WeeklyScheduleAssembler scheduleAssembler;
  private final CreditSummaryCalculator creditSummaryCalculator;

  public GetSimulationService(
      LoadSimulationPort loadSimulationPort,
      LoadSimulationSelectionPort loadSimulationSelectionPort,
      OfferingReadPort offeringReadPort,
      CreditLimitPort creditLimitPort,
      ProgressReadPort progressReadPort,
      CurriculumReadPort curriculumReadPort,
      ProgramNameReadPort programNameReadPort) {
    this.loadSimulationPort = loadSimulationPort;
    this.loadSimulationSelectionPort = loadSimulationSelectionPort;
    this.offeringReadPort = offeringReadPort;
    this.creditLimitPort = creditLimitPort;
    this.progressReadPort = progressReadPort;
    this.curriculumReadPort = curriculumReadPort;
    this.programNameReadPort = programNameReadPort;
    this.validationService = new SimulationValidationService();
    this.stalenessEvaluator = new StalenessEvaluator();
    this.scheduleAssembler = new WeeklyScheduleAssembler();
    this.creditSummaryCalculator = new CreditSummaryCalculator();
  }

  @Override
  public SimulationDetailView getSimulation(UUID simulationId, UUID callerId) {
    // 1. Load simulation — ownership check
    Simulation simulation =
        loadSimulationPort
            .loadById(simulationId)
            .filter(s -> s.getLifecycleStatus() == SimulationLifecycle.ACTIVE)
            .orElseThrow(() -> new SimulationNotFoundException(simulationId));

    if (!simulation.getOwnerId().equals(callerId)) {
      throw new SimulationAccessDeniedException(simulationId);
    }

    // 2. Resolve period name
    String periodName;
    try {
      periodName = offeringReadPort.resolvePeriodName(simulation.getPeriodId());
    } catch (Exception e) {
      periodName = simulation.getPeriodId().toString();
    }

    // 3. Load active selections
    List<SimulationSelection> activeSelections =
        loadSimulationSelectionPort.loadActiveBySimulationId(simulationId);

    // 4. Compute staleness (STALE-001, INV-004: never persisted)
    // Conservative policy (FIX F2): when the offering cannot be resolved (deleted/missing),
    // treat the selection as stale by injecting a sentinel mismatch (-1 != snapshot).
    // A simulation whose offering cannot be validated CANNOT be confirmed CURRENT.
    // This keeps the GET non-throwing (valid domain object) while surfacing the problem via
    // OUTDATED.
    List<StalenessEvaluator.VersionPair> versionPairs =
        activeSelections.stream()
            .map(
                s -> {
                  long currentVersion;
                  try {
                    currentVersion = offeringReadPort.currentOfferingVersion(s.getOfferingId());
                  } catch (Exception e) {
                    // Offering unresolvable: inject sentinel mismatch → StalenessEvaluator returns
                    // OUTDATED
                    currentVersion = s.getSelectedOfferingVersion() - 1;
                  }
                  return new StalenessEvaluator.VersionPair(
                      s.getSelectedOfferingVersion(), currentVersion);
                })
            .toList();
    SimulationStatus status = stalenessEvaluator.evaluate(versionPairs);

    // 5. Build selection views
    List<SimulationSelectionView> selectionViews =
        activeSelections.stream().map(this::toSelectionView).toList();

    // 6. Build ExistingSelection list for validation + credit summary
    List<ExistingSelection> existingSelections =
        activeSelections.stream()
            .map(
                s ->
                    new ExistingSelection(
                        s.getId().value(),
                        s.getCourseId(),
                        s.getCourseName(),
                        s.getProgramId(),
                        s.getCredits(),
                        s.getSessions()))
            .toList();

    // 7. CreditSummary — exactly one entry per programId in simulation.programIds (DET-002)
    Map<UUID, Double> creditsByProgram =
        creditSummaryCalculator.computeCreditsByProgram(existingSelections);
    List<ProgramCreditSummaryView> creditSummary = new ArrayList<>();
    for (UUID programId : simulation.getProgramIds()) {
      double selectedCredits = creditsByProgram.getOrDefault(programId, 0.0);
      int limit = creditLimitPort.creditLimit(programId, simulation.getPeriodId());
      String programName;
      try {
        programName = programNameReadPort.programName(programId);
      } catch (Exception e) {
        programName = programId.toString();
      }
      creditSummary.add(
          new ProgramCreditSummaryView(
              programId, programName, selectedCredits, limit, selectedCredits <= limit));
    }

    // 8. WeeklySchedule (DET-001)
    List<WeeklyScheduleAssembler.SelectionSessions> selectionSessions =
        activeSelections.stream()
            .map(
                s ->
                    new WeeklyScheduleAssembler.SelectionSessions(
                        s.getId().value(), s.getCourseName(), s.getProgramId(), s.getSessions()))
            .toList();
    List<WeeklyScheduleEntry> scheduleEntries = scheduleAssembler.assemble(selectionSessions);
    List<WeeklyScheduleEntryView> weeklySchedule =
        scheduleEntries.stream()
            .map(
                e ->
                    new WeeklyScheduleEntryView(
                        e.day(),
                        e.startsAt(),
                        e.endsAt(),
                        e.courseName(),
                        e.programId(),
                        e.selectionId(),
                        e.inClash()))
            .toList();

    // 9. TotalWeeklyHours — sum session durations
    double totalWeeklyHours =
        activeSelections.stream()
            .flatMap(s -> s.getSessions().stream())
            .mapToDouble(
                s -> {
                  long minutes = java.time.Duration.between(s.startsAt(), s.endsAt()).toMinutes();
                  return minutes / 60.0;
                })
            .sum();

    // 10. Validation snapshot (DET-003) — read-time re-run, does NOT mutate state
    SimulationValidationView validationView =
        buildValidationSnapshot(simulation, activeSelections, callerId);

    return new SimulationDetailView(
        simulation.getId().value(),
        simulation.getName(),
        simulation.getPeriodId(),
        periodName,
        simulation.getProgramIds(),
        status.name(),
        simulation.getCreatedAt(),
        simulation.getUpdatedAt(),
        simulation.getVersion(),
        selectionViews,
        creditSummary,
        weeklySchedule,
        totalWeeklyHours,
        validationView);
  }

  // -------------------------------------------------------------------------
  // Validation snapshot (DET-003): re-run all rules read-time
  // -------------------------------------------------------------------------

  private SimulationValidationView buildValidationSnapshot(
      Simulation simulation, List<SimulationSelection> activeSelections, UUID callerId) {

    if (activeSelections.isEmpty()) {
      return new SimulationValidationView(List.of(), List.of());
    }

    // Gather passed courses (union across all programs)
    Set<UUID> allPassedIds = new HashSet<>();
    for (UUID programId : simulation.getProgramIds()) {
      try {
        allPassedIds.addAll(progressReadPort.passedCourseIds(callerId, programId));
      } catch (Exception ignored) {
        // if progress module unavailable, assume no passed courses for validation
      }
    }
    PassedCourseSet passedCourseSet = new PassedCourseSet(allPassedIds);

    // QAL-PERF-002: Compute accumulated passed credits ONCE via batch call (O(1) queries)
    // instead of recomputing per-selection with a per-course loop (O(M*N) queries).
    // A course absent from the map contributes 0 credits (same behaviour as prior try-catch).
    int accumulatedPassedCredits = 0;
    if (!allPassedIds.isEmpty()) {
      try {
        Map<UUID, Integer> passedCreditMap = curriculumReadPort.coursesCredits(allPassedIds);
        accumulatedPassedCredits =
            passedCreditMap.values().stream().mapToInt(Integer::intValue).sum();
      } catch (Exception ignored) {
        // If curriculum is unavailable, treat as 0 credits (fail-open)
      }
    }

    // Re-validate each selection as a candidate against all OTHER existing selections
    // Accumulate all blocking + warning notices across the whole simulation
    List<ValidationNotice> allBlocking = new ArrayList<>();
    List<ValidationNotice> allWarnings = new ArrayList<>();

    for (SimulationSelection sel : activeSelections) {
      // Build candidate from this selection
      CandidateSelection candidate =
          new CandidateSelection(
              sel.getScheduleOptionId(),
              sel.getOfferingId(),
              sel.getCourseId(),
              sel.getCourseName(),
              sel.getProgramId(),
              sel.getCredits(),
              sel.getJourneyCode(),
              sel.getOfferingStatus(),
              sel.getSelectedOfferingVersion(),
              sel.getSessions());

      // Existing = all OTHER active selections
      List<ExistingSelection> others =
          activeSelections.stream()
              .filter(s -> !s.getId().value().equals(sel.getId().value()))
              .map(
                  s ->
                      new ExistingSelection(
                          s.getId().value(),
                          s.getCourseId(),
                          s.getCourseName(),
                          s.getProgramId(),
                          s.getCredits(),
                          s.getSessions()))
              .toList();

      // Prerequisites for this course
      List<PrereqRequirement> requirements;
      try {
        requirements = curriculumReadPort.requirementsForCourse(sel.getCourseId());
      } catch (Exception e) {
        requirements = List.of();
      }

      // Credit limit for this program
      ProgramCreditLimit creditLimit;
      try {
        int limit = creditLimitPort.creditLimit(sel.getProgramId(), simulation.getPeriodId());
        // Use the programName from selections (denormalized) or fetch
        String programName;
        try {
          programName = programNameReadPort.programName(sel.getProgramId());
        } catch (Exception e) {
          programName = sel.getProgramId().toString();
        }
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

      // accumulatedPassedCredits: computed once above via batch call (QAL-PERF-002)

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

    return new SimulationValidationView(
        allBlocking.stream()
            .map(n -> new ValidationNoticeView(n.code(), n.message(), n.metadata()))
            .toList(),
        allWarnings.stream()
            .map(n -> new ValidationNoticeView(n.code(), n.message(), n.metadata()))
            .toList());
  }

  // -------------------------------------------------------------------------
  // Selection → view mapping
  // -------------------------------------------------------------------------

  private SimulationSelectionView toSelectionView(SimulationSelection s) {
    List<SimulationSelectionView.SessionView> sessionViews =
        s.getSessions().stream()
            .map(
                sess ->
                    new SimulationSelectionView.SessionView(
                        sess.day(), sess.startsAt().toString(), sess.endsAt().toString()))
            .toList();
    return new SimulationSelectionView(
        s.getId().value(),
        s.getOfferingId(),
        s.getScheduleOptionId(),
        s.getCourseId(),
        s.getCourseName(),
        s.getProgramId(),
        s.getCredits(),
        s.getJourneyCode(),
        s.getOfferingStatus(),
        s.getSelectedOfferingVersion(),
        sessionViews,
        List.of()); // warnings not re-loaded for list view (they ride in the original selection)
  }
}
