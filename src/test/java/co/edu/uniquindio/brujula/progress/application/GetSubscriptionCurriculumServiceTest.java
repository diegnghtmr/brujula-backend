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
import co.edu.uniquindio.brujula.progress.application.dto.CurriculumViewData;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.service.GetSubscriptionCurriculumService;
import co.edu.uniquindio.brujula.programcatalog.application.dto.JourneyRef;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListJourneysQuery;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU4-T04 RED: unit tests for {@link GetSubscriptionCurriculumService}.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>AVL-1-BOLA: another student's subscription → 404.
 *   <li>happy path: curriculum view organized by semester with correct progressStatus.
 *   <li>N+1 absence: verify single call to AssemblePlanMallaQuery.
 * </ul>
 *
 * <p>FIX-S1: {@code ListJourneysQuery} is now mocked; journey name assertion added.
 */
class GetSubscriptionCurriculumServiceTest {

  private LoadSubscriptionPort loadSubscriptionPort;
  private LoadCourseAttemptPort loadCourseAttemptPort;
  private AssemblePlanMallaQuery assemblePlanMallaQuery;
  private ListJourneysQuery listJourneysQuery;
  private GetSubscriptionCurriculumService sut;

  private static final UUID STUDENT_ID = UUID.randomUUID();
  private static final UUID OTHER_STUDENT_ID = UUID.randomUUID();
  private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
  private static final UUID PLAN_ID = UUID.randomUUID();
  private static final UUID PROGRAM_ID = UUID.randomUUID();
  private static final UUID COURSE_1 = UUID.randomUUID();
  private static final UUID COURSE_2 = UUID.randomUUID();
  // FIX-S1: expected human-readable journey name from catalog
  private static final UUID JOURNEY_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    loadCourseAttemptPort = mock(LoadCourseAttemptPort.class);
    assemblePlanMallaQuery = mock(AssemblePlanMallaQuery.class);
    listJourneysQuery = mock(ListJourneysQuery.class);

    // FIX-S1: stub journey name resolution (DIURNA → "Jornada Diurna")
    when(listJourneysQuery.listJourneys()).thenReturn(List.of(
        new JourneyRef(JOURNEY_ID, "DIURNA", "Jornada Diurna"),
        new JourneyRef(UUID.randomUUID(), "NOCTURNA", "Jornada Nocturna")));

    sut = new GetSubscriptionCurriculumService(
        loadSubscriptionPort, loadCourseAttemptPort, assemblePlanMallaQuery, listJourneysQuery);
  }

  // -----------------------------------------------------------------------
  // AVL-1-BOLA: another student's subscription → 404
  // -----------------------------------------------------------------------

  @Test
  void bola_returns_not_found_for_wrong_owner() {
    Subscription sub = buildSubscription(SUBSCRIPTION_ID, OTHER_STUDENT_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));

    assertThatThrownBy(() -> sut.getSubscriptionCurriculum(SUBSCRIPTION_ID, STUDENT_ID))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }

  @Test
  void subscription_not_found_throws_not_found() {
    when(loadSubscriptionPort.loadById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.getSubscriptionCurriculum(SUBSCRIPTION_ID, STUDENT_ID))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }

  // -----------------------------------------------------------------------
  // Happy path: curriculum view with correct progressStatus
  // -----------------------------------------------------------------------

  @Test
  void curriculum_view_has_semester_buckets_with_computed_status() {
    Subscription sub = buildSubscription(SUBSCRIPTION_ID, STUDENT_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));

    // Plan malla: semester 1 with two courses
    PlanMallaView malla = buildMalla();
    when(assemblePlanMallaQuery.assemblePlanMalla(eq(PLAN_ID), any())).thenReturn(malla);

    // Student has PASSED attempt for COURSE_1
    CourseAttempt attempt = CourseAttempt.reconstitute(
        AttemptId.generate(), new SubscriptionId(SUBSCRIPTION_ID), COURSE_1,
        AttemptStatus.PASSED, new Grade(new BigDecimal("4.0")), UUID.randomUUID(),
        "DIURNA", Instant.now(), 0L);
    when(loadCourseAttemptPort.loadBySubscriptionId(SUBSCRIPTION_ID)).thenReturn(List.of(attempt));

    CurriculumViewData result = sut.getSubscriptionCurriculum(SUBSCRIPTION_ID, STUDENT_ID);

    assertThat(result.subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
    // FIX-S1: journey name must be human-readable, not the raw code
    assertThat(result.referenceJourneyCode()).isEqualTo("DIURNA");
    assertThat(result.referenceJourneyName()).isEqualTo("Jornada Diurna");
    assertThat(result.semesters()).hasSize(1);
    assertThat(result.semesters().get(0).recommendedSemester()).isEqualTo(1);
    assertThat(result.semesters().get(0).courses()).hasSize(2);

    // COURSE_1 is PASSED
    var course1Data = result.semesters().get(0).courses().stream()
        .filter(c -> c.courseId().equals(COURSE_1))
        .findFirst().orElseThrow();
    assertThat(course1Data.progressStatus()).isEqualTo("PASSED");

    // COURSE_2 is AVAILABLE (no prereqs, no attempts)
    var course2Data = result.semesters().get(0).courses().stream()
        .filter(c -> c.courseId().equals(COURSE_2))
        .findFirst().orElseThrow();
    assertThat(course2Data.progressStatus()).isEqualTo("AVAILABLE");
  }

  // -----------------------------------------------------------------------
  // N+1 absence: single call to AssemblePlanMallaQuery
  // -----------------------------------------------------------------------

  @Test
  void bulk_load_uses_single_call_to_assemble_plan_malla() {
    Subscription sub = buildSubscription(SUBSCRIPTION_ID, STUDENT_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));
    when(assemblePlanMallaQuery.assemblePlanMalla(eq(PLAN_ID), any())).thenReturn(buildMalla());
    when(loadCourseAttemptPort.loadBySubscriptionId(SUBSCRIPTION_ID)).thenReturn(List.of());

    sut.getSubscriptionCurriculum(SUBSCRIPTION_ID, STUDENT_ID);

    // Exactly ONE call to curriculum port — no N+1
    verify(assemblePlanMallaQuery, times(1)).assemblePlanMalla(any(), any());
    verify(loadCourseAttemptPort, times(1)).loadBySubscriptionId(any());
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private Subscription buildSubscription(UUID subscriptionId, UUID userId) {
    return Subscription.reconstitute(
        new SubscriptionId(subscriptionId),
        userId,
        PROGRAM_ID,
        PLAN_ID,
        "DIURNA",
        SubscriptionStatus.ACTIVE,
        Instant.now(),
        0L);
  }

  private PlanMallaView buildMalla() {
    var req1 = new PlanMallaView.RequirementSummary("COURSE_PASSED", null, "HIGH", false, null);
    var course1 = new PlanMallaView.CourseSummary(COURSE_1, "ISC-001", "Course 1", "REQUIRED", 3, List.of());
    var course2 = new PlanMallaView.CourseSummary(COURSE_2, "ISC-002", "Course 2", "REQUIRED", 3, List.of());
    var bucket = new PlanMallaView.SemesterBucket(1, List.of(course1, course2));
    return new PlanMallaView(PLAN_ID, "DIURNA", List.of(bucket), "Graduation requirements.");
  }
}
