package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.dto.CreateSubscriptionCommand;
import co.edu.uniquindio.brujula.progress.application.dto.SubscriptionView;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.ValidatePlanStatusPort;
import co.edu.uniquindio.brujula.progress.application.service.CreateSubscriptionService;
import co.edu.uniquindio.brujula.progress.domain.DuplicateProgramSubscriptionException;
import co.edu.uniquindio.brujula.progress.domain.InactivePlanException;
import co.edu.uniquindio.brujula.progress.domain.MaxSubscriptionsExceededException;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
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
 * <p>Covers: SUB-1-HP, SUB-1-CAP, SUB-1-DUP, SUB-1-IDEM, SUB-1-IDEM-CONFLICT,
 * SUB-1-JORNADA-DUP, SUB-1-JORNADA-DIFF-ALLOWED, SUB-1-CAP-DISTINCT-PROGRAMS.
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

    sut =
        new CreateSubscriptionService(
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
    // lockActiveByUserIdForUpdate returns empty list (0 active subscriptions)
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId)).thenReturn(List.of());
    when(loadSubscriptionPort.existsActiveByUserIdAndProgramIdAndJourneyCode(
            userId, programId, "DIURNA"))
        .thenReturn(false);
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
        new CreateSubscriptionCommand(
            idempotencyKey, userId, programId, planId, "DIURNA", "hash-arch");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId)).thenReturn(List.of());
    when(loadSubscriptionPort.existsActiveByUserIdAndProgramIdAndJourneyCode(
            userId, programId, "DIURNA"))
        .thenReturn(false);
    // Plan is ARCHIVED — subscription MUST be rejected (via anti-corruption facade)
    when(validatePlanStatusPort.getPlanStatus(planId)).thenReturn("ARCHIVED");

    assertThatThrownBy(() -> sut.createSubscription(command))
        .isInstanceOf(InactivePlanException.class)
        .hasMessageContaining("ARCHIVED");

    verify(saveSubscriptionPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // SUB-1-CAP: cap is on DISTINCT programs, not total rows
  // -------------------------------------------------------------------------

  /**
   * Active in 2 distinct programs (A, B); subscribing to a NEW program C → throws
   * MaxSubscriptionsExceededException.
   */
  @Test
  void cap_rejects_third_distinct_program_subscription() {
    UUID programA = UUID.randomUUID();
    UUID programB = UUID.randomUUID();
    UUID programC = UUID.randomUUID();

    UUID idempotencyKey = UUID.randomUUID();
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(
            idempotencyKey, userId, programC, planId, "DIURNA", "hash-cap");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());

    // Two active subscriptions for 2 distinct programs A and B
    Subscription s1 =
        Subscription.create(SubscriptionId.generate(), userId, programA, UUID.randomUUID(), "DIURNA");
    Subscription s2 =
        Subscription.create(SubscriptionId.generate(), userId, programB, UUID.randomUUID(), "DIURNA");
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId)).thenReturn(List.of(s1, s2));

    assertThatThrownBy(() -> sut.createSubscription(command))
        .isInstanceOf(MaxSubscriptionsExceededException.class);

    verify(saveSubscriptionPort, never()).save(any());
  }

  /**
   * Already active in (programA, DIURNA); subscribing (programA, NOCTURNA) → SUCCEEDS.
   * Program is already in the set, so the cap does NOT apply.
   */
  @Test
  void same_program_different_jornada_is_allowed_and_cap_does_not_apply() {
    UUID idempotencyKey = UUID.randomUUID();
    // Subscribe programId with NOCTURNA, while DIURNA already exists for same program
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(
            idempotencyKey, userId, programId, planId, "NOCTURNA", "hash-nocturna");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());

    // One active subscription: same programId, DIURNA — so programId is already in set
    Subscription existing =
        Subscription.create(SubscriptionId.generate(), userId, programId, planId, "DIURNA");
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId)).thenReturn(List.of(existing));

    // NOCTURNA for this program does NOT exist yet
    when(loadSubscriptionPort.existsActiveByUserIdAndProgramIdAndJourneyCode(
            userId, programId, "NOCTURNA"))
        .thenReturn(false);
    when(saveSubscriptionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{\"id\":\"...\"}");
    when(validatePlanStatusPort.getPlanStatus(planId)).thenReturn("ACTIVE");

    // Should NOT throw — cap is skipped because programId is already in the active set
    SubscriptionView view = sut.createSubscription(command);

    assertThat(view).isNotNull();
    verify(saveSubscriptionPort).save(any());
  }

  /**
   * Already active in (programA, DIURNA); subscribing (programA, DIURNA) again → throws
   * DuplicateProgramSubscriptionException.
   */
  @Test
  void same_program_same_jornada_is_duplicate_and_rejected() {
    UUID idempotencyKey = UUID.randomUUID();
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(idempotencyKey, userId, programId, planId, "DIURNA", "hash-dup");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());

    // One active subscription: same programId, DIURNA
    Subscription existing =
        Subscription.create(SubscriptionId.generate(), userId, programId, planId, "DIURNA");
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId)).thenReturn(List.of(existing));

    // DIURNA for this program already exists
    when(loadSubscriptionPort.existsActiveByUserIdAndProgramIdAndJourneyCode(
            userId, programId, "DIURNA"))
        .thenReturn(true);

    assertThatThrownBy(() -> sut.createSubscription(command))
        .isInstanceOf(DuplicateProgramSubscriptionException.class);

    verify(saveSubscriptionPort, never()).save(any());
  }

  /**
   * Active in (programA, DIURNA) and (programB, DIURNA) [2 distinct programs]; subscribing
   * (programA, NOCTURNA) → SUCCEEDS (programA already in set, cap skipped), resulting in 3 active rows.
   */
  @Test
  void second_jornada_of_already_subscribed_program_bypasses_cap_with_two_distinct_programs() {
    UUID programA = programId;
    UUID programB = UUID.randomUUID();

    UUID idempotencyKey = UUID.randomUUID();
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(
            idempotencyKey, userId, programA, planId, "NOCTURNA", "hash-bypass");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());

    // Two active subscriptions — 2 distinct programs, one being programA
    Subscription sA =
        Subscription.create(SubscriptionId.generate(), userId, programA, planId, "DIURNA");
    Subscription sB =
        Subscription.create(SubscriptionId.generate(), userId, programB, UUID.randomUUID(), "DIURNA");
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId)).thenReturn(List.of(sA, sB));

    // programA NOCTURNA does NOT exist yet
    when(loadSubscriptionPort.existsActiveByUserIdAndProgramIdAndJourneyCode(
            userId, programA, "NOCTURNA"))
        .thenReturn(false);
    when(saveSubscriptionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{\"id\":\"...\"}");
    when(validatePlanStatusPort.getPlanStatus(planId)).thenReturn("ACTIVE");

    // programA is already in the set → cap NOT applied → succeeds
    SubscriptionView view = sut.createSubscription(command);

    assertThat(view).isNotNull();
    verify(saveSubscriptionPort).save(any());
  }

  /**
   * Full-house scenario: 4 active rows (A-DIURNA, A-NOCTURNA, B-DIURNA, B-NOCTURNA).
   * Subscribing to a 3rd distinct program C → rejected.
   */
  @Test
  void third_distinct_program_rejected_when_full_house_four_rows() {
    UUID programA = UUID.randomUUID();
    UUID programB = UUID.randomUUID();
    UUID programC = UUID.randomUUID();

    UUID idempotencyKey = UUID.randomUUID();
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(
            idempotencyKey, userId, programC, planId, "DIURNA", "hash-full");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());

    // 4 active rows — 2 distinct programs
    Subscription s1 =
        Subscription.create(SubscriptionId.generate(), userId, programA, UUID.randomUUID(), "DIURNA");
    Subscription s2 =
        Subscription.create(SubscriptionId.generate(), userId, programA, UUID.randomUUID(), "NOCTURNA");
    Subscription s3 =
        Subscription.create(SubscriptionId.generate(), userId, programB, UUID.randomUUID(), "DIURNA");
    Subscription s4 =
        Subscription.create(SubscriptionId.generate(), userId, programB, UUID.randomUUID(), "NOCTURNA");
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId))
        .thenReturn(List.of(s1, s2, s3, s4));

    assertThatThrownBy(() -> sut.createSubscription(command))
        .isInstanceOf(MaxSubscriptionsExceededException.class);

    verify(saveSubscriptionPort, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // Original cap test — updated to use distinct-program semantics
  // -------------------------------------------------------------------------

  @Test
  void cap_2_rejects_third_subscription_with_domain_exception() {
    UUID idempotencyKey = UUID.randomUUID();
    UUID programC = UUID.randomUUID(); // NEW program, not in the active set
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(idempotencyKey, userId, programC, planId, "DIURNA", "hash2");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());

    // 2 active subscriptions for 2 DISTINCT programs — cap is on distinct programs
    Subscription s1 =
        Subscription.create(
            SubscriptionId.generate(), userId, UUID.randomUUID(), UUID.randomUUID(), "DIURNA");
    Subscription s2 =
        Subscription.create(
            SubscriptionId.generate(), userId, UUID.randomUUID(), UUID.randomUUID(), "NOCTURNA");
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId)).thenReturn(List.of(s1, s2));

    assertThatThrownBy(() -> sut.createSubscription(command))
        .isInstanceOf(MaxSubscriptionsExceededException.class);

    verify(saveSubscriptionPort, never()).save(any());
  }

  /**
   * SUB-1-CAP-DISTINCT-PROGRAMS: 2 active rows for the SAME program (programA-DIURNA +
   * programA-NOCTURNA) → only 1 distinct program. Subscribing to a NEW programB → SUCCEEDS because
   * only 1 distinct program is active, adding a 2nd distinct program is within the cap of 2.
   *
   * <p>This test discriminates the current distinct-program logic from the old row-count logic:
   * under the old count-based check (count >= 2) this would have thrown; under the correct
   * distinct-program check it must succeed.
   */
  @Test
  void two_jornadas_of_one_program_still_allow_subscribing_to_a_second_distinct_program() {
    UUID programA = UUID.randomUUID();
    UUID programB = UUID.randomUUID();

    UUID idempotencyKey = UUID.randomUUID();
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(
            idempotencyKey, userId, programB, planId, "DIURNA", "hash-distinct");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());

    // Two active rows for the SAME program (programA, DIURNA) + (programA, NOCTURNA)
    // → only 1 distinct program in the active set
    Subscription s1 =
        Subscription.create(SubscriptionId.generate(), userId, programA, UUID.randomUUID(), "DIURNA");
    Subscription s2 =
        Subscription.create(
            SubscriptionId.generate(), userId, programA, UUID.randomUUID(), "NOCTURNA");
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId)).thenReturn(List.of(s1, s2));

    // programB DIURNA does NOT exist yet
    when(loadSubscriptionPort.existsActiveByUserIdAndProgramIdAndJourneyCode(
            userId, programB, "DIURNA"))
        .thenReturn(false);
    when(saveSubscriptionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{\"id\":\"...\"}");
    when(validatePlanStatusPort.getPlanStatus(planId)).thenReturn("ACTIVE");

    // Only 1 distinct program active → cap allows a 2nd distinct program → must NOT throw
    SubscriptionView view = sut.createSubscription(command);

    assertThat(view).isNotNull();
    assertThat(view.programId()).isEqualTo(programB);
    verify(saveSubscriptionPort).save(any());
  }

  @Test
  void duplicate_program_rejects_with_domain_exception() {
    UUID idempotencyKey = UUID.randomUUID();
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(idempotencyKey, userId, programId, planId, "DIURNA", "hash3");

    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.empty());
    // 1 locked active subscription for a DIFFERENT program → cap not exceeded
    when(loadSubscriptionPort.lockActiveByUserIdForUpdate(userId))
        .thenReturn(
            List.of(
                Subscription.create(
                    SubscriptionId.generate(),
                    userId,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "DIURNA")));
    // DIURNA for programId already exists → duplicate
    when(loadSubscriptionPort.existsActiveByUserIdAndProgramIdAndJourneyCode(
            userId, programId, "DIURNA"))
        .thenReturn(true);

    assertThatThrownBy(() -> sut.createSubscription(command))
        .isInstanceOf(DuplicateProgramSubscriptionException.class);

    verify(saveSubscriptionPort, never()).save(any());
  }

  @Test
  void idempotent_replay_returns_stored_view_without_new_persistence() {
    UUID idempotencyKey = UUID.randomUUID();
    String requestHash = "hash4";
    CreateSubscriptionCommand command =
        new CreateSubscriptionCommand(
            idempotencyKey, userId, programId, planId, "DIURNA", requestHash);

    SubscriptionView storedView =
        new SubscriptionView(
            UUID.randomUUID(),
            programId,
            "P",
            planId,
            "Plan",
            "DIURNA",
            "Diurna",
            0L,
            Instant.now());

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
        new CreateSubscriptionCommand(
            idempotencyKey, userId, programId, planId, "DIURNA", "new-hash");

    IdempotencyStorePort.StoredRecord stored =
        new IdempotencyStorePort.StoredRecord("original-hash", 201, "{...}");
    when(idempotencyStorePort.find(idempotencyKey, CreateSubscriptionService.OPERATION))
        .thenReturn(Optional.of(stored));

    assertThatThrownBy(() -> sut.createSubscription(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);
  }
}
