package co.edu.uniquindio.brujula.programcatalog.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identity value object for the {@link Journey} aggregate.
 *
 * <p>Wraps a {@link UUID} to prevent accidental ID mixing at the type level. Immutable (record). No
 * Spring/JPA imports (ARQ-002).
 */
public record JourneyId(UUID value) {

  public JourneyId {
    Objects.requireNonNull(value, "JourneyId value must not be null");
  }

  /** Factory: generate a new random JourneyId. */
  public static JourneyId generate() {
    return new JourneyId(UUID.randomUUID());
  }
}
