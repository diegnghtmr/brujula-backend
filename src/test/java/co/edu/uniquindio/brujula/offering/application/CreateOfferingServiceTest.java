package co.edu.uniquindio.brujula.offering.application;

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
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.offering.application.dto.CreateOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.port.out.SaveOfferingPort;
import co.edu.uniquindio.brujula.offering.application.service.CreateOfferingService;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.OfferingStatus;
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
 * TDD unit tests for {@link CreateOfferingService}.
 *
 * <p>Covers: new key creates + cascades options + records audit; idempotency replay returns stored
 * view; same-key+different-hash throws {@link IdempotencyKeyConflictException}; version in
 * returned view is 0. Zero Spring.
 */
class CreateOfferingServiceTest {

  private SaveOfferingPort saveOfferingPort;
  private IdempotencyStorePort idempotencyStorePort;
  private JsonCodecPort jsonCodecPort;
  private AuditRecorder auditRecorder;
  private GetCourseQuery getCourseQuery;
  private CreateOfferingService service;

  private final UUID courseId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final String courseName = "Programación I";

  @BeforeEach
  void setUp() {
    saveOfferingPort = mock(SaveOfferingPort.class);
    idempotencyStorePort = mock(IdempotencyStorePort.class);
    jsonCodecPort = mock(JsonCodecPort.class);
    auditRecorder = mock(AuditRecorder.class);
    getCourseQuery = mock(GetCourseQuery.class);
    service = new CreateOfferingService(
        saveOfferingPort, idempotencyStorePort, jsonCodecPort, auditRecorder, getCourseQuery);
  }

  @Test
  void new_key_creates_offering_saves_and_records_audit() {
    UUID idempotencyKey = UUID.randomUUID();
    String requestHash = "hash-create-offering";

    when(idempotencyStorePort.find(idempotencyKey, "CREATE_OFFERING")).thenReturn(Optional.empty());

    CourseView courseView = new CourseView(
        courseId, UUID.randomUUID(), programId, "ISC-PROG1", courseName,
        "REQUIRED", 3, 6, Collections.emptyList(), "ACTIVE", 0L);
    when(getCourseQuery.getCourse(courseId)).thenReturn(courseView);

    CourseOffering saved = CourseOffering.reconstitute(
        CourseOfferingId.generate(), courseId, courseName, programId, periodId,
        OfferingStatus.DRAFT, null, Collections.emptyList(), 0L);
    when(saveOfferingPort.save(any())).thenReturn(saved);
    when(jsonCodecPort.toJson(any())).thenReturn("{\"id\":\"...\"}");

    CreateOfferingCommand command = new CreateOfferingCommand(
        idempotencyKey, actorId, programId, courseId, periodId,
        null, Collections.emptyList(), requestHash);

    OfferingDetailView result = service.createOffering(command);

    assertThat(result).isNotNull();
    assertThat(result.status()).isEqualTo("DRAFT");
    assertThat(result.version()).isEqualTo(0L);
    verify(saveOfferingPort).save(any());
    verify(auditRecorder).record(argThat(cmd -> "OFFERING_CREATED".equals(cmd.action())));
    verify(jsonCodecPort).toJson(any(OfferingDetailView.class));
    verify(idempotencyStorePort).save(
        eq(idempotencyKey), eq("CREATE_OFFERING"), eq(requestHash), eq(201), any(String.class));
  }

  @Test
  void new_key_cascades_inline_schedule_options() {
    UUID idempotencyKey = UUID.randomUUID();
    when(idempotencyStorePort.find(idempotencyKey, "CREATE_OFFERING")).thenReturn(Optional.empty());

    CourseView courseView = new CourseView(
        courseId, UUID.randomUUID(), programId, null, courseName,
        "REQUIRED", 3, 6, Collections.emptyList(), "ACTIVE", 0L);
    when(getCourseQuery.getCourse(courseId)).thenReturn(courseView);

    // Simulate saved offering with 1 schedule option
    co.edu.uniquindio.brujula.offering.domain.ScheduleOption option =
        co.edu.uniquindio.brujula.offering.domain.ScheduleOption.reconstitute(
            co.edu.uniquindio.brujula.offering.domain.ScheduleOptionId.generate(),
            UUID.randomUUID(), "Grupo A", "DIURNA", Collections.emptyList(), 0L);
    CourseOffering saved = CourseOffering.reconstitute(
        CourseOfferingId.generate(), courseId, courseName, programId, periodId,
        OfferingStatus.DRAFT, null, List.of(option), 0L);
    when(saveOfferingPort.save(any())).thenReturn(saved);
    when(jsonCodecPort.toJson(any())).thenReturn("{\"id\":\"...\"}");

    CreateOfferingCommand.SessionDraft sessionDraft = new CreateOfferingCommand.SessionDraft(
        "MONDAY", java.time.LocalTime.of(8, 0), java.time.LocalTime.of(10, 0));
    CreateOfferingCommand.ScheduleOptionDraft optionDraft =
        new CreateOfferingCommand.ScheduleOptionDraft("Grupo A", "DIURNA", List.of(sessionDraft));
    CreateOfferingCommand command = new CreateOfferingCommand(
        idempotencyKey, actorId, programId, courseId, periodId,
        null, List.of(optionDraft), "hash-xyz");

    OfferingDetailView result = service.createOffering(command);

    assertThat(result.scheduleOptions()).hasSize(1);
    // saveOfferingPort.save was called with a CourseOffering that has schedule options
    verify(saveOfferingPort).save(argThat(o -> !o.getScheduleOptions().isEmpty()));
  }

  @Test
  void same_key_same_hash_replays_stored_view() {
    UUID idempotencyKey = UUID.randomUUID();
    String requestHash = "hash-offering";
    UUID offeringId = UUID.randomUUID();
    String storedJson = "{\"id\":\"" + offeringId + "\"}";
    OfferingDetailView replayView = new OfferingDetailView(
        offeringId, courseId, courseName, programId, periodId,
        "DRAFT", null, 0L, Collections.emptyList());

    when(idempotencyStorePort.find(idempotencyKey, "CREATE_OFFERING"))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredRecord(requestHash, 201, storedJson)));
    when(jsonCodecPort.fromJson(storedJson, OfferingDetailView.class)).thenReturn(replayView);

    CreateOfferingCommand command = new CreateOfferingCommand(
        idempotencyKey, actorId, programId, courseId, periodId,
        null, Collections.emptyList(), requestHash);

    OfferingDetailView result = service.createOffering(command);

    assertThat(result.id()).isEqualTo(offeringId);
    assertThat(result.status()).isEqualTo("DRAFT");
    verify(saveOfferingPort, never()).save(any());
    verify(auditRecorder, never()).record(any());
  }

  @Test
  void same_key_different_hash_throws_conflict() {
    UUID idempotencyKey = UUID.randomUUID();
    String storedJson = "{\"id\":\"00000000-0000-0000-0000-000000000001\"}";

    when(idempotencyStorePort.find(idempotencyKey, "CREATE_OFFERING"))
        .thenReturn(Optional.of(new IdempotencyStorePort.StoredRecord("stored-hash", 201, storedJson)));

    CreateOfferingCommand command = new CreateOfferingCommand(
        idempotencyKey, actorId, programId, courseId, periodId,
        null, Collections.emptyList(), "different-hash");

    assertThatThrownBy(() -> service.createOffering(command))
        .isInstanceOf(IdempotencyKeyConflictException.class);
    verify(saveOfferingPort, never()).save(any());
  }
}
