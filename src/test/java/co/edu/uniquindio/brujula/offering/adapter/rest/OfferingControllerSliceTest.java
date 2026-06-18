package co.edu.uniquindio.brujula.offering.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingSummaryView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleSessionView;
import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * REST slice tests for offering endpoints.
 *
 * <p>Tests:
 *
 * <ul>
 *   <li>GET /periods returns 200 with array
 *   <li>GET /offerings missing periodId returns 400
 *   <li>GET /offerings/{id} unknown id returns 404
 *   <li>DRAFT offering is excluded from student listing
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OfferingControllerSliceTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  @MockitoBean private ListPeriodsQuery listPeriodsQuery;
  @MockitoBean private ListOfferingsQuery listOfferingsQuery;
  @MockitoBean private GetOfferingQuery getOfferingQuery;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void getPeriodsReturns200WithArray() throws Exception {
    when(listPeriodsQuery.listPeriods(isNull()))
        .thenReturn(
            List.of(new AcademicPeriodView(UUID.randomUUID(), "2026-2", null, null, "ACTIVE", 0L)));

    mockMvc
        .perform(
            get("/periods")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].name").value("2026-2"));
  }

  @Test
  void listOfferingsMissingPeriodIdReturns400() throws Exception {
    mockMvc
        .perform(
            get("/offerings")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getOfferingUnknownIdReturns404() throws Exception {
    UUID unknownId = UUID.randomUUID();
    when(getOfferingQuery.getOffering(unknownId))
        .thenThrow(new OfferingNotFoundException(unknownId));

    mockMvc
        .perform(
            get("/offerings/{id}", unknownId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void listOfferingsExcludesDraftFromStudentView() throws Exception {
    UUID periodId = UUID.randomUUID();
    // Only PRELIMINARY/FINAL offerings should be returned (no DRAFT)
    when(listOfferingsQuery.listOfferings(any(ListOfferingsCriteria.class)))
        .thenReturn(
            new OfferingPageView(
                List.of(
                    new OfferingSummaryView(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Programación I",
                        UUID.randomUUID(),
                        periodId,
                        "PRELIMINARY",
                        null,
                        0L)),
                0,
                1,
                1L));

    mockMvc
        .perform(
            get("/offerings")
                .param("periodId", periodId.toString())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].status").value("PRELIMINARY"));
  }

  // -------------------------------------------------------------------------
  // W-001 fix: courseName must NOT be "N/A" in REST response
  // -------------------------------------------------------------------------

  @Test
  void getOfferingDetailResponseHasRealCourseNameNotNA() throws Exception {
    UUID offeringId = UUID.randomUUID();
    UUID optionId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();

    when(getOfferingQuery.getOffering(offeringId))
        .thenReturn(
            new OfferingDetailView(
                offeringId,
                courseId,
                "Cálculo Diferencial",
                programId,
                periodId,
                "FINAL",
                null,
                0L,
                List.of(
                    new ScheduleOptionView(
                        optionId,
                        offeringId,
                        "Grupo A",
                        "DIURNA",
                        6.0,
                        0L,
                        List.of(
                            new ScheduleSessionView(
                                sessionId, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0)))))));

    mockMvc
        .perform(
            get("/offerings/{id}", offeringId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courseName").value("Cálculo Diferencial"))
        .andExpect(jsonPath("$.courseName").value(org.hamcrest.Matchers.not("N/A")));
  }

  // -------------------------------------------------------------------------
  // W-002 fix: version on OfferingDetail must come from entity, not hardcoded 0
  // -------------------------------------------------------------------------

  @Test
  void getOfferingDetailResponseHasVersionFromEntity() throws Exception {
    UUID offeringId = UUID.randomUUID();
    UUID optionId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();

    // version=3 simulates an entity that has been updated 3 times
    when(getOfferingQuery.getOffering(offeringId))
        .thenReturn(
            new OfferingDetailView(
                offeringId,
                courseId,
                "Programación III",
                programId,
                periodId,
                "FINAL",
                null,
                3L, // non-zero version
                List.of(
                    new ScheduleOptionView(
                        optionId,
                        offeringId,
                        "Grupo A",
                        "DIURNA",
                        4.0,
                        2L, // non-zero schedule option version
                        List.of(
                            new ScheduleSessionView(
                                sessionId, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0)))))));

    mockMvc
        .perform(
            get("/offerings/{id}", offeringId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(3))
        .andExpect(jsonPath("$.scheduleOptions[0].version").value(2));
  }

  // -------------------------------------------------------------------------
  // W-003 fix: offeringId must not be null on ScheduleOption in response
  // -------------------------------------------------------------------------

  @Test
  void getOfferingDetailResponseHasNonNullOfferingIdOnScheduleOption() throws Exception {
    UUID offeringId = UUID.randomUUID();
    UUID optionId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID periodId = UUID.randomUUID();

    when(getOfferingQuery.getOffering(offeringId))
        .thenReturn(
            new OfferingDetailView(
                offeringId,
                courseId,
                "Estructuras de Datos",
                programId,
                periodId,
                "FINAL",
                null,
                0L,
                List.of(
                    new ScheduleOptionView(
                        optionId,
                        offeringId, // offeringId populated
                        "Grupo A",
                        "NOCTURNA",
                        3.0,
                        0L,
                        List.of(
                            new ScheduleSessionView(
                                sessionId,
                                "TUESDAY",
                                LocalTime.of(18, 0),
                                LocalTime.of(19, 30)))))));

    mockMvc
        .perform(
            get("/offerings/{id}", offeringId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scheduleOptions[0].offeringId").value(offeringId.toString()));
  }
}
