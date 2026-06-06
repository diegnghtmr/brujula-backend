package co.edu.uniquindio.brujula.programcatalog.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identity value object for the {@link AcademicProgram} aggregate.
 *
 * <p>Wraps a {@link UUID} to prevent accidental ID mixing at the type level.
 * Immutable (record). No Spring/JPA imports (ARQ-002).
 */
public record ProgramId(UUID value) {

  public ProgramId {
    Objects.requireNonNull(value, "ProgramId value must not be null");
  }

  /** Factory: generate a new random ProgramId. */
  public static ProgramId generate() {
    return new ProgramId(UUID.randomUUID());
  }
}
