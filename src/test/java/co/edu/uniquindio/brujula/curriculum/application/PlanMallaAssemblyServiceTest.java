package co.edu.uniquindio.brujula.curriculum.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.curriculum.application.dto.PlanMallaView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.application.service.PlanMallaAssemblyService;
import co.edu.uniquindio.brujula.curriculum.domain.Confidence;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import co.edu.uniquindio.brujula.curriculum.domain.RecommendedSemester;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Mockito unit tests for {@link PlanMallaAssemblyService}.
 *
 * <p>Validates: grouping by recommended semester, journey filtering, INACTIVE course exclusion,
 * ordering of semester buckets, requirement edges per course (CUR-008 §2), no N+1 on requirements,
 * and graduation requirements advisory text (CUR-008 §3).
 */
class PlanMallaAssemblyServiceTest {

  private LoadCoursePort loadCoursePort;
  private LoadCourseRequirementPort loadCourseRequirementPort;
  private AssemblePlanMallaQuery assemblePlanMallaQuery;

  @BeforeEach
  void setUp() {
    loadCoursePort = mock(LoadCoursePort.class);
    loadCourseRequirementPort = mock(LoadCourseRequirementPort.class);
    assemblePlanMallaQuery =
        new PlanMallaAssemblyService(loadCoursePort, loadCourseRequirementPort);
  }

  // ---------------------------------------------------------------------------
  // assemble: basic grouping
  // ---------------------------------------------------------------------------

  @Test
  void assemble_groups_active_courses_by_semester_for_given_journey() {
    UUID planId = UUID.randomUUID();
    UUID c1Id = UUID.randomUUID();
    UUID c2Id = UUID.randomUUID();
    UUID c3Id = UUID.randomUUID();

    Course course1 = makeCourse(c1Id, planId, "Cálculo I", 1, "DIURNA", CourseStatus.ACTIVE);
    Course course2 = makeCourse(c2Id, planId, "Álgebra", 1, "DIURNA", CourseStatus.ACTIVE);
    Course course3 = makeCourse(c3Id, planId, "Cálculo II", 2, "DIURNA", CourseStatus.ACTIVE);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(course1, course2, course3));
    when(loadCourseRequirementPort.loadByTargetCourseIds(anyList())).thenReturn(List.of());

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    assertThat(result.planId()).isEqualTo(planId);
    assertThat(result.journeyCode()).isEqualTo("DIURNA");
    assertThat(result.semesterBuckets()).hasSize(2);

    PlanMallaView.SemesterBucket sem1 = result.semesterBuckets().get(0);
    assertThat(sem1.recommendedSemester()).isEqualTo(1);
    assertThat(sem1.courses())
        .extracting(PlanMallaView.CourseSummary::id)
        .containsExactlyInAnyOrder(c1Id, c2Id);

    PlanMallaView.SemesterBucket sem2 = result.semesterBuckets().get(1);
    assertThat(sem2.recommendedSemester()).isEqualTo(2);
    assertThat(sem2.courses()).extracting(PlanMallaView.CourseSummary::id).containsExactly(c3Id);
  }

  @Test
  void assemble_excludes_inactive_and_archived_courses() {
    UUID planId = UUID.randomUUID();
    UUID activeId = UUID.randomUUID();
    UUID inactiveId = UUID.randomUUID();
    UUID archivedId = UUID.randomUUID();

    Course active = makeCourse(activeId, planId, "Física I", 1, "DIURNA", CourseStatus.ACTIVE);
    Course inactive =
        makeCourse(inactiveId, planId, "Electiva X", 1, "DIURNA", CourseStatus.INACTIVE);
    Course archived =
        makeCourse(archivedId, planId, "Materia Vieja", 2, "DIURNA", CourseStatus.ARCHIVED);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(active, inactive, archived));
    when(loadCourseRequirementPort.loadByTargetCourseIds(anyList())).thenReturn(List.of());

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    assertThat(result.semesterBuckets()).hasSize(1);
    assertThat(result.semesterBuckets().get(0).courses()).hasSize(1);
    assertThat(result.semesterBuckets().get(0).courses().get(0).id()).isEqualTo(activeId);
  }

  @Test
  void assemble_filters_by_journey_code() {
    UUID planId = UUID.randomUUID();
    UUID diurnaOnly = UUID.randomUUID();
    UUID nocturnaOnly = UUID.randomUUID();

    Course courseD =
        makeCourse(diurnaOnly, planId, "Sistemas Digitales", 3, "DIURNA", CourseStatus.ACTIVE);
    Course courseN = makeCourse(nocturnaOnly, planId, "Redes", 4, "NOCTURNA", CourseStatus.ACTIVE);
    // Add NOCTURNA semester for courseD too (multi-journey)
    Course courseDandN = courseWithTwoJourneys(planId, "Álgebra Lineal", 2, 3);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(courseD, courseN, courseDandN));
    when(loadCourseRequirementPort.loadByTargetCourseIds(anyList())).thenReturn(List.of());

    PlanMallaView resultDiurna = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");
    PlanMallaView resultNocturna = assemblePlanMallaQuery.assemblePlanMalla(planId, "NOCTURNA");

    // DIURNA result should only include courses with DIURNA recommended semester
    assertThat(extractCourseIds(resultDiurna)).contains(diurnaOnly);
    assertThat(extractCourseIds(resultDiurna)).doesNotContain(nocturnaOnly);

    // NOCTURNA result should only include courses with NOCTURNA recommended semester
    assertThat(extractCourseIds(resultNocturna)).contains(nocturnaOnly);
    assertThat(extractCourseIds(resultNocturna)).doesNotContain(diurnaOnly);
  }

  @Test
  void assemble_returns_semester_buckets_ordered_ascending() {
    UUID planId = UUID.randomUUID();

    // Insert in reverse order to verify sorting
    Course sem3 =
        makeCourse(UUID.randomUUID(), planId, "Estructuras", 3, "DIURNA", CourseStatus.ACTIVE);
    Course sem1 =
        makeCourse(UUID.randomUUID(), planId, "Cálculo I", 1, "DIURNA", CourseStatus.ACTIVE);
    Course sem2 =
        makeCourse(UUID.randomUUID(), planId, "Cálculo II", 2, "DIURNA", CourseStatus.ACTIVE);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(sem3, sem1, sem2));
    when(loadCourseRequirementPort.loadByTargetCourseIds(anyList())).thenReturn(List.of());

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    assertThat(result.semesterBuckets())
        .extracting(PlanMallaView.SemesterBucket::recommendedSemester)
        .containsExactly(1, 2, 3);
  }

  @Test
  void assemble_empty_plan_returns_empty_buckets() {
    UUID planId = UUID.randomUUID();
    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of());
    when(loadCourseRequirementPort.loadByTargetCourseIds(anyList())).thenReturn(List.of());

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    assertThat(result.semesterBuckets()).isEmpty();
  }

  @Test
  void assemble_maps_course_fields_correctly() {
    UUID planId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    Course course =
        Course.reconstitute(
            new CourseId(courseId),
            planId,
            UUID.randomUUID(),
            "ISC-S1-PROG1",
            "Programación I",
            CourseType.REQUIRED,
            4,
            4,
            List.of(new RecommendedSemester("DIURNA", 1)),
            CourseStatus.ACTIVE,
            0L);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(course));
    when(loadCourseRequirementPort.loadByTargetCourseIds(anyList())).thenReturn(List.of());

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    PlanMallaView.CourseSummary cs = result.semesterBuckets().get(0).courses().get(0);
    assertThat(cs.id()).isEqualTo(courseId);
    assertThat(cs.officialCode()).isEqualTo("ISC-S1-PROG1");
    assertThat(cs.name()).isEqualTo("Programación I");
    assertThat(cs.credits()).isEqualTo(4);
    assertThat(cs.type()).isEqualTo("REQUIRED");
  }

  // ---------------------------------------------------------------------------
  // FIX-1 (C1, CUR-008 §2): Each course must carry its requirement edges
  // ---------------------------------------------------------------------------

  @Test
  void assemble_each_malla_course_carries_its_requirement_edges_with_confidence_and_inferred() {
    UUID planId = UUID.randomUUID();
    UUID progI_id = UUID.randomUUID();
    UUID calcI_id = UUID.randomUUID();
    UUID reqId = UUID.randomUUID();

    Course progI = makeCourse(progI_id, planId, "Programación I", 1, "DIURNA", CourseStatus.ACTIVE);
    Course calcI = makeCourse(calcI_id, planId, "Cálculo I", 1, "DIURNA", CourseStatus.ACTIVE);

    // Programación I has a COURSE_PASSED requirement on Cálculo I (confidence=HIGH, inferred=false)
    CourseRequirement req =
        CourseRequirement.coursePassed(
            new CourseRequirementId(reqId), progI_id, calcI_id, false, Confidence.HIGH, false);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(progI, calcI));
    when(loadCourseRequirementPort.loadByTargetCourseIds(anyList())).thenReturn(List.of(req));

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    // Find ProgI in results
    PlanMallaView.CourseSummary progICs =
        result.semesterBuckets().stream()
            .flatMap(b -> b.courses().stream())
            .filter(cs -> cs.id().equals(progI_id))
            .findFirst()
            .orElseThrow();

    assertThat(progICs.requirements()).hasSize(1);
    PlanMallaView.RequirementSummary rs = progICs.requirements().get(0);
    assertThat(rs.type()).isEqualTo("COURSE_PASSED");
    assertThat(rs.requiredCourseId()).isEqualTo(calcI_id);
    assertThat(rs.confidence()).isEqualTo("HIGH");
    assertThat(rs.inferred()).isFalse();

    // Cálculo I has no requirements
    PlanMallaView.CourseSummary calcICs =
        result.semesterBuckets().stream()
            .flatMap(b -> b.courses().stream())
            .filter(cs -> cs.id().equals(calcI_id))
            .findFirst()
            .orElseThrow();

    assertThat(calcICs.requirements()).isEmpty();
  }

  @Test
  void assemble_requirements_loaded_via_single_bulk_call_not_n_plus_1() {
    UUID planId = UUID.randomUUID();

    // 5 courses — requirements should be loaded in ONE bulk call, not 5
    List<Course> courses =
        List.of(
            makeCourse(UUID.randomUUID(), planId, "Curso A", 1, "DIURNA", CourseStatus.ACTIVE),
            makeCourse(UUID.randomUUID(), planId, "Curso B", 1, "DIURNA", CourseStatus.ACTIVE),
            makeCourse(UUID.randomUUID(), planId, "Curso C", 2, "DIURNA", CourseStatus.ACTIVE),
            makeCourse(UUID.randomUUID(), planId, "Curso D", 2, "DIURNA", CourseStatus.ACTIVE),
            makeCourse(UUID.randomUUID(), planId, "Curso E", 3, "DIURNA", CourseStatus.ACTIVE));

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(courses);
    when(loadCourseRequirementPort.loadByTargetCourseIds(anyList())).thenReturn(List.of());

    assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    // Must be exactly ONE bulk call — not one per course (no N+1)
    verify(loadCourseRequirementPort, times(1)).loadByTargetCourseIds(anyList());
  }

  @Test
  void assemble_courses_with_no_requirements_carry_empty_requirements_list() {
    UUID planId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    Course course = makeCourse(courseId, planId, "Introducción", 1, "DIURNA", CourseStatus.ACTIVE);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(course));
    when(loadCourseRequirementPort.loadByTargetCourseIds(anyList())).thenReturn(List.of());

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    PlanMallaView.CourseSummary cs = result.semesterBuckets().get(0).courses().get(0);
    assertThat(cs.requirements()).isNotNull().isEmpty();
  }

  // ---------------------------------------------------------------------------
  // FIX-2 (C2, CUR-008 §3): graduationRequirements advisory field
  // ---------------------------------------------------------------------------

  @Test
  void assemble_returns_non_empty_graduation_requirements_advisory() {
    UUID planId = UUID.randomUUID();
    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of());
    when(loadCourseRequirementPort.loadByTargetCourseIds(anyList())).thenReturn(List.of());

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    assertThat(result.graduationRequirements()).isNotNull().isNotBlank();
  }

  @Test
  void assemble_graduation_requirements_references_suficiencia_lengua() {
    // CUR-008 §3: must mention suficiencia en lengua extranjera and Cátedra Multidisciplinar
    UUID planId = UUID.randomUUID();
    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of());
    when(loadCourseRequirementPort.loadByTargetCourseIds(anyList())).thenReturn(List.of());

    PlanMallaView result = assemblePlanMallaQuery.assemblePlanMalla(planId, "DIURNA");

    String advisory = result.graduationRequirements();
    // Advisory must mention both graduation gates per spec
    assertThat(advisory.toLowerCase()).contains("lengua").contains("cátedra multidisciplinar");
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private Course makeCourse(
      UUID id, UUID planId, String name, int semester, String journeyCode, CourseStatus status) {
    return Course.reconstitute(
        new CourseId(id),
        planId,
        UUID.randomUUID(),
        null,
        name,
        CourseType.REQUIRED,
        3,
        3,
        List.of(new RecommendedSemester(journeyCode, semester)),
        status,
        0L);
  }

  private Course courseWithTwoJourneys(
      UUID planId, String name, int diurnaSemester, int nocturnaSwemester) {
    return Course.reconstitute(
        new CourseId(UUID.randomUUID()),
        planId,
        UUID.randomUUID(),
        null,
        name,
        CourseType.REQUIRED,
        3,
        3,
        List.of(
            new RecommendedSemester("DIURNA", diurnaSemester),
            new RecommendedSemester("NOCTURNA", nocturnaSwemester)),
        CourseStatus.ACTIVE,
        0L);
  }

  private List<UUID> extractCourseIds(PlanMallaView view) {
    return view.semesterBuckets().stream()
        .flatMap(b -> b.courses().stream())
        .map(PlanMallaView.CourseSummary::id)
        .toList();
  }
}
