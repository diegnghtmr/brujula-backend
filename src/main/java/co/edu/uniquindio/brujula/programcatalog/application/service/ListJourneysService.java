package co.edu.uniquindio.brujula.programcatalog.application.service;

import co.edu.uniquindio.brujula.programcatalog.application.dto.JourneyRef;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListJourneysQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadJourneyPort;
import co.edu.uniquindio.brujula.programcatalog.domain.Journey;
import java.util.List;

/**
 * Use case implementation: list all active journeys.
 *
 * <p>Delegates to {@link LoadJourneyPort#loadAllActive()} — the port is responsible for returning
 * only active journeys. No Spring annotations — wired via infrastructure configuration.
 */
public class ListJourneysService implements ListJourneysQuery {

  private final LoadJourneyPort loadJourneyPort;

  public ListJourneysService(LoadJourneyPort loadJourneyPort) {
    this.loadJourneyPort = loadJourneyPort;
  }

  @Override
  public List<JourneyRef> listJourneys() {
    List<Journey> journeys = loadJourneyPort.loadAllActive();
    return journeys.stream().map(this::toRef).toList();
  }

  // ---------------------------------------------------------------------------
  // Domain → DTO mapper
  // ---------------------------------------------------------------------------

  private JourneyRef toRef(Journey journey) {
    return new JourneyRef(journey.id().value(), journey.code(), journey.name());
  }
}
