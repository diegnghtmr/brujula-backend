package co.edu.uniquindio.brujula.programcatalog.application.port.in;

import co.edu.uniquindio.brujula.programcatalog.application.dto.JourneyRef;
import java.util.List;

/**
 * Input port: list all active journeys.
 *
 * <p>Backs {@code GET /journeys}. Only active journeys are returned.
 *
 * <p>No Spring/framework imports.
 */
public interface ListJourneysQuery {

  /**
   * Return all active journeys.
   *
   * @return list of {@link JourneyRef} DTOs (never null; may be empty)
   */
  List<JourneyRef> listJourneys();
}
