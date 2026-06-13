package co.edu.uniquindio.brujula.offering.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.PublishOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleSessionView;
import co.edu.uniquindio.brujula.offering.application.port.in.PublishOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.out.SaveOfferingPort;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.OfferingStatus;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case: publish (state-transition) a course offering (idempotent, STAFF/ADMIN).
 *
 * <p>Idempotency rules (mirrors CreateOfferingService / CreatePeriodService verbatim):
 *
 * <ul>
 *   <li>Same key + same hash → return the stored result (replay, no re-execution)
 *   <li>Same key + different hash → throw {@link IdempotencyKeyConflictException} (409)
 *   <li>New key → execute, persist idempotency record, record audit event
 * </ul>
 *
 * <p>Intensity advisory:
 *
 * <ul>
 *   <li>For each schedule option, compare {@code option.weeklyHours()} against {@code
 *       GetCourseQuery.getCourse(courseId).curricularWeeklyHours()}.
 *   <li>If ANY option's hours differ → set {@code intensityWarning=true} on the response.
 *   <li>This is ADVISORY: it NEVER returns 422 or blocks publication. Always returns 200 + flag.
 *   <li>This is NOT a no-op: the comparison is performed against the REAL curricular data from
 *       {@code curriculum::api}. Any shortcut that always sets the flag to false/null would be a
 *       regression.
 * </ul>
 *
 * <p>{@code course_offering.version} is incremented at flush time by the JPA @Version mechanism.
 * The returned view's version MUST be strictly greater than {@code command.expectedVersion()} —
 * this is the staleness activation invariant.
 *
 * <p>Framework-free — wired by {@code OfferingConfiguration}.
 */
public class PublishOfferingService implements PublishOfferingUseCase {

  static final String OPERATION = "PUBLISH_OFFERING";

  private final SaveOfferingPort saveOfferingPort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;
  private final AuditRecorder auditRecorder;
  private final GetCourseQuery getCourseQuery;

  public PublishOfferingService(
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
  public OfferingDetailView publishOffering(PublishOfferingCommand command) {
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

    // 2. Load the offering (throws OfferingNotFoundException if absent)
    CourseOfferingId id = CourseOfferingId.of(command.offeringId());
    CourseOffering existing1 =
        saveOfferingPort.findCurrentById(id).orElseThrow(() -> new OfferingNotFoundException(id));

    // 3. Parse target status and apply domain transition (domain guards throw on violation)
    OfferingStatus targetStatus = OfferingStatus.valueOf(command.targetStatus());
    CourseOffering transitioned = existing1.publishTo(targetStatus);

    // 4. Set expectedVersion for optimistic-lock detection then persist
    CourseOffering withExpectedVersion =
        CourseOffering.reconstitute(
            transitioned.getId(),
            transitioned.getCourseId(),
            transitioned.getCourseName(),
            transitioned.getProgramId(),
            transitioned.getPeriodId(),
            transitioned.getStatus(),
            transitioned.getNotes(),
            transitioned.getScheduleOptions(),
            command.expectedVersion());

    // 5. Persist — may throw OptimisticLockingFailureException (→ 409 at handler)
    //    Returns managed post-flush domain object with @Version incremented.
    CourseOffering saved = saveOfferingPort.save(withExpectedVersion);

    // 6. ADVISORY intensity check (NEVER blocks)
    boolean intensityWarning = computeIntensityWarning(saved);

    // 7. Build the view
    OfferingDetailView view = toDetailView(saved, intensityWarning);

    // 8. Serialize and record idempotency entry
    String responseBodyJson = jsonCodecPort.toJson(view);
    idempotencyStorePort.save(
        command.idempotencyKey(), OPERATION, command.requestHash(), 200, responseBodyJson);

    // 9. Record audit event (synchronous, same transaction)
    auditRecorder.record(
        new RecordAuditEventCommand(
            command.actorId(),
            "STAFF",
            "OFFERING_PUBLISHED",
            "course_offering",
            saved.getId().value(),
            null,
            null,
            null));

    return view;
  }

  /**
   * Advisory intensity check.
   *
   * <p>Queries {@code curriculum::api} for the course's curricular weekly hours and compares each
   * schedule option's computed weekly hours. Returns {@code true} if ANY option's hours differ from
   * the curricular hours. This comparison is REAL — it is NOT a no-op.
   *
   * <p>Intensity mismatch is advisory only: it NEVER throws, never returns 422.
   */
  private boolean computeIntensityWarning(CourseOffering offering) {
    List<ScheduleOption> options = offering.getScheduleOptions();
    if (options.isEmpty()) {
      return false;
    }
    CourseView courseView = getCourseQuery.getCourse(offering.getCourseId());
    int curricularHours = courseView.curricularWeeklyHours();
    for (ScheduleOption option : options) {
      double computed = option.weeklyHours();
      // Compare as integers (round to nearest) to avoid floating-point comparison issues
      if (Math.round(computed) != curricularHours) {
        return true;
      }
    }
    return false;
  }

  OfferingDetailView toDetailView(CourseOffering offering, boolean intensityWarning) {
    List<ScheduleOptionView> optionViews =
        offering.getScheduleOptions().stream()
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
        optionViews,
        intensityWarning);
  }

  private ScheduleOptionView toOptionView(ScheduleOption option, UUID offeringId) {
    List<ScheduleSessionView> sessionViews =
        option.getSessions().stream()
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
