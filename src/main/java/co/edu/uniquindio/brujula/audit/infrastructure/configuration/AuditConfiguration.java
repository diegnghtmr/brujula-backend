package co.edu.uniquindio.brujula.audit.infrastructure.configuration;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.audit.application.port.out.SaveAuditEventPort;
import co.edu.uniquindio.brujula.audit.application.service.RecordAuditEventService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the audit module's application services.
 *
 * <p>Exposes the {@link AuditRecorder} as a bean so other modules (identity, administration) can
 * inject it by the interface type — never the concrete class. This satisfies the Modulith
 * cross-module dependency rule: callers depend only on the port interface.
 */
@Configuration
class AuditConfiguration {

  @Bean
  AuditRecorder auditRecorder(SaveAuditEventPort saveAuditEventPort) {
    return new RecordAuditEventService(saveAuditEventPort);
  }
}
