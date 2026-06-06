package co.edu.uniquindio.brujula.programcatalog.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * StudyPlan — aggregate root representing a curricular plan for an academic program.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>{@code totalCredits} must be > 0 (CAT-002).
 *   <li>{@code name} must not be null or blank.
 *   <li>{@code programId} must not be null.
 * </ul>
 *
 * <p>Status defaults to {@link PlanStatus#DRAFT}. Version initializes to {@code 0} (DT-014/DB-006).
 * {@code referenceYear} is optional (nullable).
 *
 * <p>No Spring/JPA imports (ARQ-002).
 */
public final class StudyPlan {

  private final PlanId id;
  private final UUID programId;
  private final String name;
  private final PlanStatus status;
  private final int totalCredits;
  private final Integer referenceYear; // nullable
  private final long version;

  private StudyPlan(
      PlanId id,
      UUID programId,
      String name,
      PlanStatus status,
      int totalCredits,
      Integer referenceYear,
      long version) {
    this.id = id;
    this.programId = programId;
    this.name = name;
    this.status = status;
    this.totalCredits = totalCredits;
    this.referenceYear = referenceYear;
    this.version = version;
  }

  // ---------------------------------------------------------------------------
  // Factory: new plan creation
  // ---------------------------------------------------------------------------

  /**
   * Create a new StudyPlan. Status defaults to DRAFT. Version initializes to 0.
   *
   * @param id the plan identity
   * @param programId the owning program UUID
   * @param name the plan name
   * @param totalCredits total credits required to graduate (must be > 0)
   * @param referenceYear optional year of the plan agreement (nullable)
   */
  public static StudyPlan create(
      PlanId id, UUID programId, String name, int totalCredits, Integer referenceYear) {
    validate(id, programId, name, totalCredits);
    return new StudyPlan(id, programId, name, PlanStatus.DRAFT, totalCredits, referenceYear, 0L);
  }

  /**
   * Reconstitute a StudyPlan from persistence (all fields explicit; no default logic).
   */
  public static StudyPlan reconstitute(
      PlanId id,
      UUID programId,
      String name,
      PlanStatus status,
      int totalCredits,
      Integer referenceYear,
      long version) {
    validate(id, programId, name, totalCredits);
    Objects.requireNonNull(status, "status must not be null");
    return new StudyPlan(id, programId, name, status, totalCredits, referenceYear, version);
  }

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  private static void validate(PlanId id, UUID programId, String name, int totalCredits) {
    Objects.requireNonNull(id, "id must not be null");
    if (programId == null) throw new IllegalArgumentException("programId must not be null");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be null or blank");
    }
    if (totalCredits <= 0) {
      throw new IllegalArgumentException("totalCredits must be > 0, got: " + totalCredits);
    }
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public PlanId id() {
    return id;
  }

  public UUID programId() {
    return programId;
  }

  public String name() {
    return name;
  }

  public PlanStatus status() {
    return status;
  }

  public int totalCredits() {
    return totalCredits;
  }

  public Integer referenceYear() {
    return referenceYear;
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
    if (!(o instanceof StudyPlan other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "StudyPlan[id=%s, name=%s, status=%s, totalCredits=%d]"
        .formatted(id, name, status, totalCredits);
  }
}
