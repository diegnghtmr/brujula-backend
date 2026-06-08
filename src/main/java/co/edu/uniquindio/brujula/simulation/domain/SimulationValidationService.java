package co.edu.uniquindio.brujula.simulation.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure domain orchestrator: runs all WU4a validation rules against a candidate selection.
 *
 * <p>Evaluation order (fail-fast for blocking):
 * <ol>
 *   <li>{@link AlreadyPassedRule} — SIMULATION_COURSE_ALREADY_PASSED
 *   <li>{@link ScheduleClashDetector} — SIMULATION_SCHEDULE_CLASH
 *   <li>{@link CreditSummaryCalculator} — SIMULATION_CREDIT_LIMIT_EXCEEDED
 *   <li>{@link PrerequisiteEvaluator} — SIMULATION_PREREQUISITE_NOT_MET
 * </ol>
 *
 * <p>WU4b extension point: add rules by appending to the sequential chain below. The structure
 * (explicit ordered calls + separate blocking/warning lists) makes it trivial to add
 * SIMULATION_DUPLICATE_SELECTION, SIMULATION_ARCHIVED_OFFERING, etc. without rewriting.
 *
 * <p>TODO(WU4b): add DuplicateSelectionRule, ArchivedOfferingRule, SequentialTrackEvaluator,
 * MinCreditsRule, and soft-prereq WARNING collection.
 *
 * <p>Pure Java — no Spring, no ports (ARQ-002, ADR-F4-004). External data injected as records.
 */
public class SimulationValidationService {

  private final AlreadyPassedRule alreadyPassedRule = new AlreadyPassedRule();
  private final ScheduleClashDetector scheduleClashDetector = new ScheduleClashDetector();
  private final CreditSummaryCalculator creditSummaryCalculator = new CreditSummaryCalculator();
  private final PrerequisiteEvaluator prerequisiteEvaluator = new PrerequisiteEvaluator();

  /**
   * Validate a candidate selection against the current simulation state.
   *
   * @param candidate          the schedule option the student wants to add
   * @param existingSelections active selections already in the simulation
   * @param requirements       prerequisite requirement edges for the candidate course
   * @param passed             courses the student has effectively passed
   * @param creditLimit        the program credit limit policy (may be null if no limit configured)
   * @return result partitioned into blocking and warning lists
   */
  public SimulationValidationResult validate(
      CandidateSelection candidate,
      List<ExistingSelection> existingSelections,
      List<PrereqRequirement> requirements,
      PassedCourseSet passed,
      ProgramCreditLimit creditLimit) {

    List<ValidationNotice> blocking = new ArrayList<>();
    List<ValidationNotice> warnings = new ArrayList<>();

    // Rule 1: Already passed?
    alreadyPassedRule.evaluate(candidate, passed).ifPresent(blocking::add);

    // Rule 2: Schedule clash?
    scheduleClashDetector.detect(candidate, existingSelections).ifPresent(blocking::add);

    // Rule 3: Credit limit exceeded?
    if (creditLimit != null) {
      creditSummaryCalculator.evaluate(candidate, existingSelections, creditLimit)
          .ifPresent(blocking::add);
    }

    // Rule 4: Prerequisites not met?
    prerequisiteEvaluator.evaluate(candidate, requirements, passed).ifPresent(blocking::add);

    // TODO(WU4b): Run additional blocking rules:
    //   - DuplicateSelectionRule     → SIMULATION_DUPLICATE_SELECTION
    //   - ArchivedOfferingRule       → SIMULATION_ARCHIVED_OFFERING
    //   - SequentialTrackEvaluator   → SIMULATION_SEQUENTIAL_ORDER
    //   - MinCreditsRule             → SIMULATION_MIN_CREDITS_NOT_MET
    //   Collect soft-prereq WARNINGs: SIMULATION_SOFT_PREREQ_WARNING

    return new SimulationValidationResult(blocking, warnings);
  }
}
