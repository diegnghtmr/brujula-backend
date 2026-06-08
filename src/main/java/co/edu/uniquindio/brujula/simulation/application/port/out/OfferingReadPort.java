package co.edu.uniquindio.brujula.simulation.application.port.out;

import java.util.UUID;

/**
 * Output port: read offering data from the offering module (ADR-F4-005).
 *
 * <p>WU3 uses only {@link #resolvePeriodName(UUID)}. Other methods are declared for WU4/WU5
 * but throw {@link UnsupportedOperationException} in the adapter until then.
 */
public interface OfferingReadPort {

  /**
   * Resolve the human-readable period name for a given period UUID.
   *
   * @param periodId the period UUID
   * @return period name string (e.g. "2026-2")
   * @throws co.edu.uniquindio.brujula.offering.domain.PeriodNotFoundException if not found
   */
  String resolvePeriodName(UUID periodId);
}
