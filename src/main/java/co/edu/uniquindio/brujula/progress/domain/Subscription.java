package co.edu.uniquindio.brujula.progress.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Subscription — aggregate root representing a student's enrollment in a program/plan.
 *
 * <p>Design: ADR-F3-002. Contract alignment (#1630): NO primary flag, cap=2 enforced at application
 * level.
 *
 * <p>No Spring/JPA imports (ARQ-002). Framework-free pure domain object.
 */
public final class Subscription {

  private final SubscriptionId id;
  private final UUID userId;
  private final UUID programId;
  private UUID planId;
  private String referenceJourneyCode;
  private SubscriptionStatus status;
  private final Instant createdAt;
  private final long version;

  private Subscription(
      SubscriptionId id,
      UUID userId,
      UUID programId,
      UUID planId,
      String referenceJourneyCode,
      SubscriptionStatus status,
      Instant createdAt,
      long version) {
    this.id = id;
    this.userId = userId;
    this.programId = programId;
    this.planId = planId;
    this.referenceJourneyCode = referenceJourneyCode;
    this.status = status;
    this.createdAt = createdAt;
    this.version = version;
  }

  // ---------------------------------------------------------------------------
  // Factory
  // ---------------------------------------------------------------------------

  /**
   * Create a new subscription.
   *
   * @param id generated subscription ID
   * @param userId the student user ID (non-null)
   * @param programId program being subscribed to (non-null)
   * @param planId active study plan (non-null)
   * @param referenceJourneyCode non-blank journey code (e.g. DIURNA, NOCTURNA)
   */
  public static Subscription create(
      SubscriptionId id, UUID userId, UUID programId, UUID planId, String referenceJourneyCode) {
    Objects.requireNonNull(id, "id must not be null");
    if (userId == null) throw new IllegalArgumentException("userId must not be null");
    if (programId == null) throw new IllegalArgumentException("programId must not be null");
    if (planId == null) throw new IllegalArgumentException("planId must not be null");
    requireNonBlankJourney(referenceJourneyCode);

    return new Subscription(
        id,
        userId,
        programId,
        planId,
        referenceJourneyCode,
        SubscriptionStatus.ACTIVE,
        Instant.now(),
        0L);
  }

  /** Reconstitute a Subscription from persistence (all fields explicit, no default logic). */
  public static Subscription reconstitute(
      SubscriptionId id,
      UUID userId,
      UUID programId,
      UUID planId,
      String referenceJourneyCode,
      SubscriptionStatus status,
      Instant createdAt,
      long version) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(programId, "programId must not be null");
    Objects.requireNonNull(planId, "planId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    requireNonBlankJourney(referenceJourneyCode);

    return new Subscription(
        id, userId, programId, planId, referenceJourneyCode, status, createdAt, version);
  }

  // ---------------------------------------------------------------------------
  // Business behaviors
  // ---------------------------------------------------------------------------

  /**
   * Soft-delete this subscription. Idempotent: no-op if already DELETED.
   *
   * <p>Design: ADR-F3-003. Attempts are NOT deleted; they remain as historical data.
   */
  public void softDelete() {
    this.status = SubscriptionStatus.DELETED;
  }

  /**
   * Update the reference journey code.
   *
   * @param newJourneyCode non-blank
   */
  public void updateReferenceJourney(String newJourneyCode) {
    requireNonBlankJourney(newJourneyCode);
    this.referenceJourneyCode = newJourneyCode;
  }

  /**
   * Change the plan (and optionally the journey) of this subscription.
   *
   * <p>If {@code hasAttempts=true} and {@code acknowledgeHistoryImpact=false}, the change is
   * rejected with {@link HistoryImpactNotAcknowledgedException}. This gate ensures students
   * understand that existing course attempts may be reclassified as off-plan (ADR-F3-007).
   *
   * @param newPlanId target plan ID (non-null)
   * @param newJourneyCode target journey (non-blank)
   * @param hasAttempts true if the subscription has associated course-attempt records
   * @param acknowledgeHistoryImpact true if the student explicitly acknowledged the impact
   */
  public void changePlan(
      UUID newPlanId,
      String newJourneyCode,
      boolean hasAttempts,
      boolean acknowledgeHistoryImpact) {
    Objects.requireNonNull(newPlanId, "newPlanId must not be null");
    requireNonBlankJourney(newJourneyCode);

    if (hasAttempts && !acknowledgeHistoryImpact) {
      throw new HistoryImpactNotAcknowledgedException(this.id);
    }

    this.planId = newPlanId;
    this.referenceJourneyCode = newJourneyCode;
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public SubscriptionId getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getProgramId() {
    return programId;
  }

  public UUID getPlanId() {
    return planId;
  }

  public String getReferenceJourneyCode() {
    return referenceJourneyCode;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public long getVersion() {
    return version;
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private static void requireNonBlankJourney(String journeyCode) {
    if (journeyCode == null || journeyCode.isBlank()) {
      throw new IllegalArgumentException("referenceJourneyCode must not be blank");
    }
  }

  // ---------------------------------------------------------------------------
  // Equality (identity-based)
  // ---------------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Subscription other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Subscription[id=%s, userId=%s, programId=%s, planId=%s, journey=%s, status=%s, version=%d]"
        .formatted(id, userId, programId, planId, referenceJourneyCode, status, version);
  }
}
