package co.edu.uniquindio.brujula.curriculum.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListPlanCoursesQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.service.ListPlanCoursesService;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import co.edu.uniquindio.brujula.curriculum.domain.RecommendedSemester;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies ListPlanCoursesService delegates to LoadCoursePort, filters ARCHIVED courses when
 * callerRole=STUDENT, and returns mapped CourseView list.
 *
 * <p>INACTIVE courses must also be hidden from STUDENT (per CourseStatus Javadoc). STAFF and ADMIN
 * see all statuses including INACTIVE.
 */
class ListPlanCoursesServiceTest {

  private LoadCoursePort loadCoursePort;
  private ListPlanCoursesQuery service;
  private UUID planId;
  private UUID programId;

  @BeforeEach
  void setUp() {
    loadCoursePort = mock(LoadCoursePort.class);
    service = new ListPlanCoursesService(loadCoursePort);
    planId = UUID.randomUUID();
    programId = UUID.randomUUID();
  }

  @Test
  void delegatesToLoadCoursePortAndReturnsMappedViews() {
    CourseId id1 = CourseId.generate();
    Course activeCourse =
        Course.reconstitute(
            id1,
            planId,
            programId,
            "ISC-S1-LOGICA",
            "Lógica de Programación",
            CourseType.REQUIRED,
            3,
            4,
            List.of(new RecommendedSemester("DIURNA", 1)),
            CourseStatus.ACTIVE,
            0L);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(activeCourse));

    List<CourseView> result = service.listPlanCourses(planId, "STAFF");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(id1.value());
    assertThat(result.get(0).name()).isEqualTo("Lógica de Programación");
    assertThat(result.get(0).officialCode()).isEqualTo("ISC-S1-LOGICA");
  }

  @Test
  void filtersArchivedCoursesForStudentRole() {
    CourseId activeId = CourseId.generate();
    CourseId archivedId = CourseId.generate();

    Course activeCourse =
        Course.reconstitute(
            activeId,
            planId,
            programId,
            "ISC-S2-PROG1",
            "Programación I",
            CourseType.REQUIRED,
            3,
            4,
            List.of(new RecommendedSemester("DIURNA", 2)),
            CourseStatus.ACTIVE,
            0L);

    Course archivedCourse =
        Course.reconstitute(
            archivedId,
            planId,
            programId,
            "ISC-S2-OLDCOURSE",
            "Old Course",
            CourseType.REQUIRED,
            2,
            2,
            List.of(),
            CourseStatus.ARCHIVED,
            1L);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(activeCourse, archivedCourse));

    List<CourseView> result = service.listPlanCourses(planId, "STUDENT");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(activeId.value());
  }

  @Test
  void staffRoleSeesArchivedCourses() {
    CourseId activeId = CourseId.generate();
    CourseId archivedId = CourseId.generate();

    Course activeCourse =
        Course.reconstitute(
            activeId,
            planId,
            programId,
            "ISC-S2-PROG1",
            "Programación I",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.ACTIVE,
            0L);
    Course archivedCourse =
        Course.reconstitute(
            archivedId,
            planId,
            programId,
            "ISC-S2-OLD",
            "Old Course",
            CourseType.REQUIRED,
            2,
            2,
            List.of(),
            CourseStatus.ARCHIVED,
            1L);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(activeCourse, archivedCourse));

    List<CourseView> result = service.listPlanCourses(planId, "STAFF");

    assertThat(result).hasSize(2);
  }

  @Test
  void returnsEmptyListWhenNoCourses() {
    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of());

    List<CourseView> result = service.listPlanCourses(planId, "STUDENT");

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // INACTIVE must be hidden from STUDENT, visible to STAFF/ADMIN
  // -------------------------------------------------------------------------

  @Test
  void inactiveCourseIsHiddenFromStudent() {
    // INACTIVE is "temporarily disabled; hidden from student views" per CourseStatus Javadoc
    CourseId activeId = CourseId.generate();
    CourseId inactiveId = CourseId.generate();

    Course activeCourse =
        Course.reconstitute(
            activeId,
            planId,
            programId,
            "ISC-S1-LOGICA",
            "Lógica",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.ACTIVE,
            0L);
    Course inactiveCourse =
        Course.reconstitute(
            inactiveId,
            planId,
            programId,
            "ISC-S2-OLD",
            "Old Inactive Course",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.INACTIVE,
            1L);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(activeCourse, inactiveCourse));

    List<CourseView> result = service.listPlanCourses(planId, "STUDENT");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(activeId.value());
  }

  @Test
  void inactiveCourseIsVisibleToStaff() {
    // STAFF should see all statuses including INACTIVE
    CourseId activeId = CourseId.generate();
    CourseId inactiveId = CourseId.generate();

    Course activeCourse =
        Course.reconstitute(
            activeId,
            planId,
            programId,
            "ISC-S1-LOGICA",
            "Lógica",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.ACTIVE,
            0L);
    Course inactiveCourse =
        Course.reconstitute(
            inactiveId,
            planId,
            programId,
            "ISC-S2-OLD",
            "Old Inactive Course",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.INACTIVE,
            1L);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(activeCourse, inactiveCourse));

    List<CourseView> result = service.listPlanCourses(planId, "STAFF");

    assertThat(result).hasSize(2);
  }

  @Test
  void inactiveCourseIsVisibleToAdmin() {
    // ADMIN should see all statuses including INACTIVE
    CourseId inactiveId = CourseId.generate();

    Course inactiveCourse =
        Course.reconstitute(
            inactiveId,
            planId,
            programId,
            "ISC-S2-OLD",
            "Old Inactive Course",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.INACTIVE,
            1L);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(inactiveCourse));

    List<CourseView> result = service.listPlanCourses(planId, "ADMIN");

    assertThat(result).hasSize(1);
  }

  // -------------------------------------------------------------------------
  // null and unknown callerRole must be treated as unprivileged (allowlist)
  // -------------------------------------------------------------------------

  @Test
  void nullCallerRoleExcludesArchivedAndInactiveCourses() {
    // null role must behave as unprivileged: only ACTIVE courses visible (allowlist pattern)
    CourseId activeId = CourseId.generate();
    CourseId archivedId = CourseId.generate();
    CourseId inactiveId = CourseId.generate();

    Course active =
        Course.reconstitute(
            activeId,
            planId,
            programId,
            "ISC-A",
            "Active Course",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.ACTIVE,
            0L);
    Course archived =
        Course.reconstitute(
            archivedId,
            planId,
            programId,
            "ISC-ARCH",
            "Archived Course",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.ARCHIVED,
            1L);
    Course inactive =
        Course.reconstitute(
            inactiveId,
            planId,
            programId,
            "ISC-INACT",
            "Inactive Course",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.INACTIVE,
            1L);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(active, archived, inactive));

    List<CourseView> result = service.listPlanCourses(planId, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(activeId.value());
  }

  @Test
  void unknownCallerRoleExcludesArchivedAndInactiveCourses() {
    // Unknown role (e.g. "UNKNOWN", "VIEWER") must behave as unprivileged (not STAFF/ADMIN)
    CourseId activeId = CourseId.generate();
    CourseId archivedId = CourseId.generate();
    CourseId inactiveId = CourseId.generate();

    Course active =
        Course.reconstitute(
            activeId,
            planId,
            programId,
            "ISC-A",
            "Active Course",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.ACTIVE,
            0L);
    Course archived =
        Course.reconstitute(
            archivedId,
            planId,
            programId,
            "ISC-ARCH",
            "Archived Course",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.ARCHIVED,
            1L);
    Course inactive =
        Course.reconstitute(
            inactiveId,
            planId,
            programId,
            "ISC-INACT",
            "Inactive Course",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.INACTIVE,
            1L);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(active, archived, inactive));

    List<CourseView> resultUnknown = service.listPlanCourses(planId, "UNKNOWN");
    List<CourseView> resultViewer = service.listPlanCourses(planId, "VIEWER");

    assertThat(resultUnknown).hasSize(1);
    assertThat(resultUnknown.get(0).id()).isEqualTo(activeId.value());

    assertThat(resultViewer).hasSize(1);
    assertThat(resultViewer.get(0).id()).isEqualTo(activeId.value());
  }

  @Test
  void staffAndAdminStillSeeAllStatusesAfterAllowlistRefactor() {
    // Regression guard: allowlist refactor must not break STAFF/ADMIN visibility
    CourseId activeId = CourseId.generate();
    CourseId archivedId = CourseId.generate();
    CourseId inactiveId = CourseId.generate();

    Course active =
        Course.reconstitute(
            activeId,
            planId,
            programId,
            "ISC-A",
            "Active",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.ACTIVE,
            0L);
    Course archived =
        Course.reconstitute(
            archivedId,
            planId,
            programId,
            "ISC-ARCH",
            "Archived",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.ARCHIVED,
            1L);
    Course inactive =
        Course.reconstitute(
            inactiveId,
            planId,
            programId,
            "ISC-INACT",
            "Inactive",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.INACTIVE,
            1L);

    when(loadCoursePort.loadByPlanId(planId)).thenReturn(List.of(active, archived, inactive));

    assertThat(service.listPlanCourses(planId, "STAFF")).hasSize(3);
    assertThat(service.listPlanCourses(planId, "ADMIN")).hasSize(3);
  }
}
