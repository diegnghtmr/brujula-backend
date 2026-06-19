package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.curriculum.application.dto.PlanMallaView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery;
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Use case unit tests for {@link GetCourseAttemptService}.
 *
 * <p>Covers: get/list for owner, cross-student access blocked, and course/period name enrichment.
 */
class GetCourseAttemptServiceTest {

  private LoadCourseAttemptPort loadCourseAttemptPort;
  private LoadSubscriptionPort loadSubscriptionPort;
  private AssemblePlanMallaQuery assemblePlanMallaQuery;
  private ListPeriodsQuery listPeriodsQuery;
  private GetCourseAttemptService sut;

  private final UUID actorId = UUID.randomUUID();
  private final UUID subscriptionId = UUID.randomUUID();
  private final UUID courseId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID planId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadCourseAttemptPort = mock(LoadCourseAttemptPort.class);
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    assemblePlanMallaQuery = mock(AssemblePlanMallaQuery.class);
    listPeriodsQuery = mock(ListPeriodsQuery.class);
    // Safe defaults so name enrichment never NPEs; individual tests override as needed.
    when(assemblePlanMallaQuery.assemblePlanMalla(any(), any()))
        .thenReturn(new PlanMallaView(planId, "DIURNA", List.of(), null));
    when(listPeriodsQuery.listPeriods(null)).thenReturn(List.of());
    sut =
        new GetCourseAttemptService(
            loadCourseAttemptPort, loadSubscriptionPort, assemblePlanMallaQuery, listPeriodsQuery);
  }

  @Test
  void get_attempt_returns_view_for_owner() {
    CourseAttempt attempt = buildPassedAttempt(null);
    Subscription sub = buildSubscription(actorId);

    when(loadCourseAttemptPort.loadById(attempt.getId().value())).thenReturn(Optional.of(attempt));
    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(sub));

    CourseAttemptView view = sut.getCourseAttempt(attempt.getId().value(), actorId);

    assertThat(view.id()).isEqualTo(attempt.getId().value());
    assertThat(view.statusName()).isEqualTo("PASSED"); // plain String
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
    CourseAttempt attempt = buildPassedAttempt(null);
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
    CourseAttempt attempt = buildPassedAttempt(null);
    Subscription sub = buildSubscription(actorId);

    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(sub));
    when(loadCourseAttemptPort.loadByUserIdAndProgramIdAndCourseId(actorId, programId, null, 0, 20))
        .thenReturn(List.of(attempt));
    when(loadCourseAttemptPort.countByUserIdAndProgramIdAndCourseId(actorId, programId, null))
        .thenReturn(1L);

    ListCourseAttemptsQuery query =
        new ListCourseAttemptsQuery(actorId, subscriptionId, null, 0, 20);
    CourseAttemptPageView page = sut.listMyCourseAttempts(query);

    assertThat(page.content()).hasSize(1);
    assertThat(page.totalElements()).isEqualTo(1L);
  }

  @Test
  void list_enriches_course_and_period_names() {
    CourseAttempt attempt = buildPassedAttempt(periodId);
    Subscription sub = buildSubscription(actorId);

    when(loadSubscriptionPort.loadById(subscriptionId)).thenReturn(Optional.of(sub));
    when(loadCourseAttemptPort.loadByUserIdAndProgramIdAndCourseId(actorId, programId, null, 0, 20))
        .thenReturn(List.of(attempt));
    when(loadCourseAttemptPort.countByUserIdAndProgramIdAndCourseId(actorId, programId, null))
        .thenReturn(1L);
    when(assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA"))
        .thenReturn(
            new PlanMallaView(
                planId,
                "DIURNA",
                List.of(
                    new PlanMallaView.SemesterBucket(
                        1,
                        List.of(
                            new PlanMallaView.CourseSummary(
                                courseId,
                                "ISC-S1-PROG1",
                                "Programación I",
                                "REQUIRED",
                                3,
                                List.of())))),
                null));
    when(listPeriodsQuery.listPeriods(null))
        .thenReturn(
            List.of(
                new AcademicPeriodView(
                    periodId,
                    "2024-1",
                    LocalDate.of(2024, 1, 15),
                    LocalDate.of(2024, 6, 7),
                    "CLOSED",
                    0L)));

    ListCourseAttemptsQuery query =
        new ListCourseAttemptsQuery(actorId, subscriptionId, null, 0, 20);
    CourseAttemptPageView page = sut.listMyCourseAttempts(query);

    assertThat(page.content()).hasSize(1);
    CourseAttemptView view = page.content().get(0);
    assertThat(view.courseName()).isEqualTo("Programación I");
    assertThat(view.periodName()).isEqualTo("2024-1");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CourseAttempt buildPassedAttempt(UUID period) {
    return CourseAttempt.reconstitute(
        AttemptId.generate(),
        SubscriptionId.of(subscriptionId),
        courseId,
        AttemptStatus.PASSED,
        new Grade(new BigDecimal("4.0")),
        period,
        "DIURNA",
        Instant.now(),
        0L);
  }

  private Subscription buildSubscription(UUID ownerId) {
    return Subscription.create(
        SubscriptionId.of(subscriptionId), ownerId, programId, planId, "DIURNA");
  }
}
