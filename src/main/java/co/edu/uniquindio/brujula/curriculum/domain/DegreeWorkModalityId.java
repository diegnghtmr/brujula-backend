package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a {@link DegreeWorkModality} aggregate (value object).
 *
 * <p>No Spring/JPA imports.
 */
public record DegreeWorkModalityId(UUID value) {

  public DegreeWorkModalityId {
    Objects.requireNonNull(value, "DegreeWorkModalityId value must not be null");
  }
}
