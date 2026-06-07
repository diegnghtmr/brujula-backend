package co.edu.uniquindio.brujula.progress.application.dto;

import java.util.List;
import java.util.Objects;

/**
 * Application-layer DTO: a semester bucket in the curriculum view.
 *
 * <p>No domain type imports (ARQ-001).
 *
 * @param recommendedSemester semester number (1-based)
 * @param courses the courses in this semester bucket
 */
public record CurriculumSemesterData(int recommendedSemester, List<CurriculumCourseData> courses) {

  public CurriculumSemesterData {
    Objects.requireNonNull(courses, "courses must not be null");
  }
}
