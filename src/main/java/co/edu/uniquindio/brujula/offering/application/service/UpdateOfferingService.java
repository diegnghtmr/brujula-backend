package co.edu.uniquindio.brujula.offering.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleSessionView;
import co.edu.uniquindio.brujula.offering.application.dto.UpdateOfferingCommand;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.out.SaveOfferingPort;
import co.edu.uniquindio.brujula.offering.domain.CourseOffering;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.ScheduleOption;

import java.util.List;
import java.util.UUID;

/**
 * Use case: update offering notes with optimistic locking (STAFF/ADMIN, NOT idempotent).
 *
 * <p>A successful update MUST cause {@code course_offering.version} to increment — this is the
 * staleness activation invariant (OFFW-006). The JPA @Version mechanism handles the increment
 * automatically at flush time when the row's notes column changes.
 *
 * <p>Framework-free — wired by {@code OfferingConfiguration}.
 * Satisfies: OFFW-004, OFFW-006.
 */
public class UpdateOfferingService implements UpdateOfferingUseCase {

  private final SaveOfferingPort saveOfferingPort;
  private final AuditRecorder auditRecorder;

  public UpdateOfferingService(SaveOfferingPort saveOfferingPort, AuditRecorder auditRecorder) {
    this.saveOfferingPort = saveOfferingPort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public OfferingDetailView updateOffering(UpdateOfferingCommand command) {
    CourseOfferingId id = CourseOfferingId.of(command.offeringId());

    // 1. Load current offering (throws OfferingNotFoundException if absent)
    CourseOffering existing = saveOfferingPort.findCurrentById(id)
        .orElseThrow(() -> new OfferingNotFoundException(id));

    // 2. Apply notes update via domain method (returns new immutable instance)
    CourseOffering updated = existing.updateNotes(command.notes());

    // 3. Reconstitute with expectedVersion so the JPA adapter sets it on the entity;
    //    Hibernate detects mismatch at flush → OptimisticLockingFailureException (→ 409).
    CourseOffering withExpectedVersion = CourseOffering.reconstitute(
        updated.getId(),
        updated.getCourseId(),
        updated.getCourseName(),
        updated.getProgramId(),
        updated.getPeriodId(),
        updated.getStatus(),
        updated.getNotes(),
        updated.getScheduleOptions(),
        command.expectedVersion());

    // 4. Persist — may throw OptimisticLockingFailureException (→ 409 at handler)
    //    The adapter returns the managed post-flush entity with @Version already incremented.
    CourseOffering saved = saveOfferingPort.save(withExpectedVersion);

    // 5. Record audit event (version bump already happened in save + flush)
    auditRecorder.record(new RecordAuditEventCommand(
        command.actorId(),
        "STAFF",
        "OFFERING_UPDATED",
        "course_offering",
        saved.getId().value(),
        null,
        null,
        null));

    return toDetailView(saved);
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
