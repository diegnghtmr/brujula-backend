package co.edu.uniquindio.brujula.audit.application.port.out;

import co.edu.uniquindio.brujula.audit.domain.AuditEvent;

/**
 * Output port: persists an audit event (append-only, no update/delete).
 *
 * <p>Implemented by the persistence adapter in the infrastructure layer.
 */
public interface SaveAuditEventPort {

  /**
   * Append a new audit event to the store.
   *
   * @param event the immutable event to persist
   */
  void save(AuditEvent event);
}
