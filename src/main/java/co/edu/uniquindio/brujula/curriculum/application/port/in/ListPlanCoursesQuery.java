package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import java.util.List;
import java.util.UUID;

/**
 * Input port: list all courses for a given study plan.
 *
 * <p>Backs {@code GET /plans/{planId}/courses}. When {@code callerRole} is {@code STUDENT},
 * ARCHIVED courses are hidden from results.
 *
 * <p>No Spring/framework imports.
 */
public interface ListPlanCoursesQuery {

  /**
   * Return the list of courses for a study plan, filtered by visibility for the caller's role.
   *
   * @param planId the study plan UUID
   * @param callerRole the role of the requesting user (e.g. "STUDENT", "STAFF", "ADMIN")
   * @return list of {@link CourseView} DTOs (never null; may be empty)
   */
  List<CourseView> listPlanCourses(UUID planId, String callerRole);
}
