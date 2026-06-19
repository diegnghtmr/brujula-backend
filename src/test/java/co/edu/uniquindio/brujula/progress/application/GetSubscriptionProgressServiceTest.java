package co.edu.uniquindio.brujula.progress.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.curriculum.application.dto.PlanMallaView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery;
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.programcatalog.application.dto.ProgramView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetProgramQuery;
import co.edu.uniquindio.brujula.progress.application.dto.ProgressSummaryView;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.service.GetSubscriptionProgressService;
import co.edu.uniquindio.brujula.progress.domain.AttemptId;
import co.edu.uniquindio.brujula.progress.domain.AttemptStatus;
import co.edu.uniquindio.brujula.progress.domain.CourseAttempt;
import co.edu.uniquindio.brujula.progress.domain.Grade;
import co.edu.uniquindio.brujula.progress.domain.Subscription;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionId;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GetSubscriptionProgressService}.
 *
 * <p>Scenarios: happy path, zero attempts, disclaimer always present, ownership guard.
 *
 * <p>{@code GetProgramQuery} is mocked; programName assertion added.
 */
class GetSubscriptionProgressServiceTest {

  private LoadSubscriptionPort loadSubscriptionPort;
  private LoadCourseAttemptPort loadCourseAttemptPort;
  private AssemblePlanMallaQuery assemblePlanMallaQuery;
  private GetProgramQuery getProgramQuery;
  private ListPeriodsQuery listPeriodsQuery;
  private GetSubscriptionProgressService sut;

  private static final UUID STUDENT_ID = UUID.randomUUID();
  private static final UUID OTHER_ID = UUID.randomUUID();
  private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
  private static final UUID PLAN_ID = UUID.randomUUID();
  private static final UUID PROGRAM_ID = UUID.randomUUID();
  private static final UUID COURSE_1 = UUID.randomUUID();
  private static final UUID COURSE_2 = UUID.randomUUID();
  // Expected human-readable program name from catalog
  private static final String PROGRAM_NAME = "Ingeniería de Sistemas y Computación";

  @BeforeEach
  void setUp() {
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    loadCourseAttemptPort = mock(LoadCourseAttemptPort.class);
    assemblePlanMallaQuery = mock(AssemblePlanMallaQuery.class);
    getProgramQuery = mock(GetProgramQuery.class);
    listPeriodsQuery = mock(ListPeriodsQuery.class);
    // Default: empty period catalog so name resolution is a no-op unless a test overrides it.
    when(listPeriodsQuery.listPeriods(null)).thenReturn(List.of());

    // Stub program name resolution
    when(getProgramQuery.getProgram(PROGRAM_ID))
        .thenReturn(new ProgramView(PROGRAM_ID, PROGRAM_NAME, "Engineering", "ACTIVE", 0L));

    sut =
        new GetSubscriptionProgressService(
            loadSubscriptionPort,
            loadCourseAttemptPort,
            assemblePlanMallaQuery,
            getProgramQuery,
            listPeriodsQuery);
  }

  // -----------------------------------------------------------------------
  // Ownership guard
  // -----------------------------------------------------------------------

  @Test
  void bola_returns_not_found_for_wrong_owner() {
    Subscription sub = buildSubscription(OTHER_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));

    assertThatThrownBy(() -> sut.getSubscriptionProgress(SUBSCRIPTION_ID, STUDENT_ID))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }

  // -----------------------------------------------------------------------
  // Zero attempts
  // -----------------------------------------------------------------------

  @Test
  void zero_attempts_returns_zero_approved_and_null_average() {
    Subscription sub = buildSubscription(STUDENT_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));
    when(assemblePlanMallaQuery.assemblePlanMalla(eq(PLAN_ID), any())).thenReturn(buildMalla());
    when(loadCourseAttemptPort.loadByUserIdAndProgramId(STUDENT_ID, PROGRAM_ID))
        .thenReturn(List.of());

    ProgressSummaryView result = sut.getSubscriptionProgress(SUBSCRIPTION_ID, STUDENT_ID);

    // programName must be the human-readable catalog name, not a UUID
    assertThat(result.programName()).isEqualTo(PROGRAM_NAME);
    assertThat(result.approvedCredits()).isEqualTo(0.0);
    assertThat(result.progressPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.estimatedHistoricalAverage()).isNull();
    assertThat(result.averagesByPeriod()).isEmpty();
    assertThat(result.disclaimer()).isNotBlank();
  }

  // -----------------------------------------------------------------------
  // Mixed attempts
  // -----------------------------------------------------------------------

  @Test
  void mixed_attempts_compute_credits_correctly() {
    Subscription sub = buildSubscription(STUDENT_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));
    when(assemblePlanMallaQuery.assemblePlanMalla(eq(PLAN_ID), any())).thenReturn(buildMalla());

    // COURSE_1 PASSED (4.0), COURSE_2 FAILED (2.0) — each 3 credits
    CourseAttempt passed =
        CourseAttempt.reconstitute(
            AttemptId.generate(),
            new SubscriptionId(SUBSCRIPTION_ID),
            COURSE_1,
            AttemptStatus.PASSED,
            new Grade(new BigDecimal("4.0")),
            UUID.randomUUID(),
            "DIURNA",
            Instant.now(),
            0L);
    CourseAttempt failed =
        CourseAttempt.reconstitute(
            AttemptId.generate(),
            new SubscriptionId(SUBSCRIPTION_ID),
            COURSE_2,
            AttemptStatus.FAILED,
            new Grade(new BigDecimal("2.0")),
            UUID.randomUUID(),
            "DIURNA",
            Instant.now(),
            0L);
    when(loadCourseAttemptPort.loadByUserIdAndProgramId(STUDENT_ID, PROGRAM_ID))
        .thenReturn(List.of(passed, failed));

    ProgressSummaryView result = sut.getSubscriptionProgress(SUBSCRIPTION_ID, STUDENT_ID);

    assertThat(result.totalPlanCredits()).isEqualTo(6.0);
    assertThat(result.approvedCredits()).isEqualTo(3.0);
    assertThat(result.pendingCredits()).isEqualTo(3.0);
    assertThat(result.passedCourses()).isEqualTo(1);
    assertThat(result.pendingFailedCourses()).isEqualTo(1);
    assertThat(result.disclaimer()).isNotBlank();
  }

  // -----------------------------------------------------------------------
  // Per-period averages carry the human-readable period name (not the UUID)
  // -----------------------------------------------------------------------

  @Test
  void period_averages_resolve_human_readable_period_names() {
    UUID periodId = UUID.fromString("00000004-0000-4000-a000-000000000001");
    Subscription sub = buildSubscription(STUDENT_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));
    when(assemblePlanMallaQuery.assemblePlanMalla(eq(PLAN_ID), any())).thenReturn(buildMalla());
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

    CourseAttempt passed =
        CourseAttempt.reconstitute(
            AttemptId.generate(),
            new SubscriptionId(SUBSCRIPTION_ID),
            COURSE_1,
            AttemptStatus.PASSED,
            new Grade(new BigDecimal("4.0")),
            periodId,
            "DIURNA",
            Instant.now(),
            0L);
    when(loadCourseAttemptPort.loadByUserIdAndProgramId(STUDENT_ID, PROGRAM_ID))
        .thenReturn(List.of(passed));

    ProgressSummaryView result = sut.getSubscriptionProgress(SUBSCRIPTION_ID, STUDENT_ID);

    assertThat(result.averagesByPeriod()).isNotEmpty();
    assertThat(result.averagesByPeriod().get(0).periodId()).isEqualTo(periodId);
    assertThat(result.averagesByPeriod().get(0).periodName()).isEqualTo("2024-1");
  }

  // -----------------------------------------------------------------------
  // Non-empty disclaimer always present
  // -----------------------------------------------------------------------

  @Test
  void disclaimer_always_present() {
    Subscription sub = buildSubscription(STUDENT_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));
    when(assemblePlanMallaQuery.assemblePlanMalla(eq(PLAN_ID), any())).thenReturn(buildMalla());
    when(loadCourseAttemptPort.loadByUserIdAndProgramId(STUDENT_ID, PROGRAM_ID))
        .thenReturn(List.of());

    ProgressSummaryView result = sut.getSubscriptionProgress(SUBSCRIPTION_ID, STUDENT_ID);

    assertThat(result.disclaimer()).isNotBlank();
  }

  // -----------------------------------------------------------------------
  // N+1 absence: single bulk-load calls
  // -----------------------------------------------------------------------

  @Test
  void bulk_load_uses_single_calls() {
    Subscription sub = buildSubscription(STUDENT_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));
    when(assemblePlanMallaQuery.assemblePlanMalla(eq(PLAN_ID), any())).thenReturn(buildMalla());
    when(loadCourseAttemptPort.loadByUserIdAndProgramId(STUDENT_ID, PROGRAM_ID))
        .thenReturn(List.of());

    sut.getSubscriptionProgress(SUBSCRIPTION_ID, STUDENT_ID);

    verify(assemblePlanMallaQuery, times(1)).assemblePlanMalla(any(), any());
    verify(loadCourseAttemptPort, times(1)).loadByUserIdAndProgramId(any(), any());
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private Subscription buildSubscription(UUID userId) {
    return Subscription.reconstitute(
        new SubscriptionId(SUBSCRIPTION_ID),
        userId,
        PROGRAM_ID,
        PLAN_ID,
        "DIURNA",
        SubscriptionStatus.ACTIVE,
        Instant.now(),
        0L);
  }

  private PlanMallaView buildMalla() {
    var course1 =
        new PlanMallaView.CourseSummary(COURSE_1, "C1", "Course 1", "REQUIRED", 3, List.of());
    var course2 =
        new PlanMallaView.CourseSummary(COURSE_2, "C2", "Course 2", "REQUIRED", 3, List.of());
    var bucket = new PlanMallaView.SemesterBucket(1, List.of(course1, course2));
    return new PlanMallaView(PLAN_ID, "DIURNA", List.of(bucket), null);
  }
}
