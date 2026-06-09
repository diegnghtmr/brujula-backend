package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Course — aggregate root of the curriculum module's course bounded context.
 *
 * <p>Represents a single course within a study plan. Invariants enforced via factory methods:
 *
 * <ul>
 *   <li>{@code credits} must be ≥ 0 (a 0-credit administrative course is allowed per CUR-001).
 *   <li>{@code name} must not be null or blank.
 *   <li>{@code recommendedSemesters} list must not be null (may be empty).
 *   <li>{@code planId} and {@code programId} must not be null.
 * </ul>
 *
 * <p>Status defaults to {@link CourseStatus#ACTIVE}. Version initializes to {@code 0}.
 * {@code officialCode} is null on creation and set by {@link OfficialCodeMinter} at seed time
 * (ADR-F2-005). UUIDs remain the primary key (RN-024/025).
 *
 * <p>No Spring/JPA imports (ARQ-002).
 */
public final class Course {

  private final CourseId id;
  private final UUID planId;
  private final UUID programId;
  private String officialCode; // nullable; minted once at seed, immutable after set
  private final String name;
  private final CourseType type;
  private final int credits;
  private final int curricularWeeklyHours;
  private final List<RecommendedSemester> recommendedSemesters;
  private final CourseStatus status;
  private final long version;

  private Course(
      CourseId id,
      UUID planId,
      UUID programId,
      String officialCode,
      String name,
      CourseType type,
      int credits,
      int curricularWeeklyHours,
      List<RecommendedSemester> recommendedSemesters,
      CourseStatus status,
      long version) {
    this.id = id;
    this.planId = planId;
    this.programId = programId;
    this.officialCode = officialCode;
    this.name = name;
    this.type = type;
    this.credits = credits;
    this.curricularWeeklyHours = curricularWeeklyHours;
    this.recommendedSemesters = Collections.unmodifiableList(recommendedSemesters);
    this.status = status;
    this.version = version;
  }

  // ---------------------------------------------------------------------------
  // Factory: new course creation
  // ---------------------------------------------------------------------------

  /**
   * Create a new Course. OfficialCode defaults to null (set later by minter).
   * Status defaults to ACTIVE. Version initializes to 0.
   */
  public static Course create(
      CourseId id,
      UUID planId,
      UUID programId,
      String name,
      CourseType type,
      int credits,
      int curricularWeeklyHours,
      List<RecommendedSemester> recommendedSemesters) {
    validate(id, planId, programId, name, credits, recommendedSemesters);
    return new Course(
        id,
        planId,
        programId,
        null,
        name,
        type,
        credits,
        curricularWeeklyHours,
        List.copyOf(recommendedSemesters),
        CourseStatus.ACTIVE,
        0L);
  }

  /**
   * Reconstitute a Course from persistence (all fields explicit; no default logic).
   */
  public static Course reconstitute(
      CourseId id,
      UUID planId,
      UUID programId,
      String officialCode,
      String name,
      CourseType type,
      int credits,
      int curricularWeeklyHours,
      List<RecommendedSemester> recommendedSemesters,
      CourseStatus status,
      long version) {
    validate(id, planId, programId, name, credits, recommendedSemesters);
    return new Course(
        id,
        planId,
        programId,
        officialCode,
        name,
        type,
        credits,
        curricularWeeklyHours,
        List.copyOf(recommendedSemesters),
        status,
        version);
  }

  // ---------------------------------------------------------------------------
  // Write methods
  // ---------------------------------------------------------------------------

  /**
   * Update course fields. Returns a new immutable Course instance with updated values.
   * The id, planId, programId, officialCode, status, and version are preserved from the original.
   *
   * @param name the new name (must not be blank)
   * @param type the new course type
   * @param credits the new credit count (must be &gt;= 0)
   * @param curricularWeeklyHours the new weekly hours
   * @param recommendedSemesters the new recommended semester list
   * @return a new Course instance with the updated fields
   */
  public Course update(
      String name,
      CourseType type,
      int credits,
      int curricularWeeklyHours,
      List<RecommendedSemester> recommendedSemesters) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be null or blank");
    }
    if (credits < 0) {
      throw new IllegalArgumentException("credits must be >= 0, got: " + credits);
    }
    return new Course(
        this.id, this.planId, this.programId, this.officialCode,
        name, type, credits, curricularWeeklyHours,
        recommendedSemesters != null ? List.copyOf(recommendedSemesters) : this.recommendedSemesters,
        this.status, this.version);
  }

  /**
   * Archive this course (soft-delete). Returns a new Course instance with status ARCHIVED.
   *
   * @throws IllegalStateException if the course is already ARCHIVED
   */
  public Course archive() {
    if (this.status == CourseStatus.ARCHIVED) {
      throw new IllegalStateException("Course is already archived");
    }
    return new Course(
        this.id, this.planId, this.programId, this.officialCode,
        this.name, this.type, this.credits, this.curricularWeeklyHours,
        this.recommendedSemesters, CourseStatus.ARCHIVED, this.version);
  }

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  private static void validate(
      CourseId id,
      UUID planId,
      UUID programId,
      String name,
      int credits,
      List<RecommendedSemester> recommendedSemesters) {
    Objects.requireNonNull(id, "id must not be null");
    if (planId == null) throw new IllegalArgumentException("planId must not be null");
    if (programId == null) throw new IllegalArgumentException("programId must not be null");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be null or blank");
    }
    if (credits < 0) {
      throw new IllegalArgumentException("credits must be >= 0, got: " + credits);
    }
    if (recommendedSemesters == null) {
      throw new IllegalArgumentException("recommendedSemesters must not be null");
    }
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public CourseId id() {
    return id;
  }

  public UUID planId() {
    return planId;
  }

  public UUID programId() {
    return programId;
  }

  public String officialCode() {
    return officialCode;
  }

  public String name() {
    return name;
  }

  public CourseType type() {
    return type;
  }

  public int credits() {
    return credits;
  }

  public int curricularWeeklyHours() {
    return curricularWeeklyHours;
  }

  public List<RecommendedSemester> recommendedSemesters() {
    return recommendedSemesters;
  }

  public CourseStatus status() {
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
    if (!(o instanceof Course other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Course[id=%s, officialCode=%s, name=%s, type=%s, credits=%d, status=%s]"
        .formatted(id, officialCode, name, type, credits, status);
  }
}
