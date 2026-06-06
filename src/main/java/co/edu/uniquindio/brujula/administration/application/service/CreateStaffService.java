package co.edu.uniquindio.brujula.administration.application.service;

import co.edu.uniquindio.brujula.administration.application.dto.CreateStaffCommand;
import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;
import co.edu.uniquindio.brujula.administration.application.port.in.CreateStaffUseCase;
import co.edu.uniquindio.brujula.administration.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadAllowedEmailDomainsPort;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveStaffPort;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.identity.domain.AllowedEmailDomainPolicy;
import co.edu.uniquindio.brujula.identity.domain.DomainNotAllowedException;
import java.util.Optional;
import java.util.Set;

/**
 * Use case: create a staff account, idempotent via Idempotency-Key header.
 *
 * <p>Idempotency rules:
 *
 * <ul>
 *   <li>Same key + same hash → return the stored result (replay, no new insert)
 *   <li>Same key + different hash → throw {@link IdempotencyKeyConflictException} (409)
 *   <li>New key → execute, persist idempotency record, record audit event
 * </ul>
 *
 * <p>Returns {@link StaffAccountView} (application-layer DTO) so port callers do not need to import
 * domain types (ARQ-001). Framework-free — wired by infrastructure configuration.
 *
 * <p>Satisfies: Design §2.2, §4, Spec §Idempotent Create.
 */
public class CreateStaffService implements CreateStaffUseCase {

  private final LoadStaffPort loadStaffPort;
  private final SaveStaffPort saveStaffPort;
  private final LoadAllowedEmailDomainsPort loadAllowedEmailDomainsPort;
  private final IdempotencyStorePort idempotencyStorePort;
  private final AuditRecorder auditRecorder;

  static final String OPERATION = "CREATE_STAFF";

  public CreateStaffService(
      LoadStaffPort loadStaffPort,
      SaveStaffPort saveStaffPort,
      LoadAllowedEmailDomainsPort loadAllowedEmailDomainsPort,
      IdempotencyStorePort idempotencyStorePort,
      AuditRecorder auditRecorder) {
    this.loadStaffPort = loadStaffPort;
    this.saveStaffPort = saveStaffPort;
    this.loadAllowedEmailDomainsPort = loadAllowedEmailDomainsPort;
    this.idempotencyStorePort = idempotencyStorePort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public StaffAccountView createStaff(CreateStaffCommand command) {
    // 1. Check idempotency store first
    Optional<IdempotencyStorePort.StoredResult> existing =
        idempotencyStorePort.findByKeyAndOperation(command.idempotencyKey(), OPERATION);
    if (existing.isPresent()) {
      IdempotencyStorePort.StoredResult stored = existing.get();
      if (!stored.requestHash().equals(command.requestHash())) {
        // Same key, different body — 409 Conflict
        throw new IdempotencyKeyConflictException(command.idempotencyKey(), OPERATION);
      }
      // Same key, same body — idempotent replay
      return toView(stored.result());
    }

    // 2. Validate allowed email domain (RN-007, RNF-SEG-006 dual enforcement)
    Set<String> allowedDomains = loadAllowedEmailDomainsPort.loadAllowedDomains();
    var policy = new AllowedEmailDomainPolicy(allowedDomains);
    if (!policy.isAllowed(command.email())) {
      throw new DomainNotAllowedException(command.email());
    }

    // 3. Create and persist the staff account
    StaffAccount account = StaffAccount.create(command.email(), command.fullName());
    StaffAccount saved = saveStaffPort.save(account);

    // 4. Record idempotency entry
    idempotencyStorePort.save(command.idempotencyKey(), OPERATION, command.requestHash(), saved);

    // 5. Record audit event (RNF-SEG-007 synchronous, same transaction)
    auditRecorder.record(
        new RecordAuditEventCommand(
            command.actorId(),
            "ADMIN",
            "STAFF_CREATED",
            "staff_account",
            saved.getId(),
            null,
            null,
            null));

    return toView(saved);
  }

  private StaffAccountView toView(StaffAccount account) {
    return new StaffAccountView(
        account.getId(),
        account.getEmail(),
        account.getFullName(),
        account.getStatus().name(),
        account.getVersion());
  }
}
