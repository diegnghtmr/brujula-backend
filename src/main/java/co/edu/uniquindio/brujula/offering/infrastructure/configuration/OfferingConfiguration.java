package co.edu.uniquindio.brujula.offering.infrastructure.configuration;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.CreateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.CreatePeriodUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.PublishOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.ResolvePeriodQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ResolveScheduleOptionQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdatePeriodUseCase;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadOfferingPort;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadPeriodPort;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadScheduleOptionPort;
import co.edu.uniquindio.brujula.offering.application.port.out.SaveOfferingPort;
import co.edu.uniquindio.brujula.offering.application.port.out.SavePeriodPort;
import co.edu.uniquindio.brujula.offering.application.service.CreateOfferingService;
import co.edu.uniquindio.brujula.offering.application.service.CreatePeriodService;
import co.edu.uniquindio.brujula.offering.application.service.GetOfferingService;
import co.edu.uniquindio.brujula.offering.application.service.ListPeriodsService;
import co.edu.uniquindio.brujula.offering.application.service.PublishOfferingService;
import co.edu.uniquindio.brujula.offering.application.service.ResolvePeriodService;
import co.edu.uniquindio.brujula.offering.application.service.ResolveScheduleOptionService;
import co.edu.uniquindio.brujula.offering.application.service.UpdateOfferingService;
import co.edu.uniquindio.brujula.offering.application.service.UpdatePeriodService;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.IdempotencyStorePort;
import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean wiring for the offering module's application services.
 *
 * <p>Framework-free application services are registered here for explicit constructor injection.
 * Persistence adapters are auto-detected via {@code @Component}. All port.out adapters are injected
 * as interfaces — never concrete adapter classes (ARQ-006).
 *
 * <p>Wires:
 * <ul>
 *   <li>{@link ListPeriodsService} — implements {@link ListPeriodsQuery} (OFF-001)
 *   <li>{@link GetOfferingService} — implements {@link ListOfferingsQuery} + {@link GetOfferingQuery} (OFF-002, OFF-003)
 *   <li>{@link ResolvePeriodService} — implements {@link ResolvePeriodQuery} (OFF-004)
 *   <li>{@link ResolveScheduleOptionService} — implements {@link ResolveScheduleOptionQuery} (OFF-004)
 *   <li>{@link CreatePeriodService} — implements {@link CreatePeriodUseCase} (OFFW-001)
 *   <li>{@link UpdatePeriodService} — implements {@link UpdatePeriodUseCase} (OFFW-002)
 *   <li>{@link CreateOfferingService} — implements {@link CreateOfferingUseCase} (OFFW-003)
 *   <li>{@link UpdateOfferingService} — implements {@link UpdateOfferingUseCase} (OFFW-004)
 *   <li>{@link PublishOfferingService} — implements {@link PublishOfferingUseCase} (OFFW-005)
 * </ul>
 */
@Configuration
class OfferingConfiguration {

  @Bean
  ListPeriodsQuery listPeriodsService(LoadPeriodPort loadPeriodPort) {
    return new ListPeriodsService(loadPeriodPort);
  }

  /**
   * {@link GetOfferingService} implements both {@link ListOfferingsQuery} and
   * {@link GetOfferingQuery}.
   *
   * <p>Registering the concrete class lets Spring satisfy both interface autowiring points
   * from a single bean instance.
   */
  @Bean
  GetOfferingService getOfferingService(LoadOfferingPort loadOfferingPort) {
    return new GetOfferingService(loadOfferingPort);
  }

  @Bean
  ResolvePeriodQuery resolvePeriodService(LoadPeriodPort loadPeriodPort) {
    return new ResolvePeriodService(loadPeriodPort);
  }

  @Bean
  ResolveScheduleOptionQuery resolveScheduleOptionService(
      LoadScheduleOptionPort loadScheduleOptionPort,
      LoadOfferingPort loadOfferingPort) {
    return new ResolveScheduleOptionService(loadScheduleOptionPort, loadOfferingPort);
  }

  @Bean
  CreatePeriodUseCase createPeriodService(
      SavePeriodPort savePeriodPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder) {
    return new CreatePeriodService(savePeriodPort, idempotencyStorePort, jsonCodecPort, auditRecorder);
  }

  @Bean
  UpdatePeriodUseCase updatePeriodService(SavePeriodPort savePeriodPort, AuditRecorder auditRecorder) {
    return new UpdatePeriodService(savePeriodPort, auditRecorder);
  }

  @Bean
  CreateOfferingUseCase createOfferingService(
      SaveOfferingPort saveOfferingPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder,
      GetCourseQuery getCourseQuery) {
    return new CreateOfferingService(
        saveOfferingPort, idempotencyStorePort, jsonCodecPort, auditRecorder, getCourseQuery);
  }

  @Bean
  UpdateOfferingUseCase updateOfferingService(
      SaveOfferingPort saveOfferingPort, AuditRecorder auditRecorder) {
    return new UpdateOfferingService(saveOfferingPort, auditRecorder);
  }

  @Bean
  PublishOfferingUseCase publishOfferingService(
      SaveOfferingPort saveOfferingPort,
      IdempotencyStorePort idempotencyStorePort,
      JsonCodecPort jsonCodecPort,
      AuditRecorder auditRecorder,
      GetCourseQuery getCourseQuery) {
    return new PublishOfferingService(
        saveOfferingPort, idempotencyStorePort, jsonCodecPort, auditRecorder, getCourseQuery);
  }
}
