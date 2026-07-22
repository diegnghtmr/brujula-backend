package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * CourseRequirement — aggregate representing a prerequisite (or co-requisite) edge between courses.
 *
 * <p>Each requirement has a type ({@link RequirementType}) that determines which fields are
 * required. Factory methods enforce per-type validation rules:
 *
 * <ul>
 *   <li>{@link RequirementType#COURSE_PASSED}: requires non-null {@code requiredCourseId}.
 *   <li>{@link RequirementType#SEQUENCE}: requires non-null {@code requiredCourseId}.
 *   <li>{@link RequirementType#MINIMUM_CREDITS}: requires non-null, non-negative {@code
 *       requiredCredits}.
 *   <li>{@link RequirementType#CONFIGURABLE}: no specific required field (evaluated externally).
 * </ul>
 *
 * <p>Confidence and inferred are metadata fields stored for the simulator; they do NOT affect graph
 * topology or validation.
 *
 * <p>Status defaults to {@link CourseRequirementStatus#ACTIVE}. Version initializes to {@code 0}.
 * No Spring/JPA imports.
 */
public final class CourseRequirement {

  private final CourseRequirementId id;
  private final UUID targetCourseId;
  private final RequirementType type;
  private final UUID requiredCourseId; // nullable for MINIMUM_CREDITS / CONFIGURABLE
  private final Integer requiredCredits; // nullable; only for MINIMUM_CREDITS
  private final Boolean simultaneityAllowed; // nullable
  private final Confidence confidence; // nullable; metadata for simulator
  private final Boolean inferred; // nullable; metadata flag
  private final CourseRequirementStatus status;
  private final long version;

  private CourseRequirement(
      CourseRequirementId id,
      UUID targetCourseId,
      RequirementType type,
      UUID requiredCourseId,
      Integer requiredCredits,
      Boolean simultaneityAllowed,
      Confidence confidence,
      Boolean inferred,
      CourseRequirementStatus status,
      long version) {
    this.id = id;
    this.targetCourseId = targetCourseId;
    this.type = type;
    this.requiredCourseId = requiredCourseId;
    this.requiredCredits = requiredCredits;
    this.simultaneityAllowed = simultaneityAllowed;
    this.confidence = confidence;
    this.inferred = inferred;
    this.status = status;
    this.version = version;
  }

  // ---------------------------------------------------------------------------
  // Factory methods per type
  // ---------------------------------------------------------------------------

  /**
   * Create a COURSE_PASSED requirement: the student must have approved a specific course.
   *
   * @param requiredCourseId must not be null
   */
  public static CourseRequirement coursePassed(
      CourseRequirementId id,
      UUID targetCourseId,
      UUID requiredCourseId,
      boolean simultaneityAllowed,
      Confidence confidence,
      Boolean inferred) {
    requireNotNull(id, targetCourseId);
    if (requiredCourseId == null) {
      throw new IllegalArgumentException(
          "requiredCourseId must not be null for COURSE_PASSED requirement");
    }
    return new CourseRequirement(
        id,
        targetCourseId,
        RequirementType.COURSE_PASSED,
        requiredCourseId,
        null,
        simultaneityAllowed,
        confidence,
        inferred,
        CourseRequirementStatus.ACTIVE,
        0L);
  }

  /**
   * Create a SEQUENCE requirement: the course must be taken in a defined order.
   *
   * @param requiredCourseId must not be null
   */
  public static CourseRequirement sequence(
      CourseRequirementId id,
      UUID targetCourseId,
      UUID requiredCourseId,
      boolean simultaneityAllowed) {
    requireNotNull(id, targetCourseId);
    if (requiredCourseId == null) {
      throw new IllegalArgumentException(
          "requiredCourseId must not be null for SEQUENCE requirement");
    }
    return new CourseRequirement(
        id,
        targetCourseId,
        RequirementType.SEQUENCE,
        requiredCourseId,
        null,
        simultaneityAllowed,
        null,
        null,
        CourseRequirementStatus.ACTIVE,
        0L);
  }

  /**
   * Create a MINIMUM_CREDITS requirement: the student must have accumulated a minimum credit count.
   *
   * @param requiredCredits must not be null and must be ≥ 0
   */
  public static CourseRequirement minimumCredits(
      CourseRequirementId id, UUID targetCourseId, Integer requiredCredits) {
    requireNotNull(id, targetCourseId);
    if (requiredCredits == null) {
      throw new IllegalArgumentException(
          "requiredCredits must not be null for MINIMUM_CREDITS requirement");
    }
    if (requiredCredits < 0) {
      throw new IllegalArgumentException("requiredCredits must be >= 0, got: " + requiredCredits);
    }
    return new CourseRequirement(
        id,
        targetCourseId,
        RequirementType.MINIMUM_CREDITS,
        null,
        requiredCredits,
        null,
        null,
        null,
        CourseRequirementStatus.ACTIVE,
        0L);
  }

  /**
   * Create a CONFIGURABLE requirement: custom rule evaluated externally (future simulator logic).
   *
   * <p>Equivalent to {@link #configurable(CourseRequirementId, UUID, Integer)} with a {@code null}
   * threshold.
   */
  public static CourseRequirement configurable(CourseRequirementId id, UUID targetCourseId) {
    return configurable(id, targetCourseId, null);
  }

  /**
   * Create a CONFIGURABLE requirement carrying an externally-evaluated threshold in {@code
   * requiredCredits} (e.g. "N distinct passed pool members" for a pool-gate rule such as the
   * profundización thesis gate — design PROP-001 D1/F1). {@code requiredCredits} is nullable: a
   * CONFIGURABLE row with no threshold is still valid (custom rule with no numeric parameter).
   */
  public static CourseRequirement configurable(
      CourseRequirementId id, UUID targetCourseId, Integer requiredCredits) {
    requireNotNull(id, targetCourseId);
    return new CourseRequirement(
        id,
        targetCourseId,
        RequirementType.CONFIGURABLE,
        null,
        requiredCredits,
        null,
        null,
        null,
        CourseRequirementStatus.ACTIVE,
        0L);
  }

  /**
   * Reconstitute a CourseRequirement from persistence (all fields explicit; no default logic).
   *
   * <p>Enforces the same per-type invariants as the factory methods so that corrupt or incomplete
   * persistence data cannot produce an invalid aggregate in memory.
   */
  public static CourseRequirement reconstitute(
      CourseRequirementId id,
      UUID targetCourseId,
      RequirementType type,
      UUID requiredCourseId,
      Integer requiredCredits,
      Boolean simultaneityAllowed,
      Confidence confidence,
      Boolean inferred,
      CourseRequirementStatus status,
      long version) {
    Objects.requireNonNull(id, "id must not be null");
    if (targetCourseId == null)
      throw new IllegalArgumentException("targetCourseId must not be null");
    validatePerType(type, requiredCourseId, requiredCredits);
    return new CourseRequirement(
        id,
        targetCourseId,
        type,
        requiredCourseId,
        requiredCredits,
        simultaneityAllowed,
        confidence,
        inferred,
        status,
        version);
  }

  /**
   * Shared per-type validation used by both factory methods and reconstitute. Ensures that the
   * type-specific required fields are always present.
   */
  private static void validatePerType(
      RequirementType type, UUID requiredCourseId, Integer requiredCredits) {
    switch (type) {
      case COURSE_PASSED, SEQUENCE -> {
        if (requiredCourseId == null) {
          throw new IllegalArgumentException(
              "requiredCourseId must not be null for " + type + " requirement");
        }
      }
      case MINIMUM_CREDITS -> {
        if (requiredCredits == null) {
          throw new IllegalArgumentException(
              "requiredCredits must not be null for MINIMUM_CREDITS requirement");
        }
        if (requiredCredits < 0) {
          throw new IllegalArgumentException(
              "requiredCredits must be >= 0, got: " + requiredCredits);
        }
      }
      case CONFIGURABLE -> {
        /* no specific required field */
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Validation helpers
  // ---------------------------------------------------------------------------

  private static void requireNotNull(CourseRequirementId id, UUID targetCourseId) {
    Objects.requireNonNull(id, "id must not be null");
    if (targetCourseId == null) {
      throw new IllegalArgumentException("targetCourseId must not be null");
    }
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public CourseRequirementId id() {
    return id;
  }

  public UUID targetCourseId() {
    return targetCourseId;
  }

  public RequirementType type() {
    return type;
  }

  public UUID requiredCourseId() {
    return requiredCourseId;
  }

  public Integer requiredCredits() {
    return requiredCredits;
  }

  public Boolean simultaneityAllowed() {
    return simultaneityAllowed;
  }

  public Confidence confidence() {
    return confidence;
  }

  public Boolean inferred() {
    return inferred;
  }

  public CourseRequirementStatus status() {
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
    if (!(o instanceof CourseRequirement other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "CourseRequirement[id=%s, target=%s, type=%s, required=%s, confidence=%s, inferred=%s]"
        .formatted(id, targetCourseId, type, requiredCourseId, confidence, inferred);
  }
}
