package co.edu.uniquindio.brujula.offering.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.dto.CreatePeriodCommand;
import co.edu.uniquindio.brujula.offering.application.port.in.CreatePeriodUseCase;
import co.edu.uniquindio.brujula.offering.application.port.out.SavePeriodPort;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriod;
import co.edu.uniquindio.brujula.offering.domain.AcademicPeriodId;
import co.edu.uniquindio.brujula.offering.domain.PeriodStatus;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import java.util.Optional;

/**
 * Use case: create an academic period (idempotent via Idempotency-Key, ADMIN-only).
 *
 * <p>Idempotency rules (mirrors CreateStaffService verbatim):
 *
 * <ul>
 *   <li>Same key + same hash → return the stored result (replay, no new insert)
 *   <li>Same key + different hash → throw {@link IdempotencyKeyConflictException} (409)
 *   <li>New key → execute, persist idempotency record, record audit event
 * </ul>
 *
 * <p>Framework-free — wired by {@code OfferingConfiguration}. Satisfies: OFFW-001.
 */
public class CreatePeriodService implements CreatePeriodUseCase {

  static final String OPERATION = "CREATE_PERIOD";

  private final SavePeriodPort savePeriodPort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final JsonCodecPort jsonCodecPort;
  private final AuditRecorder auditRecorder;

  public CreatePeriodService(
      SavePeriodPort savePeriodPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder) {
    this.savePeriodPort = savePeriodPort;
    this.idempotencyStorePort = idempotencyStorePort;
    this.jsonCodecPort = jsonCodecPort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public AcademicPeriodView createPeriod(CreatePeriodCommand command) {
    // 1. Check idempotency store first
    Optional<IdempotencyStorePort.StoredRecord> existing =
        idempotencyStorePort.find(command.idempotencyKey(), OPERATION);
    if (existing.isPresent()) {
      IdempotencyStorePort.StoredRecord stored = existing.get();
      if (!stored.requestHash().equals(command.requestHash())) {
        throw new IdempotencyKeyConflictException(command.idempotencyKey(), OPERATION);
      }
      return jsonCodecPort.fromJson(stored.responseBodyJson(), AcademicPeriodView.class);
    }

    // 2. Resolve status (null → UPCOMING)
    PeriodStatus status =
        command.status() != null ? PeriodStatus.valueOf(command.status()) : PeriodStatus.UPCOMING;

    // 3. Create domain object and persist
    AcademicPeriod period =
        AcademicPeriod.create(
            AcademicPeriodId.generate(),
            command.name(),
            command.startDate(),
            command.endDate(),
            status);
    AcademicPeriod saved = savePeriodPort.save(period);

    // 4. Build view and serialize for idempotency storage
    AcademicPeriodView view = toView(saved);
    String responseBodyJson = jsonCodecPort.toJson(view);

    // 5. Record idempotency entry
    idempotencyStorePort.save(
        command.idempotencyKey(), OPERATION, command.requestHash(), 201, responseBodyJson);

    // 6. Record audit event (RNF-SEG-007 synchronous, same transaction)
    auditRecorder.record(
        new RecordAuditEventCommand(
            command.actorId(),
            "ADMIN",
            "PERIOD_CREATED",
            "academic_period",
            saved.getId().value(),
            null,
            null,
            null));

    return view;
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
