package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.port.in.DeleteSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.LastActiveSubscriptionException;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import java.util.UUID;

/**
 * Use case: soft-delete a subscription.
 *
 * <p>Rejects deletion if it would leave the student with zero active subscriptions. Course attempts
 * are retained as historical data.
 *
 * <p>Framework-free.
 */
public class DeleteSubscriptionService implements DeleteSubscriptionUseCase {

  private final LoadSubscriptionPort loadSubscriptionPort;
  private final SaveSubscriptionPort saveSubscriptionPort;
  private final AuditRecorder auditRecorder;

  public DeleteSubscriptionService(
      LoadSubscriptionPort loadSubscriptionPort,
      SaveSubscriptionPort saveSubscriptionPort,
      AuditRecorder auditRecorder) {
    this.loadSubscriptionPort = loadSubscriptionPort;
    this.saveSubscriptionPort = saveSubscriptionPort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public void deleteSubscription(UUID subscriptionId, UUID userId) {
    Subscription subscription =
        loadSubscriptionPort
            .loadById(subscriptionId)
            .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

    // BOLA ownership guard
    if (!subscription.getUserId().equals(userId)) {
      throw new SubscriptionNotFoundException(subscriptionId);
    }

    // Guard: must retain at least one active subscription
    int activeCount = loadSubscriptionPort.countActiveByUserId(userId);
    if (activeCount <= 1) {
      throw new LastActiveSubscriptionException(userId);
    }

    // Soft-delete: attempts are NOT deleted
    subscription.softDelete();
    saveSubscriptionPort.save(subscription); // return value not needed for delete

    // Audit (SUBSCRIPTION_REMOVED)
    auditRecorder.record(
        new RecordAuditEventCommand(
            userId,
            "STUDENT",
            "SUBSCRIPTION_REMOVED",
            "subscription",
            subscriptionId,
            null,
            null,
            null));
  }
}
