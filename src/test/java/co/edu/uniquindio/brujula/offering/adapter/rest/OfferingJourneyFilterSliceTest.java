package co.edu.uniquindio.brujula.offering.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.port.in.CreateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdateOfferingUseCase;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * TDD: REST slice tests for the journeyCode filter on GET /offerings.
 *
 * <p>Verifies that the journeyCode query param is correctly parsed, bound as a list, and propagated
 * into {@link ListOfferingsCriteria#journeyCodes()}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OfferingJourneyFilterSliceTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  @MockitoBean private ListPeriodsQuery listPeriodsQuery;
  @MockitoBean private ListOfferingsQuery listOfferingsQuery;
  @MockitoBean private GetOfferingQuery getOfferingQuery;
  @MockitoBean private CreateOfferingUseCase createOfferingUseCase;
  @MockitoBean private UpdateOfferingUseCase updateOfferingUseCase;
  @MockitoBean private GetCurrentUserQuery getCurrentUserQuery;

  private MockMvc mockMvc;

  private static final UUID PERIOD_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(listOfferingsQuery.listOfferings(any(ListOfferingsCriteria.class)))
        .thenReturn(new OfferingPageView(Collections.emptyList(), 0, 20, 0L));
  }

  @Test
  void journeyCodeParam_single_propagatesIntoJourneyCodes() throws Exception {
    mockMvc
        .perform(
            get("/offerings")
                .param("periodId", PERIOD_ID.toString())
                .param("journeyCode", "DIURNA")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk());

    ArgumentCaptor<ListOfferingsCriteria> captor = forClass(ListOfferingsCriteria.class);
    verify(listOfferingsQuery).listOfferings(captor.capture());
    ListOfferingsCriteria captured = captor.getValue();
    assertThat(captured.journeyCodes())
        .as("Single journeyCode param must be captured as a one-element list")
        .containsExactly("DIURNA");
  }

  @Test
  void journeyCodeParam_multi_propagatesIntoJourneyCodes() throws Exception {
    mockMvc
        .perform(
            get("/offerings")
                .param("periodId", PERIOD_ID.toString())
                .param("journeyCode", "DIURNA")
                .param("journeyCode", "NOCTURNA")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk());

    ArgumentCaptor<ListOfferingsCriteria> captor = forClass(ListOfferingsCriteria.class);
    verify(listOfferingsQuery).listOfferings(captor.capture());
    ListOfferingsCriteria captured = captor.getValue();
    assertThat(captured.journeyCodes())
        .as("Multiple journeyCode params must all be captured in the list")
        .containsExactlyInAnyOrder("DIURNA", "NOCTURNA");
  }

  @Test
  void noJourneyCodeParam_journeyCodesIsNullOrEmpty() throws Exception {
    mockMvc
        .perform(
            get("/offerings")
                .param("periodId", PERIOD_ID.toString())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk());

    ArgumentCaptor<ListOfferingsCriteria> captor = forClass(ListOfferingsCriteria.class);
    verify(listOfferingsQuery).listOfferings(captor.capture());
    ListOfferingsCriteria captured = captor.getValue();
    assertThat(captured.journeyCodes())
        .as("Absent journeyCode param must produce null or empty journeyCodes (no filter)")
        .satisfiesAnyOf(list -> assertThat(list).isNull(), list -> assertThat(list).isEmpty());
  }
}
