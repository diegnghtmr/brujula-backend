package co.edu.uniquindio.brujula.administration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveStaffPort;
import co.edu.uniquindio.brujula.administration.application.service.UpdateStaffService;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UpdateStaffService} (strict TDD, zero Spring).
 *
 * <p>Spec §Audit Log: "staff deactivate" must be audited with action STAFF_DEACTIVATED.
 */
class UpdateStaffServiceTest {

  private LoadStaffPort loadStaffPort;
  private SaveStaffPort saveStaffPort;
  private AuditRecorder auditRecorder;
  private UpdateStaffService service;

  @BeforeEach
  void setUp() {
    loadStaffPort = mock(LoadStaffPort.class);
    saveStaffPort = mock(SaveStaffPort.class);
    auditRecorder = mock(AuditRecorder.class);
    service = new UpdateStaffService(loadStaffPort, saveStaffPort, auditRecorder);
  }

  // -------------------------------------------------------------------------
  // Deactivation path — must record STAFF_DEACTIVATED audit event
  // -------------------------------------------------------------------------

  @Test
  void deactivate_staff_records_staff_deactivated_audit_event() {
    UUID staffId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    StaffAccount account = StaffAccount.create("alice@uniquindio.edu.co", "Alice Smith");
    when(loadStaffPort.findById(staffId)).thenReturn(Optional.of(account));
    when(saveStaffPort.save(any())).thenReturn(account);

    service.updateStatus(staffId, "INACTIVE", 0L, actorId);

    verify(auditRecorder)
        .record(
            argThat(
                cmd ->
                    "STAFF_DEACTIVATED".equals(cmd.action())
                        && actorId.equals(cmd.actorId())
                        && "app_user".equals(cmd.entityType())));
  }

  @Test
  void activate_staff_does_not_record_deactivated_event() {
    UUID staffId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    StaffAccount account = StaffAccount.create("bob@uniquindio.edu.co", "Bob Jones");
    account.deactivate();
    when(loadStaffPort.findById(staffId)).thenReturn(Optional.of(account));
    when(saveStaffPort.save(any())).thenReturn(account);

    service.updateStatus(staffId, "ACTIVE", 0L, actorId);

    verify(auditRecorder, never()).record(argThat(cmd -> "STAFF_DEACTIVATED".equals(cmd.action())));
  }

  @Test
  void deactivate_staff_returns_updated_view() {
    UUID staffId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    StaffAccount account = StaffAccount.create("carol@uniquindio.edu.co", "Carol White");
    when(loadStaffPort.findById(staffId)).thenReturn(Optional.of(account));
    when(saveStaffPort.save(any())).thenReturn(account);

    StaffAccountView result = service.updateStatus(staffId, "INACTIVE", 0L, actorId);

    assertThat(result).isNotNull();
    assertThat(result.email()).isEqualTo("carol@uniquindio.edu.co");
  }
}
