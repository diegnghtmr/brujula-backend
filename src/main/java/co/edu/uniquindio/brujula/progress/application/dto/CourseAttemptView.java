package co.edu.uniquindio.brujula.progress.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Application-layer DTO for a CourseAttempt (returned by use cases, serialized for idempotency).
 *
 * <p>Includes courseName for the REST response contract. ARQ-001: no domain types in application
 * DTOs — status is expressed as a plain {@code String} (the enum name). Services pass {@code
 * attempt.getStatus().name()}.
 *
 * <p>FIX-S1 (WU4): replaced domain {@code AttemptStatus status} component with plain {@code String
 * statusName} so that this DTO has zero domain-type references.
 *
 * @param id attempt UUID
 * @param subscriptionId owning subscription UUID
 * @param courseId course UUID
 * @param courseName human-readable course name (placeholder "N/A" until catalog enrichment)
 * @param periodId opaque period UUID (nullable)
 * @param periodName opaque period name (nullable)
 * @param journeyCode journey code (e.g. DIURNA)
 * @param statusName declared attempt status name (e.g. "PASSED", "FAILED", "CANCELLED")
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
    String statusName,
    BigDecimal finalGrade,
    Instant registeredAt,
    long version) {}
