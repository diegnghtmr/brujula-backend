package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.offering;

import co.edu.uniquindio.brujula.offering.application.port.in.ResolvePeriodQuery;
import co.edu.uniquindio.brujula.simulation.application.port.out.OfferingReadPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Anti-corruption adapter: bridges simulation's {@link OfferingReadPort} to offering's published
 * {@code offering::api} named interface (ADR-F4-005, INV-001).
 *
 * <p>Only {@link #resolvePeriodName(UUID)} is implemented for WU3. Other methods from the port
 * that will be added in WU4/WU5 will be implemented in those work units.
 */
@Component
class OfferingReadAdapter implements OfferingReadPort {

  private final ResolvePeriodQuery resolvePeriodQuery;

  OfferingReadAdapter(ResolvePeriodQuery resolvePeriodQuery) {
    this.resolvePeriodQuery = resolvePeriodQuery;
  }

  @Override
  public String resolvePeriodName(UUID periodId) {
    return resolvePeriodQuery.resolvePeriod(periodId).name();
  }
}
