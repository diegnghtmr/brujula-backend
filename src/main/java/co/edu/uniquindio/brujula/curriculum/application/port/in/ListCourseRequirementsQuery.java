package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import java.util.List;
import java.util.UUID;

/**
 * Input port: list all requirements for a given course.
 *
 * <p>Backs {@code GET /courses/{courseId}/requirements} (CUR-002). Returns all requirement types
 * including confidence and inferred fields (ADR-F2-004, CROSS-001 schema extension).
 *
 * <p>No Spring/framework imports (ARQ-003 — all port.in interfaces must be interfaces).
 */
public interface ListCourseRequirementsQuery {

  /**
   * Return the list of requirements for a course.
   *
   * @param targetCourseId the UUID of the course whose requirements are requested
   * @return list of {@link CourseRequirementView} DTOs (never null; may be empty)
   */
  List<CourseRequirementView> listCourseRequirements(UUID targetCourseId);
}
