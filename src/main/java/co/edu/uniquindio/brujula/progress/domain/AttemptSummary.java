package co.edu.uniquindio.brujula.progress.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Lightweight summary of a course attempt used by {@link ProgressSummaryCalculator}.
 *
 * <p>Contains only the data needed for credit/average calculation. No Spring/JPA imports (ARQ-002).
 *
 * @param courseId the course UUID
 * @param status declared attempt status
 * @param grade the grade (nullable; null for CANCELLED)
 * @param periodId opaque period UUID (nullable)
 */
public record AttemptSummary(
    UUID courseId,
    AttemptStatus status,
    BigDecimal grade,
    UUID periodId) {

  public AttemptSummary {
    Objects.requireNonNull(courseId, "courseId must not be null");
    Objects.requireNonNull(status, "status must not be null");
  }
}
