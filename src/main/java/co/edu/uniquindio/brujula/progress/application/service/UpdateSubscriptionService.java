package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.dto.UpdateSubscriptionCommand;
import co.edu.uniquindio.brujula.progress.application.port.in.UpdateSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;

/**
 * Use case: update a subscription's plan and/or journey.
 *
 * <p>Enforces: BOLA ownership guard, acknowledgeHistoryImpact gate (ADR-F3-007), optimistic locking
 * (handled by JPA @Version → OptimisticLockingFailureException → 409 in handler).
 *
 * <p>WU3-SEAM: attempt reclassification (PlanChangeReclassifier) is wired in WU3 when
 * CourseAttemptPersistenceAdapter and the full attempt domain are available. For WU2, the
 * subscription side is updated; attempts are not remapped yet.
 *
 * <p>Satisfies: SUB-3, CROSS-3, CROSS-4. Framework-free.
 */
public class UpdateSubscriptionService implements UpdateSubscriptionUseCase {

  private final LoadSubscriptionPort loadSubscriptionPort;
  private final SaveSubscriptionPort saveSubscriptionPort;
  private final AuditRecorder auditRecorder;

  public UpdateSubscriptionService(
      LoadSubscriptionPort loadSubscriptionPort,
      SaveSubscriptionPort saveSubscriptionPort,
      AuditRecorder auditRecorder) {
    this.loadSubscriptionPort = loadSubscriptionPort;
    this.saveSubscriptionPort = saveSubscriptionPort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public SubscriptionView updateSubscription(UpdateSubscriptionCommand command) {
    Subscription subscription =
        loadSubscriptionPort
            .loadById(command.subscriptionId())
            .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));

    // BOLA ownership guard
    if (!subscription.getUserId().equals(command.actorId())) {
      throw new SubscriptionNotFoundException(command.subscriptionId());
    }

    // Determine if a plan change is requested
    boolean isPlanChange =
        command.newPlanId() != null && !command.newPlanId().equals(subscription.getPlanId());

    if (isPlanChange) {
      // Check for existing attempts to determine ack requirement
      boolean hasAttempts = loadSubscriptionPort.hasAttempts(command.subscriptionId());
      String newJourney =
          command.newReferenceJourneyCode() != null
              ? command.newReferenceJourneyCode()
              : subscription.getReferenceJourneyCode();

      subscription.changePlan(
          command.newPlanId(), newJourney, hasAttempts, command.acknowledgeHistoryImpact());

      // WU3-SEAM: invoke PlanChangeReclassifier here once WU3 is merged.
      // The reclassifier will load all attempts for this subscription, determine which courseIds
      // exist in the new plan, and flag the rest as off-plan (computed, not stored in the DB).

    } else if (command.newReferenceJourneyCode() != null) {
      subscription.updateReferenceJourney(command.newReferenceJourneyCode());
    }

    // Use the RETURNED domain object — it carries the DB-incremented version (FIX-W4).
    Subscription saved = saveSubscriptionPort.save(subscription);

    // Audit (CROSS-4: SUBSCRIPTION_UPDATED)
    auditRecorder.record(
        new RecordAuditEventCommand(
            command.actorId(),
            "STUDENT",
            "SUBSCRIPTION_UPDATED",
            "subscription",
            command.subscriptionId(),
            null,
            null,
            null));

    return toView(saved);
  }

  private SubscriptionView toView(Subscription sub) {
    return new SubscriptionView(
        sub.getId().value(),
        sub.getProgramId(),
        null,
        sub.getPlanId(),
        null,
        sub.getReferenceJourneyCode(),
        null,
        sub.getVersion(),
        sub.getCreatedAt());
  }
}
