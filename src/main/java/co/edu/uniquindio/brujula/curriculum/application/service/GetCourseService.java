package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.RecommendedSemester;
import java.util.List;
import java.util.UUID;

/**
 * Use case implementation: get a single course by ID.
 *
 * <p>Throws {@link CourseNotFoundException} (maps to HTTP 404) if the course is not found.
 * No Spring annotations — wired via infrastructure configuration (ARQ-002).
 */
public class GetCourseService implements GetCourseQuery {

  private final LoadCoursePort loadCoursePort;

  public GetCourseService(LoadCoursePort loadCoursePort) {
    this.loadCoursePort = loadCoursePort;
  }

  @Override
  public CourseView getCourse(UUID courseId) {
    CourseId typedId = new CourseId(courseId);
    Course course = loadCoursePort.loadById(typedId)
        .orElseThrow(() -> new CourseNotFoundException(typedId));
    return toView(course);
  }

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
        course.status().name());
  }

  private CourseView.RecommendedSemesterView toSemesterView(RecommendedSemester rs) {
    return new CourseView.RecommendedSemesterView(rs.journeyCode(), rs.semester());
  }
}
