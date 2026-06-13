package co.edu.uniquindio.brujula.programcatalog.domain;

import java.util.Objects;

/**
 * Journey (Jornada) — entity representing a study schedule modality for a program.
 *
 * <p>Examples: DIURNA, NOCTURNA. A program may have multiple journeys.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>{@code code} must not be null or blank (e.g. "DIURNA", "NOCTURNA").
 *   <li>{@code name} must not be null or blank.
 * </ul>
 *
 * <p>No Spring/JPA imports.
 */
public final class Journey {

  private final JourneyId id;
  private final String code;
  private final String name;
  private final boolean active;

  private Journey(JourneyId id, String code, String name, boolean active) {
    this.id = id;
    this.code = code;
    this.name = name;
    this.active = active;
  }

  // ---------------------------------------------------------------------------
  // Factory
  // ---------------------------------------------------------------------------

  /** Create a new Journey. Defaults active to {@code true}. */
  public static Journey create(JourneyId id, String code, String name) {
    validate(id, code, name);
    return new Journey(id, code, name, true);
  }

  /** Reconstitute a Journey from persistence. */
  public static Journey reconstitute(JourneyId id, String code, String name, boolean active) {
    validate(id, code, name);
    return new Journey(id, code, name, active);
  }

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  private static void validate(JourneyId id, String code, String name) {
    Objects.requireNonNull(id, "id must not be null");
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code must not be null or blank");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be null or blank");
    }
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public JourneyId id() {
    return id;
  }

  public String code() {
    return code;
  }

  public String name() {
    return name;
  }

  public boolean active() {
    return active;
  }

  // ---------------------------------------------------------------------------
  // Equality (identity-based)
  // ---------------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Journey other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Journey[id=%s, code=%s, name=%s, active=%b]".formatted(id, code, name, active);
  }
}
