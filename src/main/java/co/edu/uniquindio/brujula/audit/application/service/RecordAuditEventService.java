package co.edu.uniquindio.brujula.audit.application.service;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.audit.application.port.out.SaveAuditEventPort;
import co.edu.uniquindio.brujula.audit.domain.AuditEvent;

/**
 * Use case: record an audit event (append-only).
 *
 * <p>Framework-free — wired by infrastructure configuration. Implements the {@link AuditRecorder}
 * SPI so all modules that inject {@code AuditRecorder} use this service via Spring DI.
 *
 * <p>Records audit events synchronously (append-only).
 */
public class RecordAuditEventService implements AuditRecorder {

  private final SaveAuditEventPort saveAuditEventPort;

  public RecordAuditEventService(SaveAuditEventPort saveAuditEventPort) {
    this.saveAuditEventPort = saveAuditEventPort;
  }

  @Override
  public void record(RecordAuditEventCommand command) {
    if (command == null) {
      throw new IllegalArgumentException("command must not be null");
    }
    AuditEvent event =
        AuditEvent.record(
            command.actorId(),
            command.actorRole(),
            command.action(),
            command.entityType(),
            command.entityId(),
            command.programId(),
            command.previousValue(),
            command.newValue());
    saveAuditEventPort.save(event);
  }
}
