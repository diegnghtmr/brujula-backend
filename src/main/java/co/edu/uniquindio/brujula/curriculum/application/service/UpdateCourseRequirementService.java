package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.UpdateCourseRequirementCommand;
import co.edu.uniquindio.brujula.curriculum.application.port.in.UpdateCourseRequirementUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteAcyclicityService;
import co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteGraph;
import co.edu.uniquindio.brujula.curriculum.domain.RequirementType;

import java.util.List;
import java.util.UUID;

/**
 * Use case: update an existing CourseRequirement with optimistic locking (STAFF/ADMIN, NOT idempotent).
 *
 * <p>Follows the UpdateCourseService pattern:
 * <ol>
 *   <li>Load current requirement (throws {@link CourseRequirementNotFoundException} if absent)
 *   <li>For COURSE_PASSED/SEQUENCE types, perform cycle detection against the real persisted graph
 *       with the OLD edge excluded and the NEW proposed edge included (mirrors CreateCourseRequirementService)
 *   <li>Reconstitute with updated fields and {@code expectedVersion} for optimistic lock enforcement
 *   <li>Persist — may throw {@code OptimisticLockingFailureException} (→ 409)
 *   <li>Record audit event
 * </ol>
 *
 * <p>Framework-free — wired by {@code CurriculumConfiguration}.
 * Satisfies: CUR-003, IMP-007.
 */
public class UpdateCourseRequirementService implements UpdateCourseRequirementUseCase {

  private final SaveCourseRequirementPort saveRequirementPort;
  private final LoadCoursePort loadCoursePort;
  private final AuditRecorder auditRecorder;
  private final PrerequisiteAcyclicityService acyclicityService;

  public UpdateCourseRequirementService(
      SaveCourseRequirementPort saveRequirementPort,
      LoadCoursePort loadCoursePort,
      AuditRecorder auditRecorder) {
    this(saveRequirementPort, loadCoursePort, auditRecorder, new PrerequisiteAcyclicityService());
  }

  // Package-private constructor for testing with injected acyclicity service
  UpdateCourseRequirementService(
      SaveCourseRequirementPort saveRequirementPort,
      LoadCoursePort loadCoursePort,
      AuditRecorder auditRecorder,
      PrerequisiteAcyclicityService acyclicityService) {
    this.saveRequirementPort = saveRequirementPort;
    this.loadCoursePort = loadCoursePort;
    this.auditRecorder = auditRecorder;
    this.acyclicityService = acyclicityService;
  }

  @Override
  public CourseRequirementView updateCourseRequirement(UpdateCourseRequirementCommand command) {
    CourseRequirementId id = new CourseRequirementId(command.requirementId());

    // 1. Load current requirement
    CourseRequirement existing = saveRequirementPort.findCurrentById(id)
        .orElseThrow(() -> new CourseRequirementNotFoundException(id));

    // 2. Resolve updated fields (fall back to existing if command field is null)
    RequirementType newType = command.type() != null
        ? RequirementType.valueOf(command.type())
        : existing.type();
    UUID newRequiredCourseId = command.requiredCourseId() != null
        ? command.requiredCourseId()
        : existing.requiredCourseId();
    Integer newRequiredCredits = command.requiredCredits() != null
        ? command.requiredCredits()
        : existing.requiredCredits();
    Boolean newSimultaneityAllowed = command.simultaneityAllowed() != null
        ? command.simultaneityAllowed()
        : existing.simultaneityAllowed();

    // 3. Cycle detection for edge-creating requirement types (COURSE_PASSED / SEQUENCE).
    //    Build the graph from the real persisted plan requirements, but EXCLUDE this requirement's
    //    old edge (so we don't false-reject a no-op or a legitimate change), then add the NEW
    //    proposed edge before validating acyclicity.
    if ((newType == RequirementType.COURSE_PASSED || newType == RequirementType.SEQUENCE)
        && newRequiredCourseId != null) {
      UUID planId = resolvePlanId(existing.targetCourseId());
      List<CourseRequirement> allPlanRequirements = saveRequirementPort.findAllByPlanId(planId);
      PrerequisiteGraph graph = buildGraphExcluding(allPlanRequirements, existing.id());
      // Add proposed edge: newRequiredCourse → targetCourse
      graph.addEdge(newRequiredCourseId, existing.targetCourseId());
      acyclicityService.validateAcyclicity(graph);
    }

    // 4. Reconstitute with updated fields and expectedVersion for optimistic lock enforcement
    CourseRequirement withExpectedVersion = CourseRequirement.reconstitute(
        existing.id(),
        existing.targetCourseId(),
        newType,
        newRequiredCourseId,
        newRequiredCredits,
        newSimultaneityAllowed,
        existing.confidence(),
        existing.inferred(),
        existing.status(),
        command.expectedVersion());

    // 5. Persist — may throw OptimisticLockingFailureException (→ 409)
    CourseRequirement saved = saveRequirementPort.save(withExpectedVersion);

    // 6. Record audit event
    auditRecorder.record(new RecordAuditEventCommand(
        command.actorId(),
        "STAFF",
        "COURSE_REQUIREMENT_UPDATED",
        "course_requirement",
        saved.id().value(),
        null,
        null,
        null));

    return toView(saved);
  }

  private UUID resolvePlanId(UUID targetCourseId) {
    Course course = loadCoursePort.loadById(new CourseId(targetCourseId))
        .orElseThrow(() -> new IllegalStateException(
            "Target course not found for requirement cycle check: " + targetCourseId));
    return course.planId();
  }

  private PrerequisiteGraph buildGraphExcluding(
      List<CourseRequirement> requirements, CourseRequirementId excludedId) {
    PrerequisiteGraph graph = new PrerequisiteGraph();
    for (CourseRequirement req : requirements) {
      // Skip the requirement being updated (its old edge must not be in the graph)
      if (req.id().equals(excludedId)) {
        continue;
      }
      RequirementType reqType = req.type();
      if ((reqType == RequirementType.COURSE_PASSED || reqType == RequirementType.SEQUENCE)
          && req.requiredCourseId() != null) {
        graph.addEdge(req.requiredCourseId(), req.targetCourseId());
      }
    }
    return graph;
  }

  private static CourseRequirementView toView(CourseRequirement req) {
    return new CourseRequirementView(
        req.id().value(),
        req.targetCourseId(),
        req.type().name(),
        req.requiredCourseId(),
        req.requiredCredits(),
        req.simultaneityAllowed(),
        req.confidence() != null ? req.confidence().name() : null,
        req.inferred(),
        req.status().name(),
        req.version());
  }
}
