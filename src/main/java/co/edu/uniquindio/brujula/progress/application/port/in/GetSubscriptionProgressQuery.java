package co.edu.uniquindio.brujula.progress.application.port.in;

import co.edu.uniquindio.brujula.progress.application.dto.ProgressSummaryView;
import java.util.UUID;

/**
 * Input port: compute and return the progress summary for a subscription.
 *
 * <p>Implements: AVL-2 — bulk-loads attempts and plan courses, delegates to {@link
 * co.edu.uniquindio.brujula.progress.domain.ProgressSummaryCalculator}. BOLA-guarded. ARQ-003.
 *
 * <p>Returns {@link ProgressSummaryView} (application DTO) instead of a domain type, so REST
 * adapters can use the result without importing domain classes (ARQ-001).
 *
 * <p>No Spring/framework imports.
 */
public interface GetSubscriptionProgressQuery {

  /**
   * Return the progress summary for the given subscription.
   *
   * @param subscriptionId the subscription UUID
   * @param userId the authenticated student's user ID
   * @return the computed progress summary view
   * @throws co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException if not found or
   *     not owned by userId
   */
  ProgressSummaryView getSubscriptionProgress(UUID subscriptionId, UUID userId);
}
