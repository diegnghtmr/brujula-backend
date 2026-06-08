package co.edu.uniquindio.brujula.simulation.domain;

import java.util.List;

/**
 * Pure domain service: computes whether a simulation is CURRENT or OUTDATED (spec STALE-001).
 *
 * <p>Algorithm: compare each selection's {@code selectedOfferingVersion} snapshot against the
 * current offering version. If ALL match → CURRENT. If ANY differ → OUTDATED. Empty selections →
 * CURRENT (no versions to compare, nothing is stale).
 *
 * <p>Pure Java — no Spring/JPA (ARQ-002, ADR-F4-004). Status is never persisted (INV-004).
 */
public class StalenessEvaluator {

  /**
   * Version pair for one selection: snapshot vs current.
   *
   * @param selectedOfferingVersion the version captured at selection time
   * @param currentOfferingVersion  the live version from the offering module
   */
  public record VersionPair(long selectedOfferingVersion, long currentOfferingVersion) {}

  /**
   * Evaluate staleness across all version pairs.
   *
   * @param pairs list of (snapshot, current) pairs, one per active selection
   * @return CURRENT if all pairs match or list is empty; OUTDATED if any pair differs
   */
  public SimulationStatus evaluate(List<VersionPair> pairs) {
    if (pairs == null || pairs.isEmpty()) {
      return SimulationStatus.CURRENT;
    }
    for (VersionPair pair : pairs) {
      if (pair.selectedOfferingVersion() != pair.currentOfferingVersion()) {
        return SimulationStatus.OUTDATED;
      }
    }
    return SimulationStatus.CURRENT;
  }
}
