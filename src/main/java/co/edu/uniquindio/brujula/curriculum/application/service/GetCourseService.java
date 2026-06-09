package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.RecommendedSemester;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case implementation: get a single course by ID.
 *
 * <p>Throws {@link CourseNotFoundException} (maps to HTTP 404) if the course is not found. No
 * Spring annotations — wired via infrastructure configuration (ARQ-002).
 */
public class GetCourseService implements GetCourseQuery {

  private final LoadCoursePort loadCoursePort;

  public GetCourseService(LoadCoursePort loadCoursePort) {
    this.loadCoursePort = loadCoursePort;
  }

  @Override
  public CourseView getCourse(UUID courseId) {
    CourseId typedId = new CourseId(courseId);
    Course course =
        loadCoursePort.loadById(typedId).orElseThrow(() -> new CourseNotFoundException(typedId));
    return toView(course);
  }

  /**
   * Batch load courses by their UUIDs in a single query (QAL-PERF-002).
   *
   * <p>Course IDs not found in curriculum are absent from the returned map (no exception).
   */
  @Override
  public Map<UUID, CourseView> getCourses(Set<UUID> courseIds) {
    if (courseIds == null || courseIds.isEmpty()) {
      return Map.of();
    }
    return loadCoursePort.loadByIds(courseIds).stream()
        .collect(Collectors.toMap(course -> course.id().value(), this::toView));
  }

  private CourseView toView(Course course) {
    List<CourseView.RecommendedSemesterView> semesterViews =
        course.recommendedSemesters().stream().map(this::toSemesterView).toList();
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
