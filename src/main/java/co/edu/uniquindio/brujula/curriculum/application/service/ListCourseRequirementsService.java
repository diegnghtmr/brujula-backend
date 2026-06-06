package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListCourseRequirementsQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.domain.Confidence;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import java.util.List;
import java.util.UUID;

/**
 * Use case implementation: list requirements for a course.
 *
 * <p>Returns all requirement fields including {@code confidence} and {@code inferred} metadata
 * (ADR-F2-004, CROSS-001 schema extension). No filtering by status in Fase 2.
 * No Spring annotations — wired via infrastructure configuration (ARQ-002).
 */
public class ListCourseRequirementsService implements ListCourseRequirementsQuery {

  private final LoadCourseRequirementPort loadCourseRequirementPort;

  public ListCourseRequirementsService(LoadCourseRequirementPort loadCourseRequirementPort) {
    this.loadCourseRequirementPort = loadCourseRequirementPort;
  }

  @Override
  public List<CourseRequirementView> listCourseRequirements(UUID targetCourseId) {
    List<CourseRequirement> requirements = loadCourseRequirementPort.loadByTargetCourseId(targetCourseId);
    return requirements.stream()
        .map(this::toView)
        .toList();
  }

  // ---------------------------------------------------------------------------
  // Domain → DTO mapper
  // ---------------------------------------------------------------------------

  private CourseRequirementView toView(CourseRequirement req) {
    return new CourseRequirementView(
        req.id().value(),
        req.targetCourseId(),
        req.type().name(),
        req.requiredCourseId(),
        req.requiredCredits(),
        req.simultaneityAllowed(),
        confidenceString(req.confidence()),
        req.inferred(),
        req.status().name());
  }

  private String confidenceString(Confidence confidence) {
    return confidence != null ? confidence.name() : null;
  }
}
