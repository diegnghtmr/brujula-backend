package co.edu.uniquindio.brujula.simulation.domain;

/**
 * Persistence lifecycle for a simulation aggregate.
 *
 * <p>IMPORTANT (INV-004): This is NOT the same as {@code SimulationStatus}. The contract-level
 * {@code SimulationStatus} (CURRENT / OUTDATED) is a READ-TIME computed value and is NEVER stored.
 * Only {@code ACTIVE} and {@code DELETED} (soft-delete) are persisted.
 */
public enum SimulationLifecycle {
  ACTIVE,
  DELETED
}
