package co.edu.uniquindio.brujula.simulation.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable notice produced by a validation rule.
 *
 * <p>Pure domain record — no Spring/JPA.
 *
 * @param code machine-readable code (SIMULATION_* prefix)
 * @param message human-readable description
 * @param metadata optional context map (nullable; may carry clash coordinates, course names, etc.)
 */
public record ValidationNotice(String code, String message, Map<String, Object> metadata) {

  public ValidationNotice {
    Objects.requireNonNull(code, "code required");
    Objects.requireNonNull(message, "message required");
  }

  /** Convenience constructor when no metadata is needed. */
  public ValidationNotice(String code, String message) {
    this(code, message, null);
  }
}
