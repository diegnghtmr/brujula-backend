package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * DegreeWorkModality — aggregate representing a degree work (trabajo de grado) modality
 * option for a study plan.
 *
 * <p>Examples: Aplicación, Investigación, Pasantía. Each is a distinct modality row
 * associated with a study plan ({@code plan_id}).
 *
 * <p>Credits and curricularWeeklyHours are nullable (some modalities are credit-free).
 *
 * <p>No Spring/JPA imports (ARQ-002).
 */
public final class DegreeWorkModality {

  private final DegreeWorkModalityId id;
  private final UUID planId;
  private final String name;
  private final Integer credits;              // nullable
  private final Integer curricularWeeklyHours; // nullable
  private final DegreeWorkModalityStatus status;
  private final long version;

  private DegreeWorkModality(
      DegreeWorkModalityId id,
      UUID planId,
      String name,
      Integer credits,
      Integer curricularWeeklyHours,
      DegreeWorkModalityStatus status,
      long version) {
    this.id = id;
    this.planId = planId;
    this.name = name;
    this.credits = credits;
    this.curricularWeeklyHours = curricularWeeklyHours;
    this.status = status;
    this.version = version;
  }

  /**
   * Reconstitute a DegreeWorkModality from persistence (all fields explicit; no default logic).
   */
  public static DegreeWorkModality reconstitute(
      DegreeWorkModalityId id,
      UUID planId,
      String name,
      Integer credits,
      Integer curricularWeeklyHours,
      DegreeWorkModalityStatus status,
      long version) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(planId, "planId must not be null");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be null or blank");
    }
    Objects.requireNonNull(status, "status must not be null");
    return new DegreeWorkModality(id, planId, name, credits, curricularWeeklyHours, status, version);
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public DegreeWorkModalityId id() { return id; }
  public UUID planId() { return planId; }
  public String name() { return name; }

  /** Nullable: credits for this modality (null means not defined). */
  public Integer credits() { return credits; }

  /** Nullable: weekly curricular hours (null means not defined). */
  public Integer curricularWeeklyHours() { return curricularWeeklyHours; }

  public DegreeWorkModalityStatus status() { return status; }
  public long version() { return version; }

  // ---------------------------------------------------------------------------
  // Equality (identity-based)
  // ---------------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DegreeWorkModality other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "DegreeWorkModality[id=%s, planId=%s, name=%s, status=%s]"
        .formatted(id, planId, name, status);
  }
}
