package co.edu.uniquindio.brujula.simulation.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure domain orchestrator: runs all validation rules against a candidate selection.
 *
 * <p>Evaluation order (all blocking rules run so the response lists every blocker):
 *
 * <ol>
 *   <li>{@link DuplicateSelectionRule} — SIMULATION_DUPLICATE_SELECTION
 *   <li>{@link ArchivedOfferingRule} — SIMULATION_ARCHIVED_OFFERING
 *   <li>{@link AlreadyPassedRule} — SIMULATION_COURSE_ALREADY_PASSED
 *   <li>{@link ScheduleClashDetector} — SIMULATION_SCHEDULE_CLASH
 *   <li>{@link CreditSummaryCalculator} — SIMULATION_CREDIT_LIMIT_EXCEEDED
 *   <li>{@link PrerequisiteEvaluator} — SIMULATION_PREREQUISITE_NOT_MET
 *   <li>{@link SequentialTrackEvaluator}— SIMULATION_SEQUENTIAL_ORDER
 *   <li>{@link MinCreditsRule} — SIMULATION_MIN_CREDITS_NOT_MET
 * </ol>
 *
 * <p>Non-blocking warnings collected after blocking rules:
 *
 * <ol>
 *   <li>{@link PrerequisiteEvaluator#collectSoftWarnings} — SIMULATION_SOFT_PREREQ_WARNING
 * </ol>
 *
 * <p>All blocking rules are evaluated — they do NOT short-circuit on first failure. The caller
 * receives the complete list so the user sees every problem at once.
 *
 * <p>Pure Java — no Spring, no ports. External data injected as records.
 */
public class SimulationValidationService {

  private final DuplicateSelectionRule duplicateSelectionRule = new DuplicateSelectionRule();
  private final ArchivedOfferingRule archivedOfferingRule = new ArchivedOfferingRule();
  private final AlreadyPassedRule alreadyPassedRule = new AlreadyPassedRule();
  private final ScheduleClashDetector scheduleClashDetector = new ScheduleClashDetector();
  private final CreditSummaryCalculator creditSummaryCalculator = new CreditSummaryCalculator();
  private final PrerequisiteEvaluator prerequisiteEvaluator = new PrerequisiteEvaluator();
  private final SequentialTrackEvaluator sequentialTrackEvaluator = new SequentialTrackEvaluator();
  private final MinCreditsRule minCreditsRule = new MinCreditsRule();

  /**
   * Validate a candidate selection against the current simulation state.
   *
   * @param candidate the schedule option the student wants to add
   * @param existingSelections active selections already in the simulation
   * @param requirements prerequisite requirement edges for the candidate course
   * @param passed courses the student has effectively passed
   * @param creditLimit the program credit limit policy (may be null if no limit)
   * @param sequentialPredecessor sequential-track predecessor info for the candidate course
   * @param accumulatedPassedCredits total accumulated credits the student has passed
   * @return result partitioned into blocking and warning lists
   */
  public SimulationValidationResult validate(
      CandidateSelection candidate,
      List<ExistingSelection> existingSelections,
      List<PrereqRequirement> requirements,
      PassedCourseSet passed,
      ProgramCreditLimit creditLimit,
      SequentialPredecessor sequentialPredecessor,
      int accumulatedPassedCredits) {

    List<ValidationNotice> blocking = new ArrayList<>();
    List<ValidationNotice> warnings = new ArrayList<>();

    // --- BLOCKING RULES ---

    // Rule 1: Duplicate course in simulation?
    duplicateSelectionRule.evaluate(candidate, existingSelections).ifPresent(blocking::add);

    // Rule 2: Offering is archived?
    archivedOfferingRule.evaluate(candidate).ifPresent(blocking::add);

    // Rule 3: Course already passed by student?
    alreadyPassedRule.evaluate(candidate, passed).ifPresent(blocking::add);

    // Rule 4: Schedule clash with existing sessions?
    scheduleClashDetector.detect(candidate, existingSelections).ifPresent(blocking::add);

    // Rule 5: Credit limit exceeded?
    if (creditLimit != null) {
      creditSummaryCalculator
          .evaluate(candidate, existingSelections, creditLimit)
          .ifPresent(blocking::add);
    }

    // Rule 6: Official / HIGH-confidence prerequisite not met?
    prerequisiteEvaluator.evaluate(candidate, requirements, passed).ifPresent(blocking::add);

    // Rule 7: Sequential track ordering not met?
    if (sequentialPredecessor != null) {
      sequentialTrackEvaluator
          .evaluate(candidate, sequentialPredecessor, passed)
          .ifPresent(blocking::add);
    }

    // Rule 8: Per-course minimum credits prerequisite not met?
    minCreditsRule
        .evaluate(candidate, requirements, accumulatedPassedCredits)
        .ifPresent(blocking::add);

    // --- WARNING COLLECTION ---

    // Soft-prereq warnings: inferred MEDIUM/LOW unmet prerequisites
    warnings.addAll(prerequisiteEvaluator.collectSoftWarnings(candidate, requirements, passed));

    return new SimulationValidationResult(blocking, warnings);
  }

  /**
   * Backward-compatible overload for callers that do not yet supply sequential-track or min-credits
   * data.
   *
   * @deprecated Use the full 7-parameter overload. This overload will be removed when all call
   *     sites are updated.
   */
  @Deprecated(forRemoval = true)
  public SimulationValidationResult validate(
      CandidateSelection candidate,
      List<ExistingSelection> existingSelections,
      List<PrereqRequirement> requirements,
      PassedCourseSet passed,
      ProgramCreditLimit creditLimit) {
    return validate(candidate, existingSelections, requirements, passed, creditLimit, null, 0);
  }
}
