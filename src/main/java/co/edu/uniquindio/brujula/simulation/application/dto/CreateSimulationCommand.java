package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Command to create a new simulation (SIM-001).
 *
 * <p>All fields use application-layer types only (ARQ-001). {@code scopeEntries} replaces the
 * former {@code programIds} list — each entry carries (programId, journeyCode) so the simulation
 * scope is fully specified at creation time.
 */
public record CreateSimulationCommand(
    UUID idempotencyKey,
    UUID ownerId,
    String name,
    UUID periodId,
    List<SimulationScopeEntryDto> scopeEntries,
    String requestHash) {

  public CreateSimulationCommand {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey required");
    Objects.requireNonNull(ownerId, "ownerId required");
    Objects.requireNonNull(name, "name required");
    Objects.requireNonNull(periodId, "periodId required");
    Objects.requireNonNull(scopeEntries, "scopeEntries required");
    Objects.requireNonNull(requestHash, "requestHash required");
  }
}
