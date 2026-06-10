package co.edu.uniquindio.brujula.progress.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.dto.CreateSubscriptionCommand;
import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.ValidatePlanStatusPort;
import co.edu.uniquindio.brujula.progress.domain.DuplicateProgramSubscriptionException;
import co.edu.uniquindio.brujula.progress.domain.InactivePlanException;
import co.edu.uniquindio.brujula.progress.domain.MaxSubscriptionsExceededException;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.util.Optional;

/**
 * Use case: create a new subscription for the authenticated student.
 *
 * <p>Enforces: cap=2 active subscriptions (contract #1630), duplicate-program guard, idempotency
 * via sharedkernel {@link IdempotencyStorePort}, audit via {@link AuditRecorder} (CROSS-4).
 *
 * <p>Framework-free — wired by {@code ProgressConfiguration}. Satisfies: SUB-1, CROSS-1, CROSS-4.
 */
public class CreateSubscriptionService implements CreateSubscriptionUseCase {

  public static final String OPERATION = "CREATE_SUBSCRIPTION";
  private static final int MAX_ACTIVE_SUBSCRIPTIONS = 2;

  private final LoadSubscriptionPort loadSubscriptionPort;
  private final SaveSubscriptionPort saveSubscriptionPort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;
  private final AuditRecorder auditRecorder;
  private final ValidatePlanStatusPort validatePlanStatusPort;

  public CreateSubscriptionService(
      LoadSubscriptionPort loadSubscriptionPort,
      SaveSubscriptionPort saveSubscriptionPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder,
      ValidatePlanStatusPort validatePlanStatusPort) {
    this.loadSubscriptionPort = loadSubscriptionPort;
    this.saveSubscriptionPort = saveSubscriptionPort;
    this.idempotencyStorePort = idempotencyStorePort;
    this.jsonCodecPort = jsonCodecPort;
    this.auditRecorder = auditRecorder;
    this.validatePlanStatusPort = validatePlanStatusPort;
  }

  @Override
  public SubscriptionView createSubscription(CreateSubscriptionCommand command) {
    // 1. Idempotency check
    Optional<IdempotencyStorePort.StoredRecord> existing =
        idempotencyStorePort.find(command.idempotencyKey(), OPERATION);
    if (existing.isPresent()) {
      IdempotencyStorePort.StoredRecord stored = existing.get();
      if (!stored.requestHash().equals(command.requestHash())) {
        throw new IdempotencyKeyConflictException(command.idempotencyKey(), OPERATION);
      }
      return jsonCodecPort.fromJson(stored.responseBodyJson(), SubscriptionView.class);
    }

    // 2. Cap=2 enforcement (contract #1630).
    // FIX-W1: acquire a row-level write lock on existing active subscriptions BEFORE counting.
    // This serialises concurrent creates per-student and prevents the TOCTOU race.
    int activeCount = loadSubscriptionPort.lockActiveByUserIdForUpdate(command.actorId()).size();
    if (activeCount >= MAX_ACTIVE_SUBSCRIPTIONS) {
      throw new MaxSubscriptionsExceededException(command.actorId(), activeCount);
    }

    // 3. Duplicate-program guard
    if (loadSubscriptionPort.existsActiveByUserIdAndProgramId(
        command.actorId(), command.programId())) {
      throw new DuplicateProgramSubscriptionException(command.actorId(), command.programId());
    }

    // 3b. FIX-W3 (SUB-1-INACTIVE-PLAN): validate the plan is ACTIVE via anti-corruption facade.
    // ValidatePlanStatusPort wraps GetStudyPlanQuery from programcatalog without crossing the
    // Spring Modulith named-interface boundary from the application layer (ARQ-006b).
    String planStatus = validatePlanStatusPort.getPlanStatus(command.planId());
    if (!"ACTIVE".equalsIgnoreCase(planStatus)) {
      throw new InactivePlanException(command.planId(), planStatus);
    }

    // 4. Create and persist subscription
    SubscriptionId id = SubscriptionId.generate();
    Subscription subscription =
        Subscription.create(
            id,
            command.actorId(),
            command.programId(),
            command.planId(),
            command.referenceJourneyCode());
    Subscription saved = saveSubscriptionPort.save(subscription);

    // 5. Build view and store idempotency record (use returned entity for correct version)
    SubscriptionView view = toView(saved);
    String responseBodyJson = jsonCodecPort.toJson(view);
    idempotencyStorePort.save(
        command.idempotencyKey(), OPERATION, command.requestHash(), 201, responseBodyJson);

    // 6. Audit (CROSS-4: SUBSCRIPTION_CREATED)
    auditRecorder.record(
        new RecordAuditEventCommand(
            command.actorId(),
            "STUDENT",
            "SUBSCRIPTION_CREATED",
            "subscription",
            id.value(),
            null,
            null,
            null));

    return view;
  }

  private SubscriptionView toView(Subscription subscription) {
    // programName and planName are not resolved at creation time (no catalog port injected here).
    // The REST adapter / dedicated query service resolves names for responses.
    return new SubscriptionView(
        subscription.getId().value(),
        subscription.getProgramId(),
        null, // programName resolved by query service
        subscription.getPlanId(),
        null, // planName resolved by query service
        subscription.getReferenceJourneyCode(),
        null, // referenceJourneyName resolved by query service
        subscription.getVersion(),
        subscription.getCreatedAt());
  }
}
