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
import co.edu.uniquindio.brujula.programcatalog.application.dto.JourneyRef;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListJourneysQuery;
import co.edu.uniquindio.brujula.progress.application.dto.CurriculumViewData;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadCourseAttemptPort;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.service.GetSubscriptionCurriculumService;
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
 * Unit tests for {@link GetSubscriptionCurriculumService}.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>Cross-student subscription access → 404.
 *   <li>Happy path: curriculum view organized by semester with correct progressStatus.
 *   <li>No N+1: verify single call to AssemblePlanMallaQuery.
 * </ul>
 *
 * <p>{@code ListJourneysQuery} is mocked; journey name assertion added.
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
  // Expected human-readable journey name from catalog
  private static final UUID JOURNEY_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    loadCourseAttemptPort = mock(LoadCourseAttemptPort.class);
    assemblePlanMallaQuery = mock(AssemblePlanMallaQuery.class);
    listJourneysQuery = mock(ListJourneysQuery.class);

    // Stub journey name resolution (DIURNA → "Jornada Diurna")
    when(listJourneysQuery.listJourneys())
        .thenReturn(
            List.of(
                new JourneyRef(JOURNEY_ID, "DIURNA", "Jornada Diurna"),
                new JourneyRef(UUID.randomUUID(), "NOCTURNA", "Jornada Nocturna")));

    sut =
        new GetSubscriptionCurriculumService(
            loadSubscriptionPort, loadCourseAttemptPort, assemblePlanMallaQuery, listJourneysQuery);
  }

  // -----------------------------------------------------------------------
  // Another student's subscription → 404
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
    CourseAttempt attempt =
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
    when(loadCourseAttemptPort.loadByUserIdAndProgramId(STUDENT_ID, PROGRAM_ID))
        .thenReturn(List.of(attempt));

    CurriculumViewData result = sut.getSubscriptionCurriculum(SUBSCRIPTION_ID, STUDENT_ID);

    assertThat(result.subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
    // Journey name must be human-readable, not the raw code
    assertThat(result.referenceJourneyCode()).isEqualTo("DIURNA");
    assertThat(result.referenceJourneyName()).isEqualTo("Jornada Diurna");
    assertThat(result.semesters()).hasSize(1);
    assertThat(result.semesters().get(0).recommendedSemester()).isEqualTo(1);
    assertThat(result.semesters().get(0).courses()).hasSize(2);

    // COURSE_1 is PASSED
    var course1Data =
        result.semesters().get(0).courses().stream()
            .filter(c -> c.courseId().equals(COURSE_1))
            .findFirst()
            .orElseThrow();
    assertThat(course1Data.progressStatus()).isEqualTo("PASSED");

    // COURSE_2 is AVAILABLE (no prereqs, no attempts)
    var course2Data =
        result.semesters().get(0).courses().stream()
            .filter(c -> c.courseId().equals(COURSE_2))
            .findFirst()
            .orElseThrow();
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
    when(loadCourseAttemptPort.loadByUserIdAndProgramId(STUDENT_ID, PROGRAM_ID))
        .thenReturn(List.of());

    sut.getSubscriptionCurriculum(SUBSCRIPTION_ID, STUDENT_ID);

    // Exactly ONE call to curriculum port — no N+1
    verify(assemblePlanMallaQuery, times(1)).assemblePlanMalla(any(), any());
    verify(loadCourseAttemptPort, times(1)).loadByUserIdAndProgramId(any(), any());
  }

  // -----------------------------------------------------------------------
  // PROP-001 (REQ-6, design D5): 4 distinct passed pool electives → 4 named entries, no
  // collapse; the k-th passed pool elective (ordered by periodId, then registeredAt) maps to
  // the k-th numbered display slot, showing that elective's name.
  // -----------------------------------------------------------------------

  private static final UUID SLOT_I = UUID.randomUUID();
  private static final UUID SLOT_II = UUID.randomUUID();
  private static final UUID SLOT_III = UUID.randomUUID();
  private static final UUID SLOT_IV = UUID.randomUUID();
  private static final UUID ELECTIVE_DISENO = UUID.randomUUID();
  private static final UUID ELECTIVE_MOVILES = UUID.randomUUID();
  private static final UUID ELECTIVE_USABILIDAD = UUID.randomUUID();
  private static final UUID ELECTIVE_NUBE = UUID.randomUUID();
  private static final UUID PERIOD_1 = UUID.fromString("00000000-0000-4000-a000-000000000001");
  private static final UUID PERIOD_2 = UUID.fromString("00000000-0000-4000-a000-000000000002");
  private static final UUID PERIOD_3 = UUID.fromString("00000000-0000-4000-a000-000000000003");
  private static final UUID PERIOD_4 = UUID.fromString("00000000-0000-4000-a000-000000000004");

  @Test
  void fourDistinctPassedElectives_fourNamedEntries_mappedToNumberedSlotsInPassOrder() {
    Subscription sub = buildSubscription(SUBSCRIPTION_ID, STUDENT_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));
    when(assemblePlanMallaQuery.assemblePlanMalla(eq(PLAN_ID), any()))
        .thenReturn(buildMallaWithPoolElectives());

    // Passed in this chronological order: Diseño (p1), Móviles (p2), Usabilidad (p3), Nube (p4)
    List<CourseAttempt> attempts =
        List.of(
            passedAttempt(ELECTIVE_DISENO, PERIOD_1, Instant.parse("2025-01-01T00:00:00Z")),
            passedAttempt(ELECTIVE_MOVILES, PERIOD_2, Instant.parse("2025-06-01T00:00:00Z")),
            passedAttempt(ELECTIVE_USABILIDAD, PERIOD_3, Instant.parse("2025-11-01T00:00:00Z")),
            passedAttempt(ELECTIVE_NUBE, PERIOD_4, Instant.parse("2026-06-01T00:00:00Z")));
    when(loadCourseAttemptPort.loadByUserIdAndProgramId(STUDENT_ID, PROGRAM_ID))
        .thenReturn(attempts);

    CurriculumViewData result = sut.getSubscriptionCurriculum(SUBSCRIPTION_ID, STUDENT_ID);

    var allCourses = result.semesters().stream().flatMap(s -> s.courses().stream()).toList();

    // 4 distinct named entries — no collapse onto a single shared slot
    var slotI = findByCourseId(allCourses, SLOT_I);
    var slotII = findByCourseId(allCourses, SLOT_II);
    var slotIII = findByCourseId(allCourses, SLOT_III);
    var slotIV = findByCourseId(allCourses, SLOT_IV);

    assertThat(slotI.progressStatus()).isEqualTo("PASSED");
    assertThat(slotI.filledElectiveName()).isEqualTo("Diseño Centrado en el Usuario");

    assertThat(slotII.progressStatus()).isEqualTo("PASSED");
    assertThat(slotII.filledElectiveName())
        .isEqualTo("Desarrollo de Aplicaciones para Dispositivos Móviles");

    assertThat(slotIII.progressStatus()).isEqualTo("PASSED");
    assertThat(slotIII.filledElectiveName()).isEqualTo("Usabilidad y Experiencia de Usuario");

    assertThat(slotIV.progressStatus()).isEqualTo("PASSED");
    assertThat(slotIV.filledElectiveName()).isEqualTo("Computación en la Nube");
  }

  @Test
  void thesisGate_blockedWithZeroPassedElectives_describesUnmetWithoutThrowing() {
    // Regression guard: a CONFIGURABLE pool-gate unmet requirement has requiredCourseId == null
    // by construction (no single required course). describeUnmet() must handle that gracefully
    // instead of NPE-ing on requiredCourseId().toString().
    Subscription sub = buildSubscription(SUBSCRIPTION_ID, STUDENT_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));
    when(assemblePlanMallaQuery.assemblePlanMalla(eq(PLAN_ID), any()))
        .thenReturn(buildMallaWithPoolElectivesAndThesisGate());
    when(loadCourseAttemptPort.loadByUserIdAndProgramId(STUDENT_ID, PROGRAM_ID))
        .thenReturn(List.of()); // zero passed electives — thesis gate unmet

    CurriculumViewData result = sut.getSubscriptionCurriculum(SUBSCRIPTION_ID, STUDENT_ID);

    var allCourses = result.semesters().stream().flatMap(s -> s.courses().stream()).toList();
    var thesis = findByCourseId(allCourses, TDG_ID);

    assertThat(thesis.progressStatus()).isEqualTo("BLOCKED");
    assertThat(thesis.unmetRequirements()).isNotEmpty();
  }

  @Test
  void thesisGate_availableOnceFourDistinctElectivesPassed() {
    Subscription sub = buildSubscription(SUBSCRIPTION_ID, STUDENT_ID);
    when(loadSubscriptionPort.loadById(SUBSCRIPTION_ID)).thenReturn(Optional.of(sub));
    when(assemblePlanMallaQuery.assemblePlanMalla(eq(PLAN_ID), any()))
        .thenReturn(buildMallaWithPoolElectivesAndThesisGate());

    List<CourseAttempt> attempts =
        List.of(
            passedAttempt(ELECTIVE_DISENO, PERIOD_1, Instant.parse("2025-01-01T00:00:00Z")),
            passedAttempt(ELECTIVE_MOVILES, PERIOD_2, Instant.parse("2025-06-01T00:00:00Z")),
            passedAttempt(ELECTIVE_USABILIDAD, PERIOD_3, Instant.parse("2025-11-01T00:00:00Z")),
            passedAttempt(ELECTIVE_NUBE, PERIOD_4, Instant.parse("2026-06-01T00:00:00Z")));
    when(loadCourseAttemptPort.loadByUserIdAndProgramId(STUDENT_ID, PROGRAM_ID))
        .thenReturn(attempts);

    CurriculumViewData result = sut.getSubscriptionCurriculum(SUBSCRIPTION_ID, STUDENT_ID);

    var allCourses = result.semesters().stream().flatMap(s -> s.courses().stream()).toList();
    var thesis = findByCourseId(allCourses, TDG_ID);

    assertThat(thesis.progressStatus()).isEqualTo("AVAILABLE");
  }

  private static final UUID TDG_ID = UUID.randomUUID();

  private co.edu.uniquindio.brujula.progress.application.dto.CurriculumCourseData findByCourseId(
      List<co.edu.uniquindio.brujula.progress.application.dto.CurriculumCourseData> courses,
      UUID courseId) {
    return courses.stream().filter(c -> c.courseId().equals(courseId)).findFirst().orElseThrow();
  }

  private CourseAttempt passedAttempt(UUID courseId, UUID periodId, Instant registeredAt) {
    return CourseAttempt.reconstitute(
        AttemptId.generate(),
        new SubscriptionId(SUBSCRIPTION_ID),
        courseId,
        AttemptStatus.PASSED,
        new Grade(new BigDecimal("4.0")),
        periodId,
        "DIURNA",
        registeredAt,
        0L);
  }

  private PlanMallaView buildMallaWithPoolElectives() {
    var slot1 =
        new PlanMallaView.CourseSummary(
            SLOT_I, "ISC-S8-PROFUNDIZACIONI", "Profundización I", "DEEPENING", 3, List.of());
    var slot2 =
        new PlanMallaView.CourseSummary(
            SLOT_II, "ISC-S8-PROFUNDIZACIONII", "Profundización II", "DEEPENING", 3, List.of());
    var slot3 =
        new PlanMallaView.CourseSummary(
            SLOT_III, "ISC-S9-PROFUNDIZACIONIII", "Profundización III", "DEEPENING", 3, List.of());
    var slot4 =
        new PlanMallaView.CourseSummary(
            SLOT_IV, "ISC-S9-PROFUNDIZACIONIV", "Profundización IV", "DEEPENING", 3, List.of());
    var bucket8 = new PlanMallaView.SemesterBucket(8, List.of(slot1, slot2));
    var bucket9 = new PlanMallaView.SemesterBucket(9, List.of(slot3, slot4));

    var poolElectiveDiseno =
        new PlanMallaView.CourseSummary(
            ELECTIVE_DISENO,
            "ISC-DEEP-DISENOCENTRADOENELUSUARIO",
            "Diseño Centrado en el Usuario",
            "DEEPENING",
            3,
            List.of());
    var poolElectiveMoviles =
        new PlanMallaView.CourseSummary(
            ELECTIVE_MOVILES,
            "ISC-DEEP-DESARROLLODEAPLICACIONESPARADISPOSITIVOSMOVILES",
            "Desarrollo de Aplicaciones para Dispositivos Móviles",
            "DEEPENING",
            3,
            List.of());
    var poolElectiveUsabilidad =
        new PlanMallaView.CourseSummary(
            ELECTIVE_USABILIDAD,
            "ISC-DEEP-USABILIDADYEXPERIENCIADEUSUARIO",
            "Usabilidad y Experiencia de Usuario",
            "DEEPENING",
            3,
            List.of());
    var poolElectiveNube =
        new PlanMallaView.CourseSummary(
            ELECTIVE_NUBE,
            "ISC-DEEP-COMPUTACIONENLANUBE",
            "Computación en la Nube",
            "DEEPENING",
            3,
            List.of());

    return new PlanMallaView(
        PLAN_ID,
        "DIURNA",
        List.of(bucket8, bucket9),
        "Graduation requirements.",
        List.of(poolElectiveDiseno, poolElectiveMoviles, poolElectiveUsabilidad, poolElectiveNube));
  }

  private PlanMallaView buildMallaWithPoolElectivesAndThesisGate() {
    PlanMallaView base = buildMallaWithPoolElectives();
    var thesisRequirement =
        new PlanMallaView.RequirementSummary("CONFIGURABLE", null, null, null, null, 4);
    var thesis =
        new PlanMallaView.CourseSummary(
            TDG_ID,
            "ISC-S10-TRABAJODEGRADO",
            "Trabajo de Grado",
            "DEGREE_WORK",
            6,
            List.of(thesisRequirement));
    var bucket10 = new PlanMallaView.SemesterBucket(10, List.of(thesis));

    List<PlanMallaView.SemesterBucket> buckets = new java.util.ArrayList<>(base.semesterBuckets());
    buckets.add(bucket10);

    return new PlanMallaView(
        PLAN_ID, "DIURNA", buckets, base.graduationRequirements(), base.poolCourses());
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
    var course1 =
        new PlanMallaView.CourseSummary(COURSE_1, "ISC-001", "Course 1", "REQUIRED", 3, List.of());
    var course2 =
        new PlanMallaView.CourseSummary(COURSE_2, "ISC-002", "Course 2", "REQUIRED", 3, List.of());
    var bucket = new PlanMallaView.SemesterBucket(1, List.of(course1, course2));
    return new PlanMallaView(PLAN_ID, "DIURNA", List.of(bucket), "Graduation requirements.");
  }
}
