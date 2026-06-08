package co.edu.uniquindio.brujula.administration.infrastructure.configuration;

import co.edu.uniquindio.brujula.administration.application.port.out.LoadAllowedEmailDomainsPort;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadCreditLimitPort;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveAllowedEmailDomainPort;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveStaffPort;
import co.edu.uniquindio.brujula.administration.application.service.CreateStaffService;
import co.edu.uniquindio.brujula.administration.application.service.GetCreditLimitService;
import co.edu.uniquindio.brujula.administration.application.service.ListStaffService;
import co.edu.uniquindio.brujula.administration.application.service.ManageAllowedEmailDomainsService;
import co.edu.uniquindio.brujula.administration.application.service.UpdateStaffService;
import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the administration module's application services.
 *
 * <p>Bridges framework-free application services into the Spring container. Persistence adapters
 * are auto-detected via {@code @Component}; services are registered here for explicit wiring.
 *
 * <p>ARQ-006: all port.out adapters are injected as interfaces — never concrete adapter classes.
 */
@Configuration
class AdministrationConfiguration {

  @Bean
  CreateStaffService createStaffService(
      LoadStaffPort loadStaffPort,
      SaveStaffPort saveStaffPort,
      LoadAllowedEmailDomainsPort loadAllowedEmailDomainsPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder) {
    return new CreateStaffService(
        loadStaffPort,
        saveStaffPort,
        loadAllowedEmailDomainsPort,
        idempotencyStorePort,
        jsonCodecPort,
        auditRecorder);
  }

  @Bean
  ListStaffService listStaffService(LoadStaffPort loadStaffPort) {
    return new ListStaffService(loadStaffPort);
  }

  @Bean
  UpdateStaffService updateStaffService(
      LoadStaffPort loadStaffPort, SaveStaffPort saveStaffPort, AuditRecorder auditRecorder) {
    return new UpdateStaffService(loadStaffPort, saveStaffPort, auditRecorder);
  }

  @Bean
  ManageAllowedEmailDomainsService manageAllowedEmailDomainsService(
      SaveAllowedEmailDomainPort saveAllowedEmailDomainPort, AuditRecorder auditRecorder) {
    return new ManageAllowedEmailDomainsService(saveAllowedEmailDomainPort, auditRecorder);
  }

  @Bean
  GetCreditLimitService getCreditLimitService(LoadCreditLimitPort loadCreditLimitPort) {
    return new GetCreditLimitService(loadCreditLimitPort);
  }
}
