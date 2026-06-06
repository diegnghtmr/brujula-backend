package co.edu.uniquindio.brujula.identity.infrastructure.configuration;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAllowedDomainsPort;
import co.edu.uniquindio.brujula.identity.application.port.out.LoadAppUserPort;
import co.edu.uniquindio.brujula.identity.application.port.out.ReadActivityPort;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.application.service.GetCurrentUserService;
import co.edu.uniquindio.brujula.identity.application.service.ListMyActivityService;
import co.edu.uniquindio.brujula.identity.application.service.ProvisionUserOnLoginService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the identity module's application services.
 *
 * <p>Bridges domain/application (framework-free) into the Spring container. Infrastructure adapters
 * are auto-detected via {@code @Component}; services are registered here to maintain explicit
 * wiring.
 *
 * <p>Injects {@link AuditRecorder} (cross-module SPI from the audit module) into {@link
 * ProvisionUserOnLoginService} to satisfy S1-C wiring requirement (RNF-SEG-007).
 */
@Configuration
class IdentityConfiguration {

  @Bean
  ProvisionUserOnLoginService provisionUserOnLoginService(
      LoadAppUserPort loadAppUserPort,
      SaveAppUserPort saveAppUserPort,
      LoadAllowedDomainsPort loadAllowedDomainsPort,
      AuditRecorder auditRecorder) {
    return new ProvisionUserOnLoginService(
        loadAppUserPort, saveAppUserPort, loadAllowedDomainsPort, auditRecorder);
  }

  @Bean
  GetCurrentUserService getCurrentUserService(LoadAppUserPort loadAppUserPort) {
    return new GetCurrentUserService(loadAppUserPort);
  }

  @Bean
  ListMyActivityService listMyActivityService(ReadActivityPort readActivityPort) {
    return new ListMyActivityService(readActivityPort);
  }
}
