package co.edu.uniquindio.brujula.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StalenessEvaluator} (spec STALE-001, INV-004).
 *
 * <p>Pure Java — no Spring context needed.
 */
class StalenessEvaluatorTest {

  private final StalenessEvaluator evaluator = new StalenessEvaluator();

  // -------------------------------------------------------------------------
  // CURRENT cases
  // -------------------------------------------------------------------------

  @Test
  void evaluate_emptySelections_returnsCurrent() {
    SimulationStatus status = evaluator.evaluate(List.of());
    assertThat(status).isEqualTo(SimulationStatus.CURRENT);
  }

  @Test
  void evaluate_nullList_returnsCurrent() {
    SimulationStatus status = evaluator.evaluate(null);
    assertThat(status).isEqualTo(SimulationStatus.CURRENT);
  }

  @Test
  void evaluate_allVersionsMatch_returnsCurrent() {
    List<StalenessEvaluator.VersionPair> pairs =
        List.of(
            new StalenessEvaluator.VersionPair(1L, 1L),
            new StalenessEvaluator.VersionPair(3L, 3L),
            new StalenessEvaluator.VersionPair(7L, 7L));

    SimulationStatus status = evaluator.evaluate(pairs);

    assertThat(status).isEqualTo(SimulationStatus.CURRENT);
  }

  @Test
  void evaluate_singleMatchingPair_returnsCurrent() {
    List<StalenessEvaluator.VersionPair> pairs =
        List.of(new StalenessEvaluator.VersionPair(5L, 5L));

    SimulationStatus status = evaluator.evaluate(pairs);

    assertThat(status).isEqualTo(SimulationStatus.CURRENT);
  }

  // -------------------------------------------------------------------------
  // OUTDATED cases
  // -------------------------------------------------------------------------

  @Test
  void evaluate_oneDifferentVersion_returnsOutdated() {
    List<StalenessEvaluator.VersionPair> pairs =
        List.of(
            new StalenessEvaluator.VersionPair(1L, 1L),
            new StalenessEvaluator.VersionPair(2L, 3L)); // snapshot 2, current 3 → OUTDATED

    SimulationStatus status = evaluator.evaluate(pairs);

    assertThat(status).isEqualTo(SimulationStatus.OUTDATED);
  }

  @Test
  void evaluate_allVersionsDiffer_returnsOutdated() {
    List<StalenessEvaluator.VersionPair> pairs =
        List.of(
            new StalenessEvaluator.VersionPair(1L, 2L), new StalenessEvaluator.VersionPair(3L, 5L));

    SimulationStatus status = evaluator.evaluate(pairs);

    assertThat(status).isEqualTo(SimulationStatus.OUTDATED);
  }

  @Test
  void evaluate_firstMatchSecondDiffers_returnsOutdated() {
    List<StalenessEvaluator.VersionPair> pairs =
        List.of(
            new StalenessEvaluator.VersionPair(10L, 10L), // CURRENT
            new StalenessEvaluator.VersionPair(10L, 11L)); // OUTDATED: version bumped

    SimulationStatus status = evaluator.evaluate(pairs);

    assertThat(status).isEqualTo(SimulationStatus.OUTDATED);
  }
}
