package co.edu.uniquindio.brujula.administration.application.service;

import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;
import co.edu.uniquindio.brujula.administration.application.port.in.UpdateStaffUseCase;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveStaffPort;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.administration.domain.StaffAccountStatus;
import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import java.util.UUID;

/**
 * Use case: update a staff account's status (activate / deactivate).
 *
 * <p>Takes a status name string so callers do not need to import the domain enum (ARQ-001). Returns
 * {@link StaffAccountView} (application-layer DTO).
 *
 * <p>Records a {@code STAFF_DEACTIVATED} audit event when a staff account is deactivated (Spec
 * §Audit Log, RNF-SEG-007 synchronous).
 *
 * <p>Framework-free. Satisfies: Design §2.2 UpdateStaffUseCase.
 */
public class UpdateStaffService implements UpdateStaffUseCase {

  private final LoadStaffPort loadStaffPort;
  private final SaveStaffPort saveStaffPort;
  private final AuditRecorder auditRecorder;

  public UpdateStaffService(
      LoadStaffPort loadStaffPort, SaveStaffPort saveStaffPort, AuditRecorder auditRecorder) {
    this.loadStaffPort = loadStaffPort;
    this.saveStaffPort = saveStaffPort;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public StaffAccountView updateStatus(
      UUID staffId, String newStatusName, long version, UUID actorId) {
    StaffAccount account =
        loadStaffPort.findById(staffId).orElseThrow(() -> new StaffNotFoundException(staffId));

    StaffAccountStatus newStatus = StaffAccountStatus.valueOf(newStatusName);
    if (newStatus == StaffAccountStatus.INACTIVE) {
      account.deactivate();
      auditRecorder.record(
          new RecordAuditEventCommand(
              actorId, "ADMIN", "STAFF_DEACTIVATED", "app_user", staffId, null, null, null));
    } else {
      account.activate();
    }

    StaffAccount updated = saveStaffPort.save(account);
    return toView(updated);
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
