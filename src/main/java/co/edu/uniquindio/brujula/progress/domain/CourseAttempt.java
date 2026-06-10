package co.edu.uniquindio.brujula.progress.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * CourseAttempt — aggregate root representing a student's declared attempt at a course.
 *
 * <p>Design: ADR-F3-002. Multiple attempts per (subscriptionId, courseId) are allowed. Grade↔status
 * coherence enforced on creation and update:
 *
 * <ul>
 *   <li>PASSED: grade required, must be in [3.0, 5.0].
 *   <li>FAILED: grade required, must be in [0.0, 2.9].
 *   <li>CANCELLED: grade must be null.
 * </ul>
 *
 * <p>periodId is opaque (ADR-F3-009); stored as-is without cross-module validation. No Spring/JPA
 * imports (ARQ-002).
 */
public final class CourseAttempt {

  private final AttemptId id;
  private final SubscriptionId subscriptionId;
  private final UUID courseId;
  private AttemptStatus status;
  private Grade grade; // nullable; null for CANCELLED
  private UUID periodId; // opaque; contract-required non-null (FIX-W4; ADR-F3-009 deferred)
  private String journeyCode;
  private final Instant registeredAt;
  private final long version;

  private CourseAttempt(
      AttemptId id,
      SubscriptionId subscriptionId,
      UUID courseId,
      AttemptStatus status,
      Grade grade,
      UUID periodId,
      String journeyCode,
      Instant registeredAt,
      long version) {
    this.id = id;
    this.subscriptionId = subscriptionId;
    this.courseId = courseId;
    this.status = status;
    this.grade = grade;
    this.periodId = periodId;
    this.journeyCode = journeyCode;
    this.registeredAt = registeredAt;
    this.version = version;
  }

  // ---------------------------------------------------------------------------
  // Factory — new attempt
  // ---------------------------------------------------------------------------

  /**
   * Register a new course attempt.
   *
   * <p>FIX-W4: {@code periodId} is contract-REQUIRED (non-null). The OpenAPI schema {@code
   * CourseAttempt} and {@code CourseAttemptCreateRequest} both list {@code periodId} in {@code
   * required}. The domain now enforces this — null is rejected.
   *
   * @param id generated attempt ID
   * @param subscriptionId owning subscription
   * @param courseId course being attempted
   * @param status declared outcome
   * @param grade nullable; must comply with grade-status coherence rules
   * @param periodId opaque period UUID — REQUIRED, non-null (ADR-F3-009 deferred; contract wins)
   * @param journeyCode journey code (non-blank)
   * @return the new CourseAttempt with version=0
   * @throws NullPointerException if periodId is null
   * @throws GradeStatusMismatchException if grade↔status coherence is violated
   */
  public static CourseAttempt register(
      AttemptId id,
      SubscriptionId subscriptionId,
      UUID courseId,
      AttemptStatus status,
      Grade grade,
      UUID periodId,
      String journeyCode) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
    Objects.requireNonNull(courseId, "courseId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(
        periodId, "periodId must not be null (contract-required; ADR-F3-009 deferred)");
    requireNonBlankJourney(journeyCode);
    validateGradeStatusCoherence(status, grade);

    return new CourseAttempt(
        id, subscriptionId, courseId, status, grade, periodId, journeyCode, Instant.now(), 0L);
  }

  // ---------------------------------------------------------------------------
  // Reconstitution — from persistence
  // ---------------------------------------------------------------------------

  /** Reconstitute a CourseAttempt from persistence (all fields explicit, no default logic). */
  public static CourseAttempt reconstitute(
      AttemptId id,
      SubscriptionId subscriptionId,
      UUID courseId,
      AttemptStatus status,
      Grade grade,
      UUID periodId,
      String journeyCode,
      Instant registeredAt,
      long version) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
    Objects.requireNonNull(courseId, "courseId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(registeredAt, "registeredAt must not be null");
    requireNonBlankJourney(journeyCode);
    return new CourseAttempt(
        id, subscriptionId, courseId, status, grade, periodId, journeyCode, registeredAt, version);
  }

  // ---------------------------------------------------------------------------
  // Business behaviors
  // ---------------------------------------------------------------------------

  /**
   * Update the attempt's status and grade.
   *
   * @param newStatus new declared outcome
   * @param newGrade new grade (nullable; must comply with coherence rules)
   * @param newPeriodId updated opaque period ID (nullable; pass null to keep existing)
   * @param newJourneyCode updated journey code (nullable; pass null to keep existing)
   * @throws GradeStatusMismatchException if grade↔status coherence is violated
   */
  public void update(
      AttemptStatus newStatus, Grade newGrade, UUID newPeriodId, String newJourneyCode) {
    Objects.requireNonNull(newStatus, "newStatus must not be null");
    validateGradeStatusCoherence(newStatus, newGrade);

    this.status = newStatus;
    this.grade = newGrade;
    if (newPeriodId != null) {
      this.periodId = newPeriodId;
    }
    if (newJourneyCode != null && !newJourneyCode.isBlank()) {
      this.journeyCode = newJourneyCode;
    }
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public AttemptId getId() {
    return id;
  }

  public SubscriptionId getSubscriptionId() {
    return subscriptionId;
  }

  public UUID getCourseId() {
    return courseId;
  }

  public AttemptStatus getStatus() {
    return status;
  }

  public Grade getGrade() {
    return grade;
  }

  public UUID getPeriodId() {
    return periodId;
  }

  public String getJourneyCode() {
    return journeyCode;
  }

  public Instant getRegisteredAt() {
    return registeredAt;
  }

  public long getVersion() {
    return version;
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private static void validateGradeStatusCoherence(AttemptStatus status, Grade grade) {
    switch (status) {
      case PASSED -> {
        if (grade == null) {
          throw new GradeStatusMismatchException(
              status, "PASSED requires a non-null grade in [3.0, 5.0] but grade was null");
        }
        if (!grade.passing()) {
          throw new GradeStatusMismatchException(
              status, "PASSED requires grade >= 3.0 but was " + grade.value());
        }
      }
      case FAILED -> {
        if (grade == null) {
          throw new GradeStatusMismatchException(
              status, "FAILED requires a non-null grade in [0.0, 2.9] but grade was null");
        }
        if (grade.passing()) {
          throw new GradeStatusMismatchException(
              status, "FAILED requires grade < 3.0 but was " + grade.value());
        }
      }
      case CANCELLED -> {
        if (grade != null) {
          throw new GradeStatusMismatchException(
              status, "CANCELLED must not have a grade but received " + grade.value());
        }
      }
        // Exhaustive: PASSED, FAILED, CANCELLED are all contract values (FIX-W2: PENDING removed)
    }
  }

  private static void requireNonBlankJourney(String journeyCode) {
    if (journeyCode == null || journeyCode.isBlank()) {
      throw new IllegalArgumentException("journeyCode must not be blank");
    }
  }

  // ---------------------------------------------------------------------------
  // Equality (identity-based)
  // ---------------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CourseAttempt other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "CourseAttempt[id=%s, subscriptionId=%s, courseId=%s, status=%s, version=%d]"
        .formatted(id, subscriptionId, courseId, status, version);
  }
}
