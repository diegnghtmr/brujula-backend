package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListPlanCoursesQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.RecommendedSemester;
import java.util.List;
import java.util.UUID;

/**
 * Use case implementation: list courses for a plan.
 *
 * <p>Filters ARCHIVED and INACTIVE courses when the caller is a STUDENT (CUR-001 / CourseStatus
 * Javadoc: INACTIVE is "temporarily disabled; hidden from student views"; ARCHIVED is "soft-deleted;
 * hidden from all non-admin views"). No Spring annotations — wiring is handled in the
 * infrastructure configuration layer (ARQ-002).
 */
public class ListPlanCoursesService implements ListPlanCoursesQuery {

  private final LoadCoursePort loadCoursePort;

  public ListPlanCoursesService(LoadCoursePort loadCoursePort) {
    this.loadCoursePort = loadCoursePort;
  }

  @Override
  public List<CourseView> listPlanCourses(UUID planId, String callerRole) {
    List<Course> courses = loadCoursePort.loadByPlanId(planId);
    return courses.stream()
        .filter(course -> isVisible(course, callerRole))
        .map(this::toView)
        .toList();
  }

  // ---------------------------------------------------------------------------
  // Role-based visibility filter
  // ---------------------------------------------------------------------------

  private boolean isVisible(Course course, String callerRole) {
    if (isPrivilegedRole(callerRole)) {
      return true; // STAFF and ADMIN see all statuses
    }
    // STUDENT, null, or any unknown role: only ACTIVE courses visible (allowlist; W2 fix)
    return course.status() != CourseStatus.ARCHIVED
        && course.status() != CourseStatus.INACTIVE;
  }

  /**
   * Returns {@code true} only for explicitly privileged roles.
   *
   * <p>Allowlist: {@code "STAFF"} and {@code "ADMIN"}. {@code null} and any other value returns
   * {@code false}, defaulting to the most restrictive visibility — consistent with
   * {@code ListProgramPlansService.isPrivilegedRole()} (FIX-4 / W2 allowlist alignment).
   */
  private boolean isPrivilegedRole(String callerRole) {
    return "STAFF".equalsIgnoreCase(callerRole) || "ADMIN".equalsIgnoreCase(callerRole);
  }

  // ---------------------------------------------------------------------------
  // Domain → DTO mapper
  // ---------------------------------------------------------------------------

  private CourseView toView(Course course) {
    List<CourseView.RecommendedSemesterView> semesterViews = course.recommendedSemesters().stream()
        .map(this::toSemesterView)
        .toList();
    return new CourseView(
        course.id().value(),
        course.planId(),
        course.programId(),
        course.officialCode(),
        course.name(),
        course.type().name(),
        course.credits(),
        course.curricularWeeklyHours(),
        semesterViews,
        course.status().name(),
        course.version());
  }

  private CourseView.RecommendedSemesterView toSemesterView(RecommendedSemester rs) {
    return new CourseView.RecommendedSemesterView(rs.journeyCode(), rs.semester());
  }
}
