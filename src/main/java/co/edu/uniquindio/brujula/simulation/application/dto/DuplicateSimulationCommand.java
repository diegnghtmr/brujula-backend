package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Command to duplicate an existing simulation (SIM-006).
 *
 * <p>Idempotent via {@code Idempotency-Key} header (X-001).
 */
public record DuplicateSimulationCommand(
    UUID idempotencyKey, UUID simulationId, UUID ownerId, String name, String requestHash) {

  public DuplicateSimulationCommand {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey required");
    Objects.requireNonNull(simulationId, "simulationId required");
    Objects.requireNonNull(ownerId, "ownerId required");
    Objects.requireNonNull(requestHash, "requestHash required");
    // name may be null — derived from original if absent
  }
}
