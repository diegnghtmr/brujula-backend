package co.edu.uniquindio.brujula.progress.application.port.out;

import co.edu.uniquindio.brujula.progress.domain.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: read-side operations for {@link Subscription}.
 *
 * <p>ARQ-003: application layer depends on this interface, not on concrete persistence adapters.
 */
public interface LoadSubscriptionPort {

  /**
   * Load a subscription by its ID (regardless of status).
   *
   * @param subscriptionId the subscription UUID
   * @return the subscription if found, empty otherwise
   */
  Optional<Subscription> loadById(UUID subscriptionId);

  /**
   * Load all ACTIVE subscriptions belonging to a student.
   *
   * @param userId the student user ID
   * @return list of active subscriptions (may be empty)
   */
  List<Subscription> loadActiveByUserId(UUID userId);

  /**
   * Count the number of ACTIVE subscriptions for a student (used for cap=2 enforcement).
   *
   * @param userId the student user ID
   * @return count of active subscriptions
   */
  int countActiveByUserId(UUID userId);

  /**
   * Check whether the student already has an active subscription for a specific program.
   *
   * @param userId the student user ID
   * @param programId the program to check
   * @return true if an active subscription for that program exists
   */
  boolean existsActiveByUserIdAndProgramId(UUID userId, UUID programId);

  /**
   * Check whether the subscription has any associated course attempt records.
   *
   * <p>Used by the plan-change gate to determine if acknowledgeHistoryImpact is required
   * (ADR-F3-007).
   *
   * @param subscriptionId the subscription UUID
   * @return true if at least one course attempt exists for this subscription
   */
  boolean hasAttempts(UUID subscriptionId);

  /**
   * Acquire a row-level write lock on all ACTIVE subscriptions of a student and return them.
   *
   * <p>FIX-W1: must be called (and the result discarded) at the START of the create-subscription
   * transaction, BEFORE counting. This serialises concurrent create requests per-student and
   * prevents the TOCTOU race that allows cap=2 to be exceeded.
   *
   * @param userId the student user ID
   * @return the locked active subscriptions (count usable for cap check)
   */
  List<Subscription> lockActiveByUserIdForUpdate(UUID userId);
}
