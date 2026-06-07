package co.edu.uniquindio.brujula.progress.domain;

import java.util.UUID;

/**
 * Thrown when a student attempts to subscribe to a plan that is not ACTIVE (e.g. ARCHIVED or DRAFT).
 *
 * <p>Spec: SUB-1-INACTIVE-PLAN — "The referenced plan MUST be ACTIVE; a ARCHIVED plan rejected
 * with HTTP 422."
 *
 * <p>Maps to HTTP 422 Unprocessable Entity via {@link
 * co.edu.uniquindio.brujula.progress.infrastructure.configuration.ProgressExceptionHandler}.
 *
 * <p>No Spring/JPA imports (ARQ-002). Framework-free domain exception.
 */
public class InactivePlanException extends RuntimeException {

  private final UUID planId;
  private final String planStatus;

  public InactivePlanException(UUID planId, String planStatus) {
    super(
        "Study plan %s is not ACTIVE (status=%s); only ACTIVE plans may be subscribed to."
            .formatted(planId, planStatus));
    this.planId = planId;
    this.planStatus = planStatus;
  }

  public UUID getPlanId() {
    return planId;
  }

  public String getPlanStatus() {
    return planStatus;
  }
}
