package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.service.DeleteCourseAttemptService;
import co.edu.uniquindio.brujula.progress.domain.AttemptId;
import co.edu.uniquindio.brujula.progress.domain.AttemptNotFoundException;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.Grade;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU3-T11 RED+GREEN: use case unit tests for {@link DeleteCourseAttemptService}.
 *
 * <p>Covers: PRG-4-HP (delete attempt, audit event).
 */
class DeleteCourseAttemptServiceTest {

  private LoadCourseAttemptPort loadCourseAttemptPort;
  private LoadSubscriptionPort loadSubscriptionPort;
  private SaveCourseAttemptPort saveCourseAttemptPort;
  private AuditRecorder auditRecorder;
  private DeleteCourseAttemptService sut;

  private final UUID actorId = UUID.randomUUID();
  private final UUID subscriptionId = UUID.randomUUID();
  private final UUID courseId = UUID.randomUUID();
  private final UUID planId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadCourseAttemptPort = mock(LoadCourseAttemptPort.class);
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    saveCourseAttemptPort = mock(SaveCourseAttemptPort.class);
    auditRecorder = mock(AuditRecorder.class);
    sut = new DeleteCourseAttemptService(
        loadCourseAttemptPort, loadSubscriptionPort, saveCourseAttemptPort, auditRecorder);
  }

  @Test
  void delete_happy_path_deletes_and_records_audit() {
    CourseAttempt attempt = buildPassedAttempt();
    Subscription sub = buildSubscription(actorId);

    when(loadCourseAttemptPort.loadById(attempt.getId().value())).thenReturn(Optional.of(attempt));
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(sub));

    sut.deleteCourseAttempt(attempt.getId().value(), actorId);

    verify(saveCourseAttemptPort).delete(attempt);
    verify(auditRecorder).record(any());
  }

  @Test
  void delete_bola_throws_not_found() {
    CourseAttempt attempt = buildPassedAttempt();
    UUID differentUserId = UUID.randomUUID();
    Subscription foreignSub = buildSubscription(differentUserId);

    when(loadCourseAttemptPort.loadById(attempt.getId().value())).thenReturn(Optional.of(attempt));
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(foreignSub));

    assertThatThrownBy(() -> sut.deleteCourseAttempt(attempt.getId().value(), actorId))
        .isInstanceOf(AttemptNotFoundException.class);

    verify(saveCourseAttemptPort, never()).delete(any());
  }

  @Test
  void delete_not_found_throws() {
    UUID nonExistent = UUID.randomUUID();
    when(loadCourseAttemptPort.loadById(nonExistent)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.deleteCourseAttempt(nonExistent, actorId))
        .isInstanceOf(AttemptNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CourseAttempt buildPassedAttempt() {
    return CourseAttempt.reconstitute(
        AttemptId.generate(),
        SubscriptionId.of(subscriptionId),
        courseId,
        AttemptStatus.PASSED,
        new Grade(new BigDecimal("4.0")),
        null,
        "DIURNA",
        Instant.now(),
        0L);
  }

  private Subscription buildSubscription(UUID ownerId) {
    return Subscription.create(
        SubscriptionId.of(subscriptionId), ownerId, programId, planId, "DIURNA");
  }
}
