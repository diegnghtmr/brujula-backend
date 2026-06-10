package co.edu.uniquindio.brujula.progress.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to update an existing course attempt.
 *
 * <p>Self-validating record (ARQ-001). Status is passed as a plain String (the enum name) so that
 * REST controllers do not need to import domain types — the use case performs {@code
 * AttemptStatus.valueOf(statusName)} when constructing the domain object.
 *
 * @param attemptId attempt UUID to update
 * @param actorId authenticated student's user ID (BOLA validated in use case)
 * @param statusName new declared status name (e.g. "PASSED", "FAILED", "CANCELLED")
 * @param finalGrade nullable grade (coherence with status validated in domain)
 * @param periodId updated opaque period UUID (nullable)
 * @param journeyCode updated journey code (nullable; null = keep existing)
 * @param version optimistic lock version
 */
public record UpdateCourseAttemptCommand(
    UUID attemptId,
    UUID actorId,
    String statusName,
    BigDecimal finalGrade,
    UUID periodId,
    String journeyCode,
    long version) {}
