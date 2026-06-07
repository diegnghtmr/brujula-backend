package co.edu.uniquindio.brujula.progress.application.dto;

import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Application-layer DTO for a CourseAttempt (returned by use cases, serialized for idempotency).
 *
 * <p>Includes courseName for the REST response contract. ARQ-001: no domain types except enums
 * (application layer is allowed to reference domain enums; infrastructure/REST controllers use
 * {@link #statusName()} to avoid a direct domain dependency per ARQ-001).
 *
 * <p>TODO(WU4): FIX-S1 — replace the {@code AttemptStatus status} component with a plain
 * {@code String statusName} to remove the domain-type reference entirely from this application DTO.
 * Services would pass {@code status.name()}, callers would use {@code statusName()} directly.
 * This ripples through CreateCourseAttemptService, UpdateCourseAttemptService,
 * GetCourseAttemptService, the controller, and all related tests (more than 3 files total) — defer
 * to WU4 to avoid destabilizing the WU3 fix batch (FIX-S1 risk gate per verify report).
 *
 * @param id attempt UUID
 * @param subscriptionId owning subscription UUID
 * @param courseId course UUID
 * @param courseName human-readable course name (placeholder "N/A" until WU4 resolves)
 * @param periodId opaque period UUID (nullable)
 * @param periodName opaque period name (nullable)
 * @param journeyCode journey code (e.g. DIURNA)
 * @param status declared attempt status
 * @param finalGrade nullable grade
 * @param registeredAt creation timestamp
 * @param version optimistic lock version
 */
public record CourseAttemptView(
    UUID id,
    UUID subscriptionId,
    UUID courseId,
    String courseName,
    UUID periodId,
    String periodName,
    String journeyCode,
    AttemptStatus status,
    BigDecimal finalGrade,
    Instant registeredAt,
    long version) {

  /**
   * Returns the status name as a plain String.
   *
   * <p>REST adapters MUST use this method instead of {@code status().name()} to avoid a direct
   * domain dependency that would violate ARQ-001.
   */
  public String statusName() {
    return status != null ? status.name() : null;
  }
}
