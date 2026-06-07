package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.progress.application.dto.CreateSubscriptionCommand;
import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.service.CreateSubscriptionService;
import co.edu.uniquindio.brujula.progress.domain.DuplicateProgramSubscriptionException;
import co.edu.uniquindio.brujula.progress.application.port.out.ValidatePlanStatusPort;
import co.edu.uniquindio.brujula.progress.domain.InactivePlanException;
import co.edu.uniquindio.brujula.progress.domain.MaxSubscriptionsExceededException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU2-T09 RED+GREEN: use case unit tests for {@link CreateSubscriptionService}.
 *
 * <p>Covers: SUB-1-HP, SUB-1-CAP, SUB-1-DUP, SUB-1-IDEM, SUB-1-IDEM-CONFLICT.
 */
class CreateSubscriptionServiceTest {

  private LoadSubscriptionPort loadSubscriptionPort;
  private SaveSubscriptionPort saveSubscriptionPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AuditRecorder auditRecorder;
  private ValidatePlanStatusPort validatePlanStatusPort;
  private CreateSubscriptionService sut;

  private final UUID userId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID planId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    saveSubscriptionPort = mock(SaveSubscriptionPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    auditRecorder = mock(AuditRecorder.class);
    validatePlanStatusPort = mock(ValidatePlanStatusPort.class);

    sut = new CreateSubscriptionService(
        loadSubscriptionPort,
        saveSubscriptionPort,
        idempotencyStorePort,
        jsonCodecPort,
        auditRecorder,
        validatePlanStatusPort);
  }

  @Test
  void happy_path_creates_subscription_and_returns_view() {
    UUID idempotencyKey = UUID.randomUUID();
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(idempotencyKey, userId, programId, planId, "DIURNA", "hash1");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());
    // FIX-W1: lockActiveByUserIdForUpdate returns empty list (0 active subscriptions)
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId)).thenReturn(List.of());
    when(loadSubscriptionPort.existsActiveByUserIdAndProgramId(userId, programId)).thenReturn(false);
    when(saveSubscriptionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{\"id\":\"...\"}");
    // FIX-W3: plan must be ACTIVE (via anti-corruption facade)
    when(validatePlanStatusPort.getPlanStatus(planId)).thenReturn("ACTIVE");

    SubscriptionView view = sut.createSubscription(command);

    assertThat(view).isNotNull();
    assertThat(view.programId()).isEqualTo(programId);
    assertThat(view.planId()).isEqualTo(planId);
    verify(saveSubscriptionPort).save(any());
    verify(auditRecorder).record(any(RecordAuditEventCommand.class));
  }

  // -------------------------------------------------------------------------
  // FIX-W3 RED: SUB-1-INACTIVE-PLAN — ARCHIVED plan rejected with 422
  // -------------------------------------------------------------------------

  @Test
  void archived_plan_rejected_with_inactive_plan_exception() {
    UUID idempotencyKey = UUID.randomUUID();
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(idempotencyKey, userId, programId, planId, "DIURNA", "hash-arch");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId)).thenReturn(List.of());
    when(loadSubscriptionPort.existsActiveByUserIdAndProgramId(userId, programId)).thenReturn(false);
    // Plan is ARCHIVED — subscription MUST be rejected (via anti-corruption facade)
    when(validatePlanStatusPort.getPlanStatus(planId)).thenReturn("ARCHIVED");

    assertThatThrownBy(() -> sut.createSubscription(command))
        .isInstanceOf(InactivePlanException.class)
        .hasMessageContaining("ARCHIVED");

    verify(saveSubscriptionPort, never()).save(any());
  }

  @Test
  void cap_2_rejects_third_subscription_with_domain_exception() {
    UUID idempotencyKey = UUID.randomUUID();
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(idempotencyKey, userId, programId, planId, "DIURNA", "hash2");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());
    // FIX-W1: 2 existing locked active subscriptions → cap exceeded
    Subscription s1 = Subscription.create(SubscriptionId.generate(), userId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    Subscription s2 = Subscription.create(SubscriptionId.generate(), userId, UUID.randomUUID(), UUID.randomUUID(), "NOCTURNA");
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId)).thenReturn(List.of(s1, s2));

    assertThatThrownBy(() -> sut.createSubscription(command))
        .isInstanceOf(MaxSubscriptionsExceededException.class);

    verify(saveSubscriptionPort, never()).save(any());
  }

  @Test
  void duplicate_program_rejects_with_domain_exception() {
    UUID idempotencyKey = UUID.randomUUID();
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(idempotencyKey, userId, programId, planId, "DIURNA", "hash3");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());
    // 1 locked active subscription → cap not exceeded, proceed to duplicate check
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId))
        .thenReturn(List.of(Subscription.create(SubscriptionId.generate(), userId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA")));
    when(loadSubscriptionPort.existsActiveByUserIdAndProgramId(userId, programId)).thenReturn(true);

    assertThatThrownBy(() -> sut.createSubscription(command))
        .isInstanceOf(DuplicateProgramSubscriptionException.class);

    verify(saveSubscriptionPort, never()).save(any());
  }

  @Test
  void idempotent_replay_returns_stored_view_without_new_persistence() {
    UUID idempotencyKey = UUID.randomUUID();
    String requestHash = "hash4";
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(idempotencyKey, userId, programId, planId, "DIURNA", requestHash);

    SubscriptionView storedView =
        new SubscriptionView(UUID.randomUUID(), programId, "P", planId, "Plan", "DIURNA", "Diurna", 0L, Instant.now());

    IdempotencyStorePort.StoredRecord stored =
        new IdempotencyStorePort.StoredRecord(requestHash, 201, "{...}");
    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.of(stored));
    when(jsonCodecPort.fromJson("{...}", SubscriptionView.class)).thenReturn(storedView);

    SubscriptionView result = sut.createSubscription(command);

    assertThat(result).isEqualTo(storedView);
    verify(saveSubscriptionPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void idempotency_key_reuse_with_different_body_throws_conflict() {
    UUID idempotencyKey = UUID.randomUUID();
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(idempotencyKey, userId, programId, planId, "DIURNA", "new-hash");

    IdempotencyStorePort.StoredRecord stored =
        new IdempotencyStorePort.StoredRecord("original-hash", 201, "{...}");
    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.of(stored));

    assertThatThrownBy(() -> sut.createSubscription(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);
  }
}
