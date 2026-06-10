package co.edu.uniquindio.brujula.programcatalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.programcatalog.application.dto.JourneyRef;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListJourneysQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadJourneyPort;
import co.edu.uniquindio.brujula.programcatalog.application.service.ListJourneysService;
import co.edu.uniquindio.brujula.programcatalog.domain.Journey;
import co.edu.uniquindio.brujula.programcatalog.domain.JourneyId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** WU2-2.5 RED: Verifies ListJourneysService returns only active journeys (CAT-003). */
class ListJourneysServiceTest {

  private LoadJourneyPort loadJourneyPort;
  private ListJourneysQuery service;

  @BeforeEach
  void setUp() {
    loadJourneyPort = mock(LoadJourneyPort.class);
    service = new ListJourneysService(loadJourneyPort);
  }

  @Test
  void returnsAllActiveJourneys() {
    Journey diurna = Journey.reconstitute(JourneyId.generate(), "DIURNA", "Jornada Diurna", true);
    Journey nocturna =
        Journey.reconstitute(JourneyId.generate(), "NOCTURNA", "Jornada Nocturna", true);

    when(loadJourneyPort.loadAllActive()).thenReturn(List.of(diurna, nocturna));

    List<JourneyRef> result = service.listJourneys();

    assertThat(result).hasSize(2);
    assertThat(result).extracting(JourneyRef::code).containsExactlyInAnyOrder("DIURNA", "NOCTURNA");
  }

  @Test
  void journeyRefContainsRequiredFields() {
    JourneyId id = JourneyId.generate();
    Journey diurna = Journey.reconstitute(id, "DIURNA", "Jornada Diurna", true);

    when(loadJourneyPort.loadAllActive()).thenReturn(List.of(diurna));

    List<JourneyRef> result = service.listJourneys();

    JourneyRef ref = result.get(0);
    assertThat(ref.id()).isEqualTo(id.value());
    assertThat(ref.code()).isEqualTo("DIURNA");
    assertThat(ref.name()).isEqualTo("Jornada Diurna");
  }

  @Test
  void returnsEmptyWhenNoActiveJourneys() {
    when(loadJourneyPort.loadAllActive()).thenReturn(List.of());

    List<JourneyRef> result = service.listJourneys();

    assertThat(result).isEmpty();
  }
}
