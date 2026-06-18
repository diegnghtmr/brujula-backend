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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: create a new subscription for the authenticated student.
 *
 * <p>Enforces:
 *
 * <ul>
 *   <li>Cap: at most 2 DISTINCT programs (careers) per student. Within each program, both jornadas
 *       (Diurna and Nocturna) are allowed. If the incoming programId is already in the student's
 *       active set, the cap does NOT apply.
 *   <li>Duplicate guard: the uniqueness key is (programId + referenceJourneyCode). Same program +
 *       same journey is rejected as a duplicate; same program + different journey is allowed.
 *   <li>Idempotency via sharedkernel {@link IdempotencyStorePort}.
 *   <li>Audit via {@link AuditRecorder}.
 * </ul>
 *
 * <p>Framework-free — wired by {@code ProgressConfiguration}.
 */
public class CreateSubscriptionService implements CreateSubscriptionUseCase {

  public static final String OPERATION = "CREATE_SUBSCRIPTION";
  private static final int MAX_DISTINCT_PROGRAMS = 2;

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
  @Transactional
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

    // 2. Cap enforcement (contract #1630 — revised).
    // Acquire a row-level write lock on existing active subscriptions BEFORE any check.
    // This serialises concurrent creates per-student and prevents the TOCTOU race.
    //
    // Business rule: the cap is on DISTINCT PROGRAMS (careers), not on total active rows.
    // Within each program both jornadas are allowed (max 4 rows = 2 programs × 2 jornadas).
    // If the incoming programId is already in the active set, the cap does NOT apply
    // (adding a different jornada of an already-subscribed program is always permitted, subject
    // to the duplicate-jornada check below).
    List<Subscription> activeSubs =
        loadSubscriptionPort.lockActiveByUserIdForUpdate(command.actorId());
    Set<UUID> distinctActiveProgramIds =
        activeSubs.stream().map(Subscription::getProgramId).collect(Collectors.toSet());

    boolean programAlreadyInSet = distinctActiveProgramIds.contains(command.programId());
    if (!programAlreadyInSet && distinctActiveProgramIds.size() >= MAX_DISTINCT_PROGRAMS) {
      throw new MaxSubscriptionsExceededException(
          command.actorId(), distinctActiveProgramIds.size());
    }

    // 3. Duplicate-jornada guard: (programId + referenceJourneyCode) is the uniqueness key.
    // Same program + same journey → duplicate. Same program + different journey → allowed.
    if (loadSubscriptionPort.existsActiveByUserIdAndProgramIdAndJourneyCode(
        command.actorId(), command.programId(), command.referenceJourneyCode())) {
      throw new DuplicateProgramSubscriptionException(command.actorId(), command.programId());
    }

    // 3b. Validate the plan is ACTIVE via anti-corruption facade.
    // ValidatePlanStatusPort wraps GetStudyPlanQuery from programcatalog without crossing the
    // Spring Modulith named-interface boundary from the application layer.
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

    // 6. Audit (SUBSCRIPTION_CREATED)
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
    // TODO: resolve program/plan/journey names like GetSubscriptionService
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
