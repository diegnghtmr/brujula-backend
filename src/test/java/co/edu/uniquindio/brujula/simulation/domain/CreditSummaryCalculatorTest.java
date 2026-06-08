package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CreditSummaryCalculator}.
 *
 * <p>Pure domain — no Spring, no mocks.
 */
class CreditSummaryCalculatorTest {

  private CreditSummaryCalculator calculator;
  private final UUID programA = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private final UUID programB = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

  @BeforeEach
  void setUp() {
    calculator = new CreditSummaryCalculator();
  }

  @Test
  void exceeds_whenExisting21PlusCandiate3IsOver23Limit() {
    // 21 existing + 3 candidate = 24 > 23 limit → blocked
    List<ExistingSelection> existing = List.of(
        buildSelection(programA, 21.0));
    CandidateSelection candidate = buildCandidate(programA, 3.0);
    ProgramCreditLimit limit = new ProgramCreditLimit(programA, "Sistemas", 23);

    Optional<ValidationNotice> result = calculator.evaluate(candidate, existing, limit);

    assertThat(result).isPresent();
    ValidationNotice notice = result.get();
    assertThat(notice.code()).isEqualTo("SIMULATION_CREDIT_LIMIT_EXCEEDED");
    assertThat(notice.metadata()).containsEntry("creditLimit", 23);
    assertThat(notice.metadata()).containsEntry("programName", "Sistemas");
    assertThat((Double) notice.metadata().get("attemptedCredits")).isEqualTo(24.0);
  }

  @Test
  void allowedWhenExisting21PlusCandidate2EqualsLimit() {
    // 21 existing + 2 candidate = 23 = limit → allowed (exactly at limit is OK)
    List<ExistingSelection> existing = List.of(
        buildSelection(programA, 21.0));
    CandidateSelection candidate = buildCandidate(programA, 2.0);
    ProgramCreditLimit limit = new ProgramCreditLimit(programA, "Sistemas", 23);

    Optional<ValidationNotice> result = calculator.evaluate(candidate, existing, limit);

    assertThat(result).isEmpty();
  }

  @Test
  void differentProgramCreditsAreNotSummed() {
    // Existing 21 credits for programA; candidate 3 credits for programB — separate tracking
    List<ExistingSelection> existing = List.of(
        buildSelection(programA, 21.0));
    CandidateSelection candidate = buildCandidate(programB, 3.0);
    ProgramCreditLimit limit = new ProgramCreditLimit(programB, "Lenguas", 23);

    // programB has 0 existing + 3 candidate = 3, well under 23
    Optional<ValidationNotice> result = calculator.evaluate(candidate, existing, limit);

    assertThat(result).isEmpty();
  }

  @Test
  void noExistingSelectionsAllowed() {
    CandidateSelection candidate = buildCandidate(programA, 3.0);
    ProgramCreditLimit limit = new ProgramCreditLimit(programA, "Sistemas", 23);

    Optional<ValidationNotice> result = calculator.evaluate(candidate, List.of(), limit);

    assertThat(result).isEmpty();
  }

  @Test
  void blockedWhenNoCreditLimitNullIsHandledByService() {
    // The service guards null creditLimit — calculator itself requires a non-null limit
    // This test verifies the calculator correctly blocks at boundary
    List<ExistingSelection> existing = List.of(buildSelection(programA, 22.0));
    CandidateSelection candidate = buildCandidate(programA, 2.0); // 24 > 23
    ProgramCreditLimit limit = new ProgramCreditLimit(programA, "Sistemas", 23);

    Optional<ValidationNotice> result = calculator.evaluate(candidate, existing, limit);
    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_CREDIT_LIMIT_EXCEEDED");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private ExistingSelection buildSelection(UUID programId, double credits) {
    return new ExistingSelection(UUID.randomUUID(), UUID.randomUUID(), "Some Course",
        programId, credits, List.of());
  }

  private CandidateSelection buildCandidate(UUID programId, double credits) {
    return new CandidateSelection(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "New Course", programId, credits, "DIURNA", "FINAL", 1L, List.of());
  }
}
