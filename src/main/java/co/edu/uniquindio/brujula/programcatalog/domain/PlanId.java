package co.edu.uniquindio.brujula.programcatalog.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identity value object for the {@link StudyPlan} aggregate.
 *
 * <p>Wraps a {@link UUID} to prevent accidental ID mixing at the type level. Immutable (record). No
 * Spring/JPA imports.
 */
public record PlanId(UUID value) {

  public PlanId {
    Objects.requireNonNull(value, "PlanId value must not be null");
  }

  /** Factory: generate a new random PlanId. */
  public static PlanId generate() {
    return new PlanId(UUID.randomUUID());
  }
}
