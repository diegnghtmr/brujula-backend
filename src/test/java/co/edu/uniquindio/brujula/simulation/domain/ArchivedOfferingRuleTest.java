package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * RED→GREEN unit tests for {@link ArchivedOfferingRule} (spec VAL-007).
 *
 * <p>Each test genuinely exercises the rule branch under test:
 * <ul>
 *   <li>ARCHIVED status → blocked.
 *   <li>FINAL status → allowed.
 *   <li>PRELIMINARY status → allowed.
 *   <li>Case-insensitive match for robustness.
 * </ul>
 */
class ArchivedOfferingRuleTest {

  private ArchivedOfferingRule rule;

  private final UUID courseId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    rule = new ArchivedOfferingRule();
  }

  // -------------------------------------------------------------------------
  // BLOCKING path — rule fires for ARCHIVED
  // -------------------------------------------------------------------------

  @Test
  void archivedOffering_returnsBlockingNotice() {
    // ARRANGE: offering status = ARCHIVED
    CandidateSelection candidate = buildCandidateWithStatus("ARCHIVED");

    // ACT
    Optional<ValidationNotice> result = rule.evaluate(candidate);

    // ASSERT: rule fires → BLOCKING notice
    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_ARCHIVED_OFFERING");
    assertThat(result.get().message()).contains("Programación II");
  }

  @Test
  void archivedOffering_caseInsensitive_returnsBlockingNotice() {
    CandidateSelection candidate = buildCandidateWithStatus("archived");
    Optional<ValidationNotice> result = rule.evaluate(candidate);
    assertThat(result).isPresent();
    assertThat(result.get().code()).isEqualTo("SIMULATION_ARCHIVED_OFFERING");
  }

  // -------------------------------------------------------------------------
  // ALLOWED paths — rule does NOT fire
  // -------------------------------------------------------------------------

  @Test
  void finalOffering_returnsEmpty() {
    CandidateSelection candidate = buildCandidateWithStatus("FINAL");
    Optional<ValidationNotice> result = rule.evaluate(candidate);
    assertThat(result).isEmpty();
  }

  @Test
  void preliminaryOffering_returnsEmpty() {
    CandidateSelection candidate = buildCandidateWithStatus("PRELIMINARY");
    Optional<ValidationNotice> result = rule.evaluate(candidate);
    assertThat(result).isEmpty();
  }

  @Test
  void draftOffering_returnsEmpty() {
    // DRAFT is not ARCHIVED — rule does not block (other rules may apply)
    CandidateSelection candidate = buildCandidateWithStatus("DRAFT");
    Optional<ValidationNotice> result = rule.evaluate(candidate);
    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CandidateSelection buildCandidateWithStatus(String offeringStatus) {
    SimScheduleSession session = new SimScheduleSession(
        "FRIDAY", LocalTime.of(19, 0), LocalTime.of(22, 0));
    return new CandidateSelection(
        UUID.randomUUID(), UUID.randomUUID(), courseId,
        "Programación II", programId, 3.0, "NOCTURNA",
        offeringStatus, 1L, List.of(session));
  }
}
