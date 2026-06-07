package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import co.edu.uniquindio.brujula.progress.application.dto.CreateCourseAttemptCommand;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateCourseAttemptUseCase;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.service.CreateCourseAttemptService;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.AttemptNotFoundException;
import co.edu.uniquindio.brujula.progress.domain.GradeStatusMismatchException;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU3-T07 RED: use case unit tests for {@link CreateCourseAttemptService}.
 *
 * <p>Covers: PRG-1-HP, PRG-1-GRADE-MISMATCH, PRG-1-CANCELLED-WITH-GRADE, PRG-1-OPAQUE-PERIOD,
 * PRG-1-MULTI-ATTEMPT, PRG-1-FOREIGN-SUB, PRG-1-IDEM.
 */
class CreateCourseAttemptServiceTest {

  private LoadSubscriptionPort loadSubscriptionPort;
  private SaveCourseAttemptPort saveCourseAttemptPort;
  private LoadCourseAttemptPort loadCourseAttemptPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AuditRecorder auditRecorder;
  private CreateCourseAttemptService sut;

  private final UUID actorId = UUID.randomUUID();
  private final UUID subscriptionId = UUID.randomUUID();
  private final UUID courseId = UUID.randomUUID();
  private final UUID planId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    saveCourseAttemptPort = mock(SaveCourseAttemptPort.class);
    loadCourseAttemptPort = mock(LoadCourseAttemptPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    auditRecorder = mock(AuditRecorder.class);

    sut = new CreateCourseAttemptService(
        loadSubscriptionPort,
        saveCourseAttemptPort,
        loadCourseAttemptPort,
        idempotencyStorePort,
        jsonCodecPort,
        auditRecorder);
  }

  @Test
  void happy_path_passed_attempt_returns_view() {
    UUID idempotencyKey = UUID.randomUUID();
    Subscription subscription = buildSubscription(actorId);

    when(idempotencyStorePort.find(idempotencyKey, CreateCourseAttemptService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(subscription));
    when(saveCourseAttemptPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    CreateCourseAttemptCommand command = new CreateCourseAttemptCommand(
        idempotencyKey, actorId, subscriptionId, courseId, null, "DIURNA",
        "PASSED", new BigDecimal("4.2"), "hash1");

    CourseAttemptView view = sut.createCourseAttempt(command);

    assertThat(view).isNotNull();
    assertThat(view.subscriptionId()).isEqualTo(subscriptionId);
    assertThat(view.courseId()).isEqualTo(courseId);
    assertThat(view.status()).isEqualTo(AttemptStatus.PASSED);
    verify(auditRecorder).record(any());
  }

  @Test
  void foreign_subscription_ownership_throws_not_found() {
    UUID idempotencyKey = UUID.randomUUID();
    UUID differentUserId = UUID.randomUUID();
    Subscription foreignSub = buildSubscription(differentUserId); // belongs to different user

    when(idempotencyStorePort.find(idempotencyKey, CreateCourseAttemptService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(foreignSub));

    CreateCourseAttemptCommand command = new CreateCourseAttemptCommand(
        idempotencyKey, actorId, subscriptionId, courseId, null, "DIURNA",
        "PASSED", new BigDecimal("4.2"), "hash1");

    assertThatThrownBy(() -> sut.createCourseAttempt(command))
        .isInstanceOf(SubscriptionNotFoundException.class);

    verify(saveCourseAttemptPort, never()).save(any());
  }

  @Test
  void grade_status_mismatch_passed_below_3_throws() {
    UUID idempotencyKey = UUID.randomUUID();
    Subscription subscription = buildSubscription(actorId);

    when(idempotencyStorePort.find(idempotencyKey, CreateCourseAttemptService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(subscription));

    CreateCourseAttemptCommand command = new CreateCourseAttemptCommand(
        idempotencyKey, actorId, subscriptionId, courseId, null, "DIURNA",
        "PASSED", new BigDecimal("2.5"), "hash1");

    assertThatThrownBy(() -> sut.createCourseAttempt(command))
        .isInstanceOf(GradeStatusMismatchException.class);
  }

  @Test
  void cancelled_with_grade_throws_grade_status_mismatch() {
    UUID idempotencyKey = UUID.randomUUID();
    Subscription subscription = buildSubscription(actorId);

    when(idempotencyStorePort.find(idempotencyKey, CreateCourseAttemptService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(subscription));

    CreateCourseAttemptCommand command = new CreateCourseAttemptCommand(
        idempotencyKey, actorId, subscriptionId, courseId, null, "DIURNA",
        "CANCELLED", new BigDecimal("3.0"), "hash1");

    assertThatThrownBy(() -> sut.createCourseAttempt(command))
        .isInstanceOf(GradeStatusMismatchException.class);
  }

  @Test
  void opaque_period_id_accepted_without_validation() {
    UUID idempotencyKey = UUID.randomUUID();
    UUID opaquePeriodId = UUID.randomUUID(); // any UUID — no catalog validation
    Subscription subscription = buildSubscription(actorId);

    when(idempotencyStorePort.find(idempotencyKey, CreateCourseAttemptService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(subscription));
    when(saveCourseAttemptPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jsonCodecPort.toJson(any())).thenReturn("{}");

    CreateCourseAttemptCommand command = new CreateCourseAttemptCommand(
        idempotencyKey, actorId, subscriptionId, courseId, opaquePeriodId, "DIURNA",
        "PASSED", new BigDecimal("4.0"), "hash1");

    CourseAttemptView view = sut.createCourseAttempt(command);
    assertThat(view).isNotNull();
    assertThat(view.periodId()).isEqualTo(opaquePeriodId);
  }

  @Test
  void idempotent_replay_returns_original_view_without_persisting() {
    UUID idempotencyKey = UUID.randomUUID();
    CourseAttemptView originalView = new CourseAttemptView(
        UUID.randomUUID(), subscriptionId, courseId, "N/A", null, null, "DIURNA",
        AttemptStatus.PASSED, new BigDecimal("4.2"), Instant.now(), 0L);

    when(idempotencyStorePort.find(idempotencyKey, CreateCourseAttemptService.OPERATION))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredRecord("hash1", 201, "{}")));
    when(jsonCodecPort.fromJson("{}", CourseAttemptView.class)).thenReturn(originalView);

    CreateCourseAttemptCommand command = new CreateCourseAttemptCommand(
        idempotencyKey, actorId, subscriptionId, courseId, null, "DIURNA",
        "PASSED", new BigDecimal("4.2"), "hash1");

    CourseAttemptView result = sut.createCourseAttempt(command);

    assertThat(result).isEqualTo(originalView);
    verify(saveCourseAttemptPort, never()).save(any());
  }

  @Test
  void idempotency_key_conflict_throws() {
    UUID idempotencyKey = UUID.randomUUID();

    when(idempotencyStorePort.find(idempotencyKey, CreateCourseAttemptService.OPERATION))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredRecord("old-hash", 201, "{}")));

    CreateCourseAttemptCommand command = new CreateCourseAttemptCommand(
        idempotencyKey, actorId, subscriptionId, courseId, null, "DIURNA",
        "PASSED", new BigDecimal("4.2"), "new-hash"); // different hash

    assertThatThrownBy(() -> sut.createCourseAttempt(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);
  }

  @Test
  void subscription_not_found_throws() {
    UUID idempotencyKey = UUID.randomUUID();

    when(idempotencyStorePort.find(idempotencyKey, CreateCourseAttemptService.OPERATION))
        .thenReturn(Optional.empty());
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.empty());

    CreateCourseAttemptCommand command = new CreateCourseAttemptCommand(
        idempotencyKey, actorId, subscriptionId, courseId, null, "DIURNA",
        "PASSED", new BigDecimal("4.2"), "hash1");

    assertThatThrownBy(() -> sut.createCourseAttempt(command))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private Subscription buildSubscription(UUID ownerId) {
    return Subscription.create(
        SubscriptionId.of(subscriptionId), ownerId, programId, planId, "DIURNA");
  }
}
