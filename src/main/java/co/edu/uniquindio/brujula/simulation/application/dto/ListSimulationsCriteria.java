package co.edu.uniquindio.brujula.simulation.application.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Query criteria for listing simulations (SIM-002).
 *
 * <p>BOLA: {@code ownerId} is always from the authenticated principal.
 */
public record ListSimulationsCriteria(
    UUID ownerId, UUID periodId, String status, int page, int size) {

  public ListSimulationsCriteria {
    Objects.requireNonNull(ownerId, "ownerId required");
  }
}
