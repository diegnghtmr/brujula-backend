package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Command: add a schedule option selection to a simulation (VAL-001).
 *
 * <p>ARQ-001: only UUID/String/boolean primitives — no domain entities.
 *
 * @param idempotencyKey       UUID from Idempotency-Key header (X-001)
 * @param simulationId         target simulation UUID
 * @param ownerId              caller UUID (resolved from SecurityContext, BOLA-safe)
 * @param scheduleOptionId     the schedule option to add
 * @param acknowledgeWarnings  client acknowledges non-blocking warnings (WU4b feature — ignored in WU4a)
 * @param requestHash          SHA-256 of the request body for idempotency conflict detection
 */
public record AddSelectionCommand(
    UUID idempotencyKey,
    UUID simulationId,
    UUID ownerId,
    UUID scheduleOptionId,
    boolean acknowledgeWarnings,
    String requestHash) {

  public AddSelectionCommand {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey required");
    Objects.requireNonNull(simulationId, "simulationId required");
    Objects.requireNonNull(ownerId, "ownerId required");
    Objects.requireNonNull(scheduleOptionId, "scheduleOptionId required");
    Objects.requireNonNull(requestHash, "requestHash required");
  }
}
