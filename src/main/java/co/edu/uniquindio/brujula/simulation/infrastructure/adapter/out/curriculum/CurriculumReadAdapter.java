package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.curriculum;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListCourseRequirementsQuery;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.domain.PrereqRequirement;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Anti-corruption adapter: bridges simulation's {@link CurriculumReadPort} to
 * curriculum's published {@code curriculum::api} named interface (INV-001).
 *
 * <p>Maps {@code CourseRequirementView} to sim-local {@link PrereqRequirement} records
 * at this boundary (ARQ-002). The simulation domain never sees curriculum types.
 */
@Component
class CurriculumReadAdapter implements CurriculumReadPort {

  private final ListCourseRequirementsQuery listCourseRequirementsQuery;
  private final GetCourseQuery getCourseQuery;

  CurriculumReadAdapter(
      ListCourseRequirementsQuery listCourseRequirementsQuery,
      GetCourseQuery getCourseQuery) {
    this.listCourseRequirementsQuery = listCourseRequirementsQuery;
    this.getCourseQuery = getCourseQuery;
  }

  @Override
  public List<PrereqRequirement> requirementsForCourse(UUID courseId) {
    List<CourseRequirementView> views = listCourseRequirementsQuery.listCourseRequirements(courseId);
    return views.stream()
        .filter(v -> "ACTIVE".equals(v.status()))
        .map(v -> new PrereqRequirement(
            v.targetCourseId(),
            v.requiredCourseId(),
            v.type(),
            v.confidence(),
            v.inferred(),
            resolveCourseName(v.requiredCourseId())))
        .toList();
  }

  @Override
  public int courseCredits(UUID courseId) {
    CourseView course = getCourseQuery.getCourse(courseId);
    return course.credits();
  }

  private String resolveCourseName(UUID courseId) {
    if (courseId == null) {
      return null;
    }
    try {
      CourseView course = getCourseQuery.getCourse(courseId);
      return course != null ? course.name() : courseId.toString();
    } catch (Exception e) {
      // If course name resolution fails, fall back to UUID string
      return courseId.toString();
    }
  }
}
