package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Input port: retrieve a single course by its ID.
 *
 * <p>Backs {@code GET /courses/{courseId}} (CUR-001).
 *
 * <p>No Spring/framework imports (ARQ-003 — all port.in interfaces must be interfaces). Accepts
 * plain {@link UUID} (not a domain VO) so REST adapters do not need to import domain types
 * (ARQ-001).
 */
public interface GetCourseQuery {

  /**
   * Return the course view for the given course ID.
   *
   * @param courseId the course UUID
   * @return the {@link CourseView} DTO
   * @throws co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException if not found
   */
  CourseView getCourse(UUID courseId);

  /**
   * Return a map of course views for the given set of course IDs (batch read, QAL-PERF-002).
   *
   * <p>Additive method — does NOT change the existing {@link #getCourse(UUID)} signature. Course
   * IDs not found in the curriculum are absent from the returned map (no exception thrown).
   *
   * <p>This method stays inside the {@code curriculum::api} {@link
   * org.springframework.modulith.NamedInterface} package so Spring Modulith validates the
   * cross-module edge.
   *
   * @param courseIds the set of course UUIDs to resolve; must not be null
   * @return map from courseId to {@link CourseView} for each found course
   */
  Map<UUID, CourseView> getCourses(Set<UUID> courseIds);
}
