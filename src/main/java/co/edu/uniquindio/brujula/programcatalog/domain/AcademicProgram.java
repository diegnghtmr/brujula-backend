package co.edu.uniquindio.brujula.programcatalog.domain;

import java.util.Objects;

/**
 * AcademicProgram — aggregate root of the programcatalog module.
 *
 * <p>Represents a university academic program (e.g. Ingeniería de Sistemas, Lenguas Modernas).
 * Invariants enforced via factory methods:
 *
 * <ul>
 *   <li>{@code name} must not be null or blank.
 *   <li>{@code faculty} must not be null or blank.
 * </ul>
 *
 * <p>Status defaults to {@link ProgramStatus#ACTIVE}. Version initializes to {@code 0}. Carries
 * {@code @Version} in the JPA entity (DT-014 / DB-006).
 *
 * <p>No Spring/JPA imports (ARQ-002).
 */
public final class AcademicProgram {

  private final ProgramId id;
  private final String name;
  private final String faculty;
  private final ProgramStatus status;
  private final long version;

  private AcademicProgram(
      ProgramId id, String name, String faculty, ProgramStatus status, long version) {
    this.id = id;
    this.name = name;
    this.faculty = faculty;
    this.status = status;
    this.version = version;
  }

  // ---------------------------------------------------------------------------
  // Factory: new program creation
  // ---------------------------------------------------------------------------

  /** Create a new AcademicProgram. Status defaults to ACTIVE. Version initializes to 0. */
  public static AcademicProgram create(ProgramId id, String name, String faculty) {
    validate(id, name, faculty);
    return new AcademicProgram(id, name, faculty, ProgramStatus.ACTIVE, 0L);
  }

  /** Reconstitute an AcademicProgram from persistence (all fields explicit; no default logic). */
  public static AcademicProgram reconstitute(
      ProgramId id, String name, String faculty, ProgramStatus status, long version) {
    validate(id, name, faculty);
    Objects.requireNonNull(status, "status must not be null");
    return new AcademicProgram(id, name, faculty, status, version);
  }

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  private static void validate(ProgramId id, String name, String faculty) {
    Objects.requireNonNull(id, "id must not be null");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be null or blank");
    }
    if (faculty == null || faculty.isBlank()) {
      throw new IllegalArgumentException("faculty must not be null or blank");
    }
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public ProgramId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public String faculty() {
    return faculty;
  }

  public ProgramStatus status() {
    return status;
  }

  public long version() {
    return version;
  }

  // ---------------------------------------------------------------------------
  // Equality (identity-based)
  // ---------------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AcademicProgram other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "AcademicProgram[id=%s, name=%s, faculty=%s, status=%s]"
        .formatted(id, name, faculty, status);
  }
}
