package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import co.edu.uniquindio.brujula.progress.application.dto.UpdateCourseAttemptCommand;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.service.UpdateCourseAttemptService;
import co.edu.uniquindio.brujula.progress.domain.AttemptId;
import co.edu.uniquindio.brujula.progress.domain.AttemptNotFoundException;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.Grade;
import co.edu.uniquindio.brujula.progress.domain.GradeStatusMismatchException;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU3-T10 RED+GREEN: use case unit tests for {@link UpdateCourseAttemptService}.
 *
 * <p>Covers: PRG-3-HP, PRG-3-VERSION-CONFLICT (via JPA optimistic lock), grade revalidation.
 */
class UpdateCourseAttemptServiceTest {

  private LoadCourseAttemptPort loadCourseAttemptPort;
  private LoadSubscriptionPort loadSubscriptionPort;
  private SaveCourseAttemptPort saveCourseAttemptPort;
  private AuditRecorder auditRecorder;
  private UpdateCourseAttemptService sut;

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
    sut = new UpdateCourseAttemptService(
        loadCourseAttemptPort, loadSubscriptionPort, saveCourseAttemptPort, auditRecorder);
  }

  @Test
  void update_from_failed_to_passed_returns_view() {
    CourseAttempt attempt = buildFailedAttempt();
    Subscription sub = buildSubscription(actorId);

    when(loadCourseAttemptPort.loadById(attempt.getId().value())).thenReturn(Optional.of(attempt));
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(sub));
    when(saveCourseAttemptPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UpdateCourseAttemptCommand command = new UpdateCourseAttemptCommand(
        attempt.getId().value(), actorId, "PASSED", new BigDecimal("3.5"),
        null, "DIURNA", 0L);

    CourseAttemptView view = sut.updateCourseAttempt(command);

    assertThat(view.statusName()).isEqualTo("PASSED"); // FIX-S1: plain String
    verify(auditRecorder).record(any());
  }

  @Test
  void update_with_invalid_grade_throws_grade_status_mismatch() {
    CourseAttempt attempt = buildFailedAttempt();
    Subscription sub = buildSubscription(actorId);

    when(loadCourseAttemptPort.loadById(attempt.getId().value())).thenReturn(Optional.of(attempt));
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(sub));

    // PASSED requires grade >= 3.0
    UpdateCourseAttemptCommand command = new UpdateCourseAttemptCommand(
        attempt.getId().value(), actorId, "PASSED", new BigDecimal("2.5"),
        null, "DIURNA", 0L);

    assertThatThrownBy(() -> sut.updateCourseAttempt(command))
        .isInstanceOf(GradeStatusMismatchException.class);
  }

  @Test
  void update_bola_throws_not_found() {
    CourseAttempt attempt = buildFailedAttempt();
    UUID differentUserId = UUID.randomUUID();
    Subscription foreignSub = buildSubscription(differentUserId);

    when(loadCourseAttemptPort.loadById(attempt.getId().value())).thenReturn(Optional.of(attempt));
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(foreignSub));

    UpdateCourseAttemptCommand command = new UpdateCourseAttemptCommand(
        attempt.getId().value(), actorId, "PASSED", new BigDecimal("3.5"),
        null, "DIURNA", 0L);

    assertThatThrownBy(() -> sut.updateCourseAttempt(command))
        .isInstanceOf(AttemptNotFoundException.class);
  }

  @Test
  void update_attempt_not_found_throws() {
    UUID nonExistent = UUID.randomUUID();
    when(loadCourseAttemptPort.loadById(nonExistent)).thenReturn(Optional.empty());

    UpdateCourseAttemptCommand command = new UpdateCourseAttemptCommand(
        nonExistent, actorId, "PASSED", new BigDecimal("3.5"), null, "DIURNA", 0L);

    assertThatThrownBy(() -> sut.updateCourseAttempt(command))
        .isInstanceOf(AttemptNotFoundException.class);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CourseAttempt buildFailedAttempt() {
    return CourseAttempt.reconstitute(
        AttemptId.generate(),
        SubscriptionId.of(subscriptionId),
        courseId,
        AttemptStatus.FAILED,
        new Grade(new BigDecimal("2.0")),
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
