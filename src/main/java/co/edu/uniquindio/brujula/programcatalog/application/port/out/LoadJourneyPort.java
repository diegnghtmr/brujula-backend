package co.edu.uniquindio.brujula.programcatalog.application.port.out;

import co.edu.uniquindio.brujula.programcatalog.domain.Journey;
import java.util.List;

/**
 * Output port: load journeys from persistence.
 *
 * <p>Named by need (ISP) per hexagonal architecture conventions. No Spring/framework imports.
 */
public interface LoadJourneyPort {

  /**
   * Load all active journeys (active=true only).
   *
   * @return active journeys
   */
  List<Journey> loadAllActive();
}
