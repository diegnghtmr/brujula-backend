package co.edu.uniquindio.brujula.audit.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.out.SaveAuditEventPort;
import co.edu.uniquindio.brujula.audit.application.service.RecordAuditEventService;
import co.edu.uniquindio.brujula.audit.domain.AuditEvent;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RecordAuditEventService} use case (TDD RED phase, zero Spring).
 *
 * <p>Verifies that the service correctly persists audit events via the output port.
 */
class RecordAuditEventServiceTest {

  private SaveAuditEventPort saveAuditEventPort;
  private RecordAuditEventService service;

  @BeforeEach
  void setUp() {
    saveAuditEventPort = mock(SaveAuditEventPort.class);
    service = new RecordAuditEventService(saveAuditEventPort);
  }

  @Test
  void record_persists_audit_event() {
    UUID actorId = UUID.randomUUID();
    var command =
        new RecordAuditEventCommand(
            actorId, "ADMIN", "STAFF_CREATED", "app_user", null, null, null, null);

    service.record(command);

    verify(saveAuditEventPort).save(any(AuditEvent.class));
  }

  @Test
  void record_passes_event_with_correct_action_to_port() {
    UUID actorId = UUID.randomUUID();
    var command =
        new RecordAuditEventCommand(actorId, "STAFF", "LOGIN", "session", null, null, null, null);

    service.record(command);

    verify(saveAuditEventPort)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                evt -> "LOGIN".equals(evt.getAction()) && actorId.equals(evt.getActorId())));
  }

  @Test
  void record_rejects_null_command() {
    assertThatThrownBy(() -> service.record(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("command");
    verify(saveAuditEventPort, never()).save(any());
  }
}
