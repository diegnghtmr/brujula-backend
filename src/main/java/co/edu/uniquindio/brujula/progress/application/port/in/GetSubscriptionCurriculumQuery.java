package co.edu.uniquindio.brujula.progress.application.port.in;

import co.edu.uniquindio.brujula.progress.application.dto.CurriculumViewData;
import java.util.UUID;

/**
 * Input port: retrieve the curriculum view with availability computation for a subscription.
 *
 * <p>Implements: AVL-1 — bulk-loads plan malla via curriculum published port, resolves effective
 * attempt statuses, computes per-course availability (PASSED/BLOCKED/AVAILABLE/FAILED/CANCELLED),
 * maps to the contract {@code CurriculumView} schema. BOLA-guarded (ownership verified).
 *
 * <p>No Spring/framework imports.
 */
public interface GetSubscriptionCurriculumQuery {

  /**
   * Return the curriculum view for the given subscription.
   *
   * @param subscriptionId the subscription UUID
   * @param userId the authenticated student's user ID
   * @return the curriculum view with computed availability per course
   * @throws co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException if not found or
   *     not owned by userId
   */
  CurriculumViewData getSubscriptionCurriculum(UUID subscriptionId, UUID userId);
}
