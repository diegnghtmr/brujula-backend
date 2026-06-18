package co.edu.uniquindio.brujula.curriculum.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseRequirementCommand;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.application.service.CreateCourseRequirementService;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementStatus;
import co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteCycleException;
import co.edu.uniquindio.brujula.curriculum.domain.RequirementType;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TDD RED → GREEN: unit tests for {@link CreateCourseRequirementService}.
 *
 * <p>Covers: happy path saves and records audit; cycle detection rejects with {@link
 * PrerequisiteCycleException}; idempotency replay; idempotency conflict.
 */
class CreateCourseRequirementServiceTest {

  private SaveCourseRequirementPort saveRequirementPort;
  private LoadCoursePort loadCoursePort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AuditRecorder auditRecorder;
  private CreateCourseRequirementService service;

  private final UUID planId = UUID.randomUUID();
  private final UUID targetCourseId = UUID.randomUUID();
  private final UUID requiredCourseId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID idempotencyKey = UUID.randomUUID();
  private final String requestHash = "hash-req";

  @BeforeEach
  void setUp() {
    saveRequirementPort = mock(SaveCourseRequirementPort.class);
    loadCoursePort = mock(LoadCoursePort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    auditRecorder = mock(AuditRecorder.class);
    service =
        new CreateCourseRequirementService(
            saveRequirementPort,
            loadCoursePort,
            idempotencyStorePort,
            jsonCodecPort,
            auditRecorder);
  }

  @Test
  void creates_requirement_and_records_audit() {
    when(idempotencyStorePort.find(idempotencyKey, CreateCourseRequirementService.OPERATION))
        .thenReturn(Optional.empty());
    // No existing requirements — acyclicity trivially satisfied
    when(saveRequirementPort.findAllByPlanId(planId)).thenReturn(Collections.emptyList());
    // No courses needed for semester-ordering when no existing reqs
    when(loadCoursePort.loadByPlanId(planId)).thenReturn(Collections.emptyList());

    CourseRequirement saved =
        CourseRequirement.reconstitute(
            CourseRequirementId.generate(),
            targetCourseId,
            RequirementType.COURSE_PASSED,
            requiredCourseId,
            null,
            false,
            null,
            null,
            CourseRequirementStatus.ACTIVE,
            0L);
    when(saveRequirementPort.save(any(CourseRequirement.class))).thenReturn(saved);
    when(jsonCodecPort.toJson(any())).thenReturn("{\"id\":\"x\"}");

    CreateCourseRequirementCommand command =
        new CreateCourseRequirementCommand(
            idempotencyKey,
            requestHash,
            actorId,
            targetCourseId,
            planId,
            "COURSE_PASSED",
            requiredCourseId,
            null,
            false);

    CourseRequirementView result = service.createCourseRequirement(command);

    assertThat(result.type()).isEqualTo("COURSE_PASSED");
    verify(saveRequirementPort).save(any(CourseRequirement.class));
    verify(auditRecorder).record(argThat(cmd -> "COURSE_REQUIREMENT_CREATED".equals(cmd.action())));
    verify(idempotencyStorePort)
        .save(
            eq(idempotencyKey),
            eq(CreateCourseRequirementService.OPERATION),
            eq(requestHash),
            eq(201),
            any());
  }

  @Test
  void cycle_detected_throws_PrerequisiteCycleException() {
    // Existing: A→B (A is required for B)
    // New: B→A — this would create a cycle
    UUID courseA = requiredCourseId;
    UUID courseB = targetCourseId;

    when(idempotencyStorePort.find(idempotencyKey, CreateCourseRequirementService.OPERATION))
        .thenReturn(Optional.empty());

    // Existing requirement: A is required for B
    CourseRequirement existingReq =
        CourseRequirement.reconstitute(
            CourseRequirementId.generate(),
            courseB,
            RequirementType.COURSE_PASSED,
            courseA,
            null,
            false,
            null,
            null,
            CourseRequirementStatus.ACTIVE,
            0L);
    when(saveRequirementPort.findAllByPlanId(planId)).thenReturn(List.of(existingReq));
    when(loadCoursePort.loadByPlanId(planId)).thenReturn(Collections.emptyList());

    // New requirement: B is required for A — creates cycle A→B→A
    CreateCourseRequirementCommand command =
        new CreateCourseRequirementCommand(
            idempotencyKey,
            requestHash,
            actorId,
            courseA,
            planId,
            "COURSE_PASSED",
            courseB,
            null,
            false);

    assertThatThrownBy(() -> service.createCourseRequirement(command))
        .isInstanceOf(PrerequisiteCycleException.class);

    verify(saveRequirementPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void idempotency_replay_returns_stored_view() {
    CourseRequirementView storedView =
        new CourseRequirementView(
            UUID.randomUUID(),
            targetCourseId,
            "COURSE_PASSED",
            requiredCourseId,
            null,
            false,
            null,
            null,
            "ACTIVE",
            0L);
    String storedJson = "{\"id\":\"x\"}";

    when(idempotencyStorePort.find(idempotencyKey, CreateCourseRequirementService.OPERATION))
        .thenReturn(
            Optional.of(new IdempotencyStorePort.StoredRecord(requestHash, 201, storedJson)));
    when(jsonCodecPort.fromJson(storedJson, CourseRequirementView.class)).thenReturn(storedView);

    CreateCourseRequirementCommand command =
        new CreateCourseRequirementCommand(
            idempotencyKey,
            requestHash,
            actorId,
            targetCourseId,
            planId,
            "COURSE_PASSED",
            requiredCourseId,
            null,
            false);

    CourseRequirementView result = service.createCourseRequirement(command);

    assertThat(result).isEqualTo(storedView);
    verify(saveRequirementPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void idempotency_conflict_throws() {
    when(idempotencyStorePort.find(idempotencyKey, CreateCourseRequirementService.OPERATION))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredRecord("other-hash", 201, "{}")));

    CreateCourseRequirementCommand command =
        new CreateCourseRequirementCommand(
            idempotencyKey,
            requestHash,
            actorId,
            targetCourseId,
            planId,
            "COURSE_PASSED",
            requiredCourseId,
            null,
            false);

    assertThatThrownBy(() -> service.createCourseRequirement(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);

    verify(saveRequirementPort, never()).save(any());
  }
}
