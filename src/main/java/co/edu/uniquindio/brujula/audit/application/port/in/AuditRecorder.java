package co.edu.uniquindio.brujula.audit.application.port.in;

import co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand;

/**
 * Cross-module SPI: the port other modules (identity, administration) call to record audit events.
 *
 * <p>This interface lives in the audit module's {@code application.port.in} package so it is the
 * natural inbound entry point for external callers. Other modules depend on this interface via
 * Spring injection — they MUST NOT import the concrete service or any persistence adapter.
 *
 * <p>Satisfies: Design §2.3 AuditRecorder, Spec §Audit Log (TRD §6.3 Todos→audit).
 */
public interface AuditRecorder {

  /**
   * Record a single audit event synchronously in the same transaction as the mutating action.
   *
   * @param command the audit event details (must not be null)
   */
  void record(RecordAuditEventCommand command);
}
