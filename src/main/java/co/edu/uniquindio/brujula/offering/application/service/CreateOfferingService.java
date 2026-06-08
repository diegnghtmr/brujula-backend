package co.edu.uniquindio.brujula.offering.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.offering.application.dto.CreateOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleSessionView;
import co.edu.uniquindio.brujula.offering.application.port.in.CreateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.out.SaveOfferingPort;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOptionId;
import co.edu.uniquindio.brujula.offering.domain.ScheduleSession;
import co.edu.uniquindio.brujula.offering.domain.ScheduleSessionId;
import co.edu.uniquindio.brujula.offering.domain.Weekday;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case: create a course offering (idempotent via Idempotency-Key, STAFF/ADMIN).
 *
 * <p>Idempotency rules (mirrors CreateStaffService / CreatePeriodService verbatim):
 * <ul>
 *   <li>Same key + same hash → return the stored result (replay, no new insert)
 *   <li>Same key + different hash → throw {@link IdempotencyKeyConflictException} (409)
 *   <li>New key → execute, persist idempotency record, record audit event
 * </ul>
 *
 * <p>Inline schedule options are mapped to domain objects and persisted atomically
 * in the same transaction via CascadeType.ALL on the offering aggregate.
 *
 * <p>{@code courseName} is denormalized at create time from {@code curriculum::api GetCourseQuery}.
 *
 * <p>Framework-free — wired by {@code OfferingConfiguration}.
 * Satisfies: OFFW-003.
 */
public class CreateOfferingService implements CreateOfferingUseCase {

  static final String OPERATION = "CREATE_OFFERING";

  private final SaveOfferingPort saveOfferingPort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;
  private final AuditRecorder auditRecorder;
  private final GetCourseQuery getCourseQuery;

  public CreateOfferingService(
      SaveOfferingPort saveOfferingPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder,
      GetCourseQuery getCourseQuery) {
    this.saveOfferingPort = saveOfferingPort;
    this.idempotencyStorePort = idempotencyStorePort;
    this.jsonCodecPort = jsonCodecPort;
    this.auditRecorder = auditRecorder;
    this.getCourseQuery = getCourseQuery;
  }

  @Override
  public OfferingDetailView createOffering(CreateOfferingCommand command) {
    // 1. Check idempotency store first
    Optional<IdempotencyStorePort.StoredRecord> existing =
        idempotencyStorePort.find(command.idempotencyKey(), OPERATION);
    if (existing.isPresent()) {
      IdempotencyStorePort.StoredRecord stored = existing.get();
      if (!stored.requestHash().equals(command.requestHash())) {
        throw new IdempotencyKeyConflictException(command.idempotencyKey(), OPERATION);
      }
      return jsonCodecPort.fromJson(stored.responseBodyJson(), OfferingDetailView.class);
    }

    // 2. Denormalize course name from curriculum (avoids runtime coupling at query time)
    CourseView courseView = getCourseQuery.getCourse(command.courseId());
    String courseName = courseView.name();

    // 3. Map inline schedule option drafts → domain objects with fresh UUIDs
    List<ScheduleOption> options = mapDraftOptions(command.scheduleOptions(), command.courseId());

    // 4. Create domain aggregate (starts DRAFT, version 0)
    CourseOffering offering = CourseOffering.create(
        CourseOfferingId.generate(),
        command.courseId(),
        courseName,
        command.programId(),
        command.periodId(),
        command.notes(),
        options);

    // 5. Persist (cascade saves options + sessions in same tx)
    CourseOffering saved = saveOfferingPort.save(offering);

    // 6. Build view and serialize for idempotency storage
    OfferingDetailView view = toDetailView(saved);
    String responseBodyJson = jsonCodecPort.toJson(view);

    // 7. Record idempotency entry
    idempotencyStorePort.save(
        command.idempotencyKey(), OPERATION, command.requestHash(), 201, responseBodyJson);

    // 8. Record audit event (RNF-SEG-007 synchronous, same transaction)
    auditRecorder.record(new RecordAuditEventCommand(
        command.actorId(),
        "STAFF",
        "OFFERING_CREATED",
        "course_offering",
        saved.getId().value(),
        null,
        null,
        null));

    return view;
  }

  private List<ScheduleOption> mapDraftOptions(
      List<CreateOfferingCommand.ScheduleOptionDraft> drafts, UUID parentOfferingId) {
    if (drafts == null || drafts.isEmpty()) {
      return Collections.emptyList();
    }
    return drafts.stream()
        .map(draft -> {
          ScheduleOptionId optionId = ScheduleOptionId.generate();
          List<ScheduleSession> sessions = mapDraftSessions(draft.sessions());
          return ScheduleOption.reconstitute(
              optionId, parentOfferingId, draft.label(), draft.journeyCode(), sessions, 0L);
        })
        .toList();
  }

  private List<ScheduleSession> mapDraftSessions(
      List<CreateOfferingCommand.SessionDraft> drafts) {
    if (drafts == null || drafts.isEmpty()) {
      return Collections.emptyList();
    }
    return drafts.stream()
        .map(draft -> new ScheduleSession(
            UUID.randomUUID(),
            Weekday.valueOf(draft.day()),
            draft.startsAt(),
            draft.endsAt()))
        .toList();
  }

  OfferingDetailView toDetailView(CourseOffering offering) {
    List<ScheduleOptionView> optionViews = offering.getScheduleOptions().stream()
        .map(option -> toOptionView(option, offering.getId().value()))
        .toList();
    return new OfferingDetailView(
        offering.getId().value(),
        offering.getCourseId(),
        offering.getCourseName(),
        offering.getProgramId(),
        offering.getPeriodId(),
        offering.getStatus().name(),
        offering.getNotes(),
        offering.getVersion(),
        optionViews);
  }

  private ScheduleOptionView toOptionView(ScheduleOption option, UUID offeringId) {
    List<ScheduleSessionView> sessionViews = option.getSessions().stream()
        .map(s -> new ScheduleSessionView(s.id(), s.day().name(), s.startsAt(), s.endsAt()))
        .toList();
    return new ScheduleOptionView(
        option.getId().value(),
        offeringId,
        option.getLabel(),
        option.getJourneyCode(),
        option.weeklyHours(),
        option.getVersion(),
        sessionViews);
  }
}
