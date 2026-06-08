package co.edu.uniquindio.brujula.offering.infrastructure.configuration;

import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ResolvePeriodQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ResolveScheduleOptionQuery;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadOfferingPort;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadPeriodPort;
import co.edu.uniquindio.brujula.offering.application.port.out.LoadScheduleOptionPort;
import co.edu.uniquindio.brujula.offering.application.service.GetOfferingService;
import co.edu.uniquindio.brujula.offering.application.service.ListPeriodsService;
import co.edu.uniquindio.brujula.offering.application.service.ResolvePeriodService;
import co.edu.uniquindio.brujula.offering.application.service.ResolveScheduleOptionService;

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
}
