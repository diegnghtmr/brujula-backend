package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptPageView;
import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptView;
import co.edu.uniquindio.brujula.progress.application.dto.ListCourseAttemptsQuery;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.service.GetCourseAttemptService;
import co.edu.uniquindio.brujula.progress.domain.AttemptId;
import co.edu.uniquindio.brujula.progress.domain.AttemptNotFoundException;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.Grade;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU3-T09 RED+GREEN: use case unit tests for {@link GetCourseAttemptService}.
 *
 * <p>Covers: PRG-2-LIST (list with subscription filter), PRG-2-BOLA (cross-student access blocked).
 */
class GetCourseAttemptServiceTest {

  private LoadCourseAttemptPort loadCourseAttemptPort;
  private LoadSubscriptionPort loadSubscriptionPort;
  private GetCourseAttemptService sut;

  private final UUID actorId = UUID.randomUUID();
  private final UUID subscriptionId = UUID.randomUUID();
  private final UUID courseId = UUID.randomUUID();
  private final UUID planId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadCourseAttemptPort = mock(LoadCourseAttemptPort.class);
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    sut = new GetCourseAttemptService(loadCourseAttemptPort, loadSubscriptionPort);
  }

  @Test
  void get_attempt_returns_view_for_owner() {
    CourseAttempt attempt = buildPassedAttempt();
    Subscription sub = buildSubscription(actorId);

    when(loadCourseAttemptPort.loadById(attempt.getId().value())).thenReturn(Optional.of(attempt));
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(sub));

    CourseAttemptView view = sut.getCourseAttempt(attempt.getId().value(), actorId);

    assertThat(view.id()).isEqualTo(attempt.getId().value());
    assertThat(view.statusName()).isEqualTo("PASSED"); // FIX-S1: plain String
  }

  @Test
  void get_attempt_not_found_throws() {
    UUID nonExistentId = UUID.randomUUID();
    when(loadCourseAttemptPort.loadById(nonExistentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.getCourseAttempt(nonExistentId, actorId))
        .isInstanceOf(AttemptNotFoundException.class);
  }

  @Test
  void bola_cross_student_access_throws_not_found() {
    CourseAttempt attempt = buildPassedAttempt();
    UUID differentUserId = UUID.randomUUID();
    Subscription foreignSub = buildSubscription(differentUserId); // belongs to different user

    when(loadCourseAttemptPort.loadById(attempt.getId().value())).thenReturn(Optional.of(attempt));
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(foreignSub));

    // actorId != foreignSub.userId → 404
    assertThatThrownBy(() -> sut.getCourseAttempt(attempt.getId().value(), actorId))
        .isInstanceOf(AttemptNotFoundException.class);
  }

  @Test
  void list_returns_paged_results() {
    CourseAttempt attempt = buildPassedAttempt();
    Subscription sub = buildSubscription(actorId);

    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(sub));
    when(loadCourseAttemptPort.loadBySubscriptionIdAndCourseId(subscriptionId, null, 0, 20))
        .thenReturn(List.of(attempt));
    when(loadCourseAttemptPort.countBySubscriptionIdAndCourseId(subscriptionId, null))
        .thenReturn(1L);

    ListCourseAttemptsQuery query = new ListCourseAttemptsQuery(actorId, subscriptionId, null, 0, 20);
    CourseAttemptPageView page = sut.listMyCourseAttempts(query);

    assertThat(page.content()).hasSize(1);
    assertThat(page.totalElements()).isEqualTo(1L);
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
