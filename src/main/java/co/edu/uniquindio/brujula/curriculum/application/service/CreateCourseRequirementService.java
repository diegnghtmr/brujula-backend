package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseRequirementCommand;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseRequirementUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteAcyclicityService;
import co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteGraph;
import co.edu.uniquindio.brujula.curriculum.domain.RequirementType;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;

import java.util.List;
import java.util.Optional;

/**
 * Use case: create a new CourseRequirement (idempotent via Idempotency-Key, STAFF/ADMIN).
 *
 * <p>Idempotency rules (mirrors CreateOfferingService):
 * <ul>
 *   <li>Same key + same hash → return stored result (replay)
 *   <li>Same key + different hash → throw {@link IdempotencyKeyConflictException} (409)
 *   <li>New key → execute cycle detection, persist, record audit event
 * </ul>
 *
 * <p>Cycle detection: loads all existing COURSE_PASSED/SEQUENCE requirements for the plan,
 * builds the prerequisite graph, adds the proposed edge, and validates acyclicity via
 * {@link PrerequisiteAcyclicityService}. A cycle throws
 * {@link co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteCycleException} (→ 422).
 *
 * <p>Framework-free — wired by {@code CurriculumConfiguration}.
 * Satisfies: CUR-003, IMP-007.
 */
public class CreateCourseRequirementService implements CreateCourseRequirementUseCase {

  public static final String OPERATION = "CREATE_COURSE_REQUIREMENT";

  private final SaveCourseRequirementPort saveRequirementPort;
  private final LoadCoursePort loadCoursePort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;
  private final AuditRecorder auditRecorder;
  private final PrerequisiteAcyclicityService acyclicityService;

  public CreateCourseRequirementService(
      SaveCourseRequirementPort saveRequirementPort,
      LoadCoursePort loadCoursePort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder) {
    this(saveRequirementPort, loadCoursePort, idempotencyStorePort, jsonCodecPort, auditRecorder,
        new PrerequisiteAcyclicityService());
  }

  // Package-private constructor for testing with injected acyclicity service
  CreateCourseRequirementService(
      SaveCourseRequirementPort saveRequirementPort,
      LoadCoursePort loadCoursePort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder,
      PrerequisiteAcyclicityService acyclicityService) {
    this.saveRequirementPort = saveRequirementPort;
    this.loadCoursePort = loadCoursePort;
    this.idempotencyStorePort = idempotencyStorePort;
    this.jsonCodecPort = jsonCodecPort;
    this.auditRecorder = auditRecorder;
    this.acyclicityService = acyclicityService;
  }

  @Override
  public CourseRequirementView createCourseRequirement(CreateCourseRequirementCommand command) {
    // 1. Check idempotency store first
    Optional<IdempotencyStorePort.StoredRecord> existing =
        idempotencyStorePort.find(command.idempotencyKey(), OPERATION);
    if (existing.isPresent()) {
      IdempotencyStorePort.StoredRecord stored = existing.get();
      if (!stored.requestHash().equals(command.requestHash())) {
        throw new IdempotencyKeyConflictException(command.idempotencyKey(), OPERATION);
      }
      return jsonCodecPort.fromJson(stored.responseBodyJson(), CourseRequirementView.class);
    }

    RequirementType type = RequirementType.valueOf(command.type());

    // 2. Cycle detection for edge-creating requirement types (COURSE_PASSED / SEQUENCE)
    if (type == RequirementType.COURSE_PASSED || type == RequirementType.SEQUENCE) {
      if (command.requiredCourseId() != null) {
        List<CourseRequirement> existingRequirements =
            saveRequirementPort.findAllByPlanId(command.planId());
        PrerequisiteGraph graph = buildGraph(existingRequirements);
        // Proposed edge: requiredCourse → targetCourse (requiredCourse must pass before target)
        graph.addEdge(command.requiredCourseId(), command.targetCourseId());
        acyclicityService.validateAcyclicity(graph);
      }
    }

    // 3. Create domain aggregate via typed factory method
    CourseRequirement requirement = switch (type) {
      case COURSE_PASSED -> CourseRequirement.coursePassed(
          CourseRequirementId.generate(),
          command.targetCourseId(),
          command.requiredCourseId(),
          Boolean.TRUE.equals(command.simultaneityAllowed()),
          null, null);
      case SEQUENCE -> CourseRequirement.sequence(
          CourseRequirementId.generate(),
          command.targetCourseId(),
          command.requiredCourseId(),
          Boolean.TRUE.equals(command.simultaneityAllowed()));
      case MINIMUM_CREDITS -> CourseRequirement.minimumCredits(
          CourseRequirementId.generate(),
          command.targetCourseId(),
          command.requiredCredits());
      case CONFIGURABLE -> CourseRequirement.configurable(
          CourseRequirementId.generate(),
          command.targetCourseId());
    };

    // 4. Persist
    CourseRequirement saved = saveRequirementPort.save(requirement);

    // 5. Build view and serialize for idempotency storage
    CourseRequirementView view = toView(saved);
    String responseBodyJson = jsonCodecPort.toJson(view);

    // 6. Record idempotency entry
    idempotencyStorePort.save(
        command.idempotencyKey(), OPERATION, command.requestHash(), 201, responseBodyJson);

    // 7. Record audit event
    auditRecorder.record(new RecordAuditEventCommand(
        command.actorId(),
        "STAFF",
        "COURSE_REQUIREMENT_CREATED",
        "course_requirement",
        saved.id().value(),
        null,
        null,
        null));

    return view;
  }

  private PrerequisiteGraph buildGraph(List<CourseRequirement> requirements) {
    PrerequisiteGraph graph = new PrerequisiteGraph();
    for (CourseRequirement req : requirements) {
      RequirementType reqType = req.type();
      if ((reqType == RequirementType.COURSE_PASSED || reqType == RequirementType.SEQUENCE)
          && req.requiredCourseId() != null) {
        // Edge: requiredCourse → targetCourse
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
