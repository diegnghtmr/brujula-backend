package co.edu.uniquindio.brujula.offering.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.dto.UpdatePeriodCommand;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdatePeriodUseCase;
import co.edu.uniquindio.brujula.offering.application.port.out.SavePeriodPort;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriodId;
import co.edu.uniquindio.brujula.offering.domain.PeriodNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.PeriodStatus;

/**
 * Use case: update an academic period with optimistic locking (ADMIN-only, NOT idempotent).
 *
 * <p>Optimistic lock: the domain update method returns a new instance; the persistence adapter
 * maps {@code expectedVersion} onto the JPA entity before calling {@code save + flush} so
 * Hibernate raises {@link org.springframework.dao.OptimisticLockingFailureException} on conflict.
 *
 * <p>Framework-free — wired by {@code OfferingConfiguration}.
 * Satisfies: OFFW-002.
 */
public class UpdatePeriodService implements UpdatePeriodUseCase {

  private final SavePeriodPort savePeriodPort;
  private final AuditRecorder auditRecorder;

  public UpdatePeriodService(SavePeriodPort savePeriodPort, AuditRecorder auditRecorder) {
    this.savePeriodPort = savePeriodPort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public AcademicPeriodView updatePeriod(UpdatePeriodCommand command) {
    AcademicPeriodId id = AcademicPeriodId.of(command.periodId());

    // 1. Load current period (throws PeriodNotFoundException if absent)
    AcademicPeriod existing = savePeriodPort.findCurrentById(id)
        .orElseThrow(() -> new PeriodNotFoundException(id));

    // 2. Apply non-null fields via domain update method
    PeriodStatus newStatus = command.status() != null
        ? PeriodStatus.valueOf(command.status())
        : null;
    AcademicPeriod updated = existing.update(command.startDate(), command.endDate(), newStatus);

    // 3. Reconstitute with expectedVersion so the JPA adapter can set it on the entity
    //    The adapter sets entity.version = expectedVersion; Hibernate detects conflict at flush.
    AcademicPeriod withExpectedVersion = AcademicPeriod.reconstitute(
        updated.getId(),
        updated.getName(),
        updated.getStartDate(),
        updated.getEndDate(),
        updated.getStatus(),
        command.expectedVersion());

    // 4. Persist — may throw OptimisticLockingFailureException (→ 409 at handler)
    AcademicPeriod saved = savePeriodPort.save(withExpectedVersion);

    // 5. Record audit event
    auditRecorder.record(new RecordAuditEventCommand(
        command.actorId(),
        "ADMIN",
        "PERIOD_UPDATED",
        "academic_period",
        saved.getId().value(),
        null,
        null,
        null));

    return toView(saved);
  }

  private AcademicPeriodView toView(AcademicPeriod period) {
    return new AcademicPeriodView(
        period.getId().value(),
        period.getName(),
        period.getStartDate(),
        period.getEndDate(),
        period.getStatus().name(),
        period.getVersion());
  }
}
