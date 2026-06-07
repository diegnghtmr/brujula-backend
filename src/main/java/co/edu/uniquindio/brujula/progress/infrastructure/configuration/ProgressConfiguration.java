package co.edu.uniquindio.brujula.progress.infrastructure.configuration;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.progress.application.port.in.CreateSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.DeleteSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.in.UpdateSubscriptionUseCase;
import co.edu.uniquindio.brujula.progress.application.port.out.LoadSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.port.out.SaveSubscriptionPort;
import co.edu.uniquindio.brujula.progress.application.service.CreateSubscriptionService;
import co.edu.uniquindio.brujula.progress.application.service.DeleteSubscriptionService;
import co.edu.uniquindio.brujula.progress.application.service.GetSubscriptionService;
import co.edu.uniquindio.brujula.progress.application.port.out.ValidatePlanStatusPort;
import co.edu.uniquindio.brujula.progress.application.service.UpdateSubscriptionService;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the progress module's application services.
 *
 * <p>Framework-free application services are registered here for explicit constructor injection.
 * Persistence adapters are auto-detected via {@code @Component}. All port.out adapters are injected
 * as interfaces — never concrete adapter classes (ARQ-006).
 *
 * <p>Wires:
 *
 * <ul>
 *   <li>{@link CreateSubscriptionService} (SUB-1, CROSS-1)
 *   <li>{@link GetSubscriptionService} — implements both {@code GetSubscriptionQuery} and
 *       {@code ListMySubscriptionsQuery} (SUB-2)
 *   <li>{@link UpdateSubscriptionService} (SUB-3)
 *   <li>{@link DeleteSubscriptionService} (SUB-4)
 * </ul>
 *
 * <p>{@code GetCurrentUserQuery} is injected by Spring's auto-wiring into {@link
 * co.edu.uniquindio.brujula.progress.infrastructure.adapter.in.rest.SubscriptionController}
 * directly — no explicit bean needed here.
 */
@Configuration
class ProgressConfiguration {

  @Bean
  CreateSubscriptionUseCase createSubscriptionService(
      LoadSubscriptionPort loadSubscriptionPort,
      SaveSubscriptionPort saveSubscriptionPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder,
      ValidatePlanStatusPort validatePlanStatusPort) {
    return new CreateSubscriptionService(
        loadSubscriptionPort,
        saveSubscriptionPort,
        idempotencyStorePort,
        jsonCodecPort,
        auditRecorder,
        validatePlanStatusPort);
  }

  /**
   * {@link GetSubscriptionService} implements both {@link
   * co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionQuery} and {@link
   * co.edu.uniquindio.brujula.progress.application.port.in.ListMySubscriptionsQuery}.
   *
   * <p>Registering the concrete class lets Spring satisfy both interface autowiring points from a
   * single bean instance.
   */
  @Bean
  GetSubscriptionService getSubscriptionService(LoadSubscriptionPort loadSubscriptionPort) {
    return new GetSubscriptionService(loadSubscriptionPort);
  }

  @Bean
  UpdateSubscriptionUseCase updateSubscriptionService(
      LoadSubscriptionPort loadSubscriptionPort,
      SaveSubscriptionPort saveSubscriptionPort,
      AuditRecorder auditRecorder) {
    return new UpdateSubscriptionService(loadSubscriptionPort, saveSubscriptionPort, auditRecorder);
  }

  @Bean
  DeleteSubscriptionUseCase deleteSubscriptionService(
      LoadSubscriptionPort loadSubscriptionPort,
      SaveSubscriptionPort saveSubscriptionPort,
      AuditRecorder auditRecorder) {
    return new DeleteSubscriptionService(loadSubscriptionPort, saveSubscriptionPort, auditRecorder);
  }
}
