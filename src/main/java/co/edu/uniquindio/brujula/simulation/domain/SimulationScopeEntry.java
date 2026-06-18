package co.edu.uniquindio.brujula.simulation.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a (programId, journeyCode) pair that scopes a simulation.
 *
 * <p>A simulation's scope is made up of 1..4 such pairs, with at most 2 distinct programIds
 * (INV-002-v2). Within each program, 1 or 2 jornadas are allowed.
 *
 * <p>Framework-free — no Spring/JPA annotations (ARQ-002).
 */
public record SimulationScopeEntry(UUID programId, String journeyCode) {

  public SimulationScopeEntry {
    Objects.requireNonNull(programId, "programId required");
    Objects.requireNonNull(journeyCode, "journeyCode required");
    if (journeyCode.isBlank()) {
      throw new IllegalArgumentException("journeyCode must not be blank");
    }
  }
}
