package co.edu.uniquindio.brujula.simulation.domain;

import java.util.List;

/**
 * Sealed outcome of the simulation validation engine.
 *
 * <p>Compiler-enforced — either the selection is {@link Allowed} (no blocking notices) or {@link
 * Blocked} (at least one blocking notice). Both carry warnings for transparency.
 *
 * <p>Pure domain sealed interface — no Spring/JPA (ARQ-002, ADR-F4-004).
 */
public sealed interface ValidationOutcome
    permits ValidationOutcome.Allowed, ValidationOutcome.Blocked {

  /**
   * Selection is allowed. May carry non-blocking warnings.
   *
   * @param warnings non-blocking advisory notices (empty list if none)
   */
  record Allowed(List<ValidationNotice> warnings) implements ValidationOutcome {
    public Allowed {
      warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
  }

  /**
   * Selection is blocked. At least one blocking notice is present. Warnings are also carried.
   *
   * @param blocking blocking notices (non-empty)
   * @param warnings non-blocking advisory notices (may be empty)
   */
  record Blocked(List<ValidationNotice> blocking, List<ValidationNotice> warnings)
      implements ValidationOutcome {
    public Blocked {
      if (blocking == null || blocking.isEmpty()) {
        throw new IllegalArgumentException("Blocked outcome requires at least one blocking notice");
      }
      blocking = List.copyOf(blocking);
      warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
  }
}
