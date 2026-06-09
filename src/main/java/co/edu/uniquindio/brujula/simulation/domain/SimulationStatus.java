package co.edu.uniquindio.brujula.simulation.domain;

/**
 * Read-time computed status of a simulation (spec STALE-001, INV-004).
 *
 * <p>CURRENT: all selections' {@code selectedOfferingVersion} match the current offering version.
 * OUTDATED: at least one selection's snapshot version differs from the current offering version.
 *
 * <p>This enum is NEVER persisted — it is always derived at read time by {@link
 * StalenessEvaluator}. The persistence layer stores ACTIVE/DELETED (lifecycle) only.
 *
 * <p>Pure Java, no Spring/JPA (ARQ-002, INV-004, ADR-F4-001).
 */
public enum SimulationStatus {
  CURRENT,
  OUTDATED
}
