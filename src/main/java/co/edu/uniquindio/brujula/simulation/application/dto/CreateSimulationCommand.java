package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Command to create a new simulation (SIM-001).
 *
 * <p>All fields use UUID/String primitives (ARQ-001).
 */
public record CreateSimulationCommand(
    UUID idempotencyKey,
    UUID ownerId,
    String name,
    UUID periodId,
    List<UUID> programIds,
    String requestHash) {

  public CreateSimulationCommand {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey required");
    Objects.requireNonNull(ownerId, "ownerId required");
    Objects.requireNonNull(name, "name required");
    Objects.requireNonNull(periodId, "periodId required");
    Objects.requireNonNull(programIds, "programIds required");
    Objects.requireNonNull(requestHash, "requestHash required");
  }
}
