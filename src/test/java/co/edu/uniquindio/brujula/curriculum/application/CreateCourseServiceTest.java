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
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CreateCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.service.CreateCourseService;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
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
 * TDD RED → GREEN: unit tests for {@link CreateCourseService}.
 *
 * <p>Covers: happy path (creates, saves, records audit), idempotency replay, key conflict.
 */
class CreateCourseServiceTest {

  private SaveCoursePort saveCoursePort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AuditRecorder auditRecorder;
  private CreateCourseService service;

  private final UUID planId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID idempotencyKey = UUID.randomUUID();
  private final String requestHash = "hash-abc";

  @BeforeEach
  void setUp() {
    saveCoursePort = mock(SaveCoursePort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    auditRecorder = mock(AuditRecorder.class);
    service =
        new CreateCourseService(saveCoursePort, idempotencyStorePort, jsonCodecPort, auditRecorder);
  }

  @Test
  void creates_course_saves_and_records_audit() {
    when(idempotencyStorePort.find(idempotencyKey, CreateCourseService.OPERATION))
        .thenReturn(Optional.empty());

    Course savedCourse =
        Course.reconstitute(
            CourseId.generate(),
            planId,
            programId,
            null,
            "Cálculo I",
            CourseType.REQUIRED,
            4,
            4,
            Collections.emptyList(),
            CourseStatus.ACTIVE,
            0L);
    when(saveCoursePort.save(any(Course.class))).thenReturn(savedCourse);

    CourseView expectedView = toView(savedCourse);
    when(jsonCodecPort.toJson(any())).thenReturn("{\"id\":\"test\"}");

    CreateCourseCommand command =
        new CreateCourseCommand(
            idempotencyKey,
            requestHash,
            actorId,
            planId,
            programId,
            "Cálculo I",
            null,
            "REQUIRED",
            4,
            4,
            List.of());

    CourseView result = service.createCourse(command);

    assertThat(result.name()).isEqualTo("Cálculo I");
    assertThat(result.status()).isEqualTo("ACTIVE");
    verify(saveCoursePort).save(any(Course.class));
    verify(auditRecorder).record(argThat(cmd -> "COURSE_CREATED".equals(cmd.action())));
    verify(idempotencyStorePort)
        .save(
            eq(idempotencyKey), eq(CreateCourseService.OPERATION), eq(requestHash), eq(201), any());
  }

  @Test
  void idempotency_replay_returns_stored_view() {
    CourseView storedView =
        new CourseView(
            UUID.randomUUID(),
            planId,
            programId,
            null,
            "Cálculo I",
            "REQUIRED",
            4,
            4,
            List.of(),
            "ACTIVE",
            0L);
    String storedJson = "{\"id\":\"some-id\"}";

    when(idempotencyStorePort.find(idempotencyKey, CreateCourseService.OPERATION))
        .thenReturn(
            Optional.of(new IdempotencyStorePort.StoredRecord(requestHash, 201, storedJson)));
    when(jsonCodecPort.fromJson(storedJson, CourseView.class)).thenReturn(storedView);

    CreateCourseCommand command =
        new CreateCourseCommand(
            idempotencyKey,
            requestHash,
            actorId,
            planId,
            programId,
            "Cálculo I",
            null,
            "REQUIRED",
            4,
            4,
            List.of());

    CourseView result = service.createCourse(command);

    assertThat(result).isEqualTo(storedView);
    verify(saveCoursePort, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void idempotency_conflict_throws() {
    when(idempotencyStorePort.find(idempotencyKey, CreateCourseService.OPERATION))
        .thenReturn(
            Optional.of(new IdempotencyStorePort.StoredRecord("different-hash", 201, "{}")));

    CreateCourseCommand command =
        new CreateCourseCommand(
            idempotencyKey,
            requestHash,
            actorId,
            planId,
            programId,
            "Cálculo I",
            null,
            "REQUIRED",
            4,
            4,
            List.of());

    assertThatThrownBy(() -> service.createCourse(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);

    verify(saveCoursePort, never()).save(any());
  }

  @Test
  void unknown_initial_status_throws_with_clear_message() {
    // S1 guard: invalid status string must produce a clear message, not a raw enum error
    when(idempotencyStorePort.find(idempotencyKey, CreateCourseService.OPERATION))
        .thenReturn(Optional.empty());

    CreateCourseCommand command =
        new CreateCourseCommand(
            idempotencyKey,
            requestHash,
            actorId,
            planId,
            programId,
            "Cálculo I",
            null,
            "REQUIRED",
            4,
            4,
            List.of(),
            "DRAFT");

    assertThatThrownBy(() -> service.createCourse(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown course status: DRAFT")
        .hasMessageContaining("expected one of");

    verify(saveCoursePort, never()).save(any());
  }

  private static CourseView toView(Course c) {
    return new CourseView(
        c.id().value(),
        c.planId(),
        c.programId(),
        c.officialCode(),
        c.name(),
        c.type().name(),
        c.credits(),
        c.curricularWeeklyHours(),
        List.of(),
        c.status().name(),
        c.version());
  }
}
