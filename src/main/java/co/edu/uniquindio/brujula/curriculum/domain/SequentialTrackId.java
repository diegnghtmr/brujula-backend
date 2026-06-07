package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a {@link SequentialTrack} aggregate (value object).
 *
 * <p>No Spring/JPA imports (ARQ-002).
 */
public record SequentialTrackId(UUID value) {

  public SequentialTrackId {
    Objects.requireNonNull(value, "SequentialTrackId value must not be null");
  }
}
