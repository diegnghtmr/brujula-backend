package co.edu.uniquindio.brujula.simulation.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure validation rule: enforces per-program credit limits (spec VAL-004).
 *
 * <p>Sums the credits of existing selections for the same {@code programId} as the candidate,
 * adds the candidate's credits, and compares against the configured limit from
 * {@code ProgramCreditLimit}. Exactly at the limit is allowed ({@code selectedCredits <= limit}).
 *
 * <p>Also reused by WU5 for building {@code ProgramCreditSummaryView} — kept general.
 *
 * <p>Package-private — only used by {@link SimulationValidationService}.
 * Pure Java, no Spring/JPA (ARQ-002, ADR-F4-004).
 */
class CreditSummaryCalculator {

  /**
   * Returns a blocking notice if adding the candidate would exceed the credit limit for its program.
   *
   * @param candidate          the selection being added
   * @param existingSelections existing active selections in the simulation
   * @param creditLimit        the program's credit limit policy
   * @return optional blocking notice with code {@code SIMULATION_CREDIT_LIMIT_EXCEEDED}
   */
  Optional<ValidationNotice> evaluate(
      CandidateSelection candidate,
      List<ExistingSelection> existingSelections,
      ProgramCreditLimit creditLimit) {

    double existingCredits = sumCreditsForProgram(existingSelections, candidate.programId());
    double attemptedCredits = existingCredits + candidate.credits();

    if (attemptedCredits > creditLimit.limit()) {
      return Optional.of(buildNotice(creditLimit, attemptedCredits));
    }
    return Optional.empty();
  }

  /**
   * Compute the total credits per program across all existing selections.
   * Useful for building the credit summary (WU5).
   *
   * @param selections the selections to summarize
   * @return map of programId → total selected credits
   */
  Map<UUID, Double> computeCreditsByProgram(List<ExistingSelection> selections) {
    Map<UUID, Double> result = new HashMap<>();
    for (ExistingSelection sel : selections) {
      result.merge(sel.programId(), sel.credits(), Double::sum);
    }
    return result;
  }

  private double sumCreditsForProgram(List<ExistingSelection> selections, UUID programId) {
    return selections.stream()
        .filter(s -> programId.equals(s.programId()))
        .mapToDouble(ExistingSelection::credits)
        .sum();
  }

  private ValidationNotice buildNotice(ProgramCreditLimit limit, double attemptedCredits) {
    return new ValidationNotice(
        "SIMULATION_CREDIT_LIMIT_EXCEEDED",
        "Adding this course would exceed the credit limit for program: " + limit.programName(),
        Map.of(
            "programName", limit.programName(),
            "attemptedCredits", attemptedCredits,
            "creditLimit", limit.limit()));
  }
}
