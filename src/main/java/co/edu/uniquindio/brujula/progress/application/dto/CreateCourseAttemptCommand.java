package co.edu.uniquindio.brujula.progress.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to create a new course attempt.
 *
 * <p>Status is passed as a plain String (the enum name) so that REST controllers do not need to
 * import domain types — the use case performs {@code AttemptStatus.valueOf(statusName)} when
 * constructing the domain object.
 *
 * @param idempotencyKey UUID from the Idempotency-Key header (required)
 * @param actorId authenticated student's user ID
 * @param subscriptionId owning subscription UUID (BOLA validated in use case)
 * @param courseId course being attempted
 * @param periodId opaque period UUID (REQUIRED, non-null; contract-required per OpenAPI schema)
 * @param journeyCode journey code (non-blank)
 * @param statusName declared attempt status name (e.g. "PASSED", "FAILED", "CANCELLED")
 * @param finalGrade nullable grade (coherence with status validated in domain)
 * @param requestHash SHA-256 hash of the request body for idempotency
 */
public record CreateCourseAttemptCommand(
    UUID idempotencyKey,
    UUID actorId,
    UUID subscriptionId,
    UUID courseId,
    UUID periodId,
    String journeyCode,
    String statusName,
    BigDecimal finalGrade,
    String requestHash) {}
